package com.crisscross.ui.admin;

import com.crisscross.dao.ParkingAreaDAO;
import com.crisscross.dao.ParkingSlotDAO;
import com.crisscross.models.ParkingArea;
import com.crisscross.models.ParkingSlot;
import com.crisscross.ui.ScreenManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.util.List;

public class ParkingStatusManagementView extends VBox {
    private final ScreenManager screenManager;
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final ParkingAreaDAO areaDAO = new ParkingAreaDAO();
    private final ComboBox<ParkingArea> areaCombo = new ComboBox<>();
    private final FlowPane slotsPane = new FlowPane();
    private final Label zoneSummaryLbl = new Label();
    private boolean suppressComboEvent = false;

    public ParkingStatusManagementView(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.setSpacing(20);
        this.setFillWidth(true);

        // Header
        HBox topBox = new HBox(14);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Parking Status Management");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("Live interactive control panel: Click any slot to toggle between AVAILABLE, OCCUPIED, and BLOCKED");
        subtitle.getStyleClass().add("text-subtle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        List<ParkingArea> areas = areaDAO.findAll();
        areaCombo.getItems().addAll(areas);

        ParkingArea preselected = screenManager.getSelectedParkingArea();
        if (preselected != null) {
            for (ParkingArea a : areas) {
                if (a.getAreaId() == preselected.getAreaId()) {
                    areaCombo.setValue(a);
                    break;
                }
            }
        }
        if (areaCombo.getValue() == null && !areas.isEmpty()) {
            areaCombo.setValue(areas.get(0));
        }

        areaCombo.setMinWidth(260);
        areaCombo.setPrefWidth(280);
        areaCombo.setOnAction(e -> {
            if (!suppressComboEvent) {
                ParkingArea sel = areaCombo.getValue();
                if (sel != null) {
                    screenManager.setSelectedParkingArea(sel);
                }
                renderSlots();
            }
        });

        Button addSlotBtn = new Button("+ Add New Slot");
        addSlotBtn.getStyleClass().add("button-primary");
        addSlotBtn.setOnAction(e -> showAddSlotDialog());

        topBox.getChildren().addAll(titleBox, spacer, areaCombo, addSlotBtn);

        // Instructions & Live Zone Summary Strip
        VBox infoCard = new VBox(8);
        infoCard.getStyleClass().add("glass-card");
        infoCard.setPadding(new Insets(14, 20, 14, 20));

        HBox instructStrip = new HBox(14);
        instructStrip.setAlignment(Pos.CENTER_LEFT);

        Label instructLbl = new Label("Quick Status Action: Click a slot to open fast toggle modal (Available / Occupied / Blocked)");
        instructLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #34d399; -fx-font-weight: bold;");

        Region instSpacer = new Region();
        HBox.setHgrow(instSpacer, Priority.ALWAYS);

        Label legend = new Label("🟢 Available   🟡 Reserved   🔴 Occupied   ⚪ Blocked");
        legend.setStyle("-fx-font-size: 12px; -fx-text-fill: #cbd5e1;");

        instructStrip.getChildren().addAll(instructLbl, instSpacer, legend);

        zoneSummaryLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #E8B86D;");
        infoCard.getChildren().addAll(instructStrip, zoneSummaryLbl);

        // Slots Grid
        slotsPane.setHgap(12);
        slotsPane.setVgap(12);
        slotsPane.setPadding(new Insets(20));
        slotsPane.getStyleClass().add("glass-card");
        slotsPane.setStyle("-fx-background-color: rgba(10, 18, 14, 0.55);");

        this.getChildren().addAll(topBox, infoCard, slotsPane);
        renderSlots();
    }

    private void refreshAreaComboAndKeepSelection(int targetAreaId) {
        suppressComboEvent = true;
        try {
            List<ParkingArea> refreshedAreas = areaDAO.findAll();
            areaCombo.getItems().setAll(refreshedAreas);
            ParkingArea matched = null;
            for (ParkingArea a : refreshedAreas) {
                if (a.getAreaId() == targetAreaId) {
                    matched = a;
                    break;
                }
            }
            if (matched == null && !refreshedAreas.isEmpty()) {
                matched = refreshedAreas.get(0);
            }
            areaCombo.setValue(matched);
            if (matched != null) {
                screenManager.setSelectedParkingArea(matched);
            }
        } finally {
            suppressComboEvent = false;
        }
    }

    private void renderSlots() {
        slotsPane.getChildren().clear();
        ParkingArea selectedArea = areaCombo.getValue();
        if (selectedArea == null) return;

        // Ensure slots are provisioned for newly added areas
        areaDAO.ensureSlotsProvisionedForArea(selectedArea);

        List<ParkingSlot> slots = slotDAO.findByArea(selectedArea.getAreaId());

        int availCount = 0;
        int resCount = 0;
        int occCount = 0;
        int blkCount = 0;
        int carCount = 0;
        int bikeCount = 0;

        for (ParkingSlot slot : slots) {
            if ("BIKE".equalsIgnoreCase(slot.getSlotType())) bikeCount++;
            else carCount++;

            Button btn = new Button();
            btn.getStyleClass().add("slot-btn");
            btn.setText(slot.getSlotNumber() + "\n" + slot.getSlotType() + "\n" + slot.getStatus());

            if ("AVAILABLE".equalsIgnoreCase(slot.getStatus())) {
                availCount++;
                btn.getStyleClass().add("slot-btn-available");
            } else if ("RESERVED".equalsIgnoreCase(slot.getStatus())) {
                resCount++;
                btn.getStyleClass().add("slot-btn-reserved");
            } else if ("OCCUPIED".equalsIgnoreCase(slot.getStatus())) {
                occCount++;
                btn.getStyleClass().add("slot-btn-occupied");
            } else {
                blkCount++;
                btn.getStyleClass().add("slot-btn-blocked");
            }

            btn.setOnAction(e -> showStatusChangeDialog(slot));
            slotsPane.getChildren().add(btn);
        }

        zoneSummaryLbl.setText(String.format(
                "📍 %s (%s)  •  Total Slots: %d (🚗 %d Car, 🏍️ %d Bike)   |   🟢 %d Available   •   🟡 %d Reserved   •   🔴 %d Occupied   •   ⚪ %d Blocked",
                selectedArea.getName(),
                selectedArea.getCode(),
                slots.size(),
                carCount,
                bikeCount,
                availCount,
                resCount,
                occCount,
                blkCount
        ));
    }

    private void showAddSlotDialog() {
        StackPane root = (StackPane) screenManager.getPrimaryStage().getScene().getRoot();

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("modal-overlay");

        VBox dialog = new VBox(16);
        dialog.getStyleClass().add("modal-dialog");
        dialog.setMaxWidth(460);

        Label titleLbl = new Label("Add New Parking Slot");
        titleLbl.getStyleClass().add("heading-1");

        Label subLbl = new Label("Add a new bay directly to the Status Monitor and update live zone capacity.");
        subLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);

        ComboBox<ParkingArea> modalAreaCombo = new ComboBox<>();
        List<ParkingArea> areas = areaDAO.findAll();
        modalAreaCombo.getItems().addAll(areas);
        ParkingArea currentSelected = areaCombo.getValue();
        if (currentSelected != null) {
            for (ParkingArea a : areas) {
                if (a.getAreaId() == currentSelected.getAreaId()) {
                    modalAreaCombo.setValue(a);
                    break;
                }
            }
        }
        if (modalAreaCombo.getValue() == null && !areas.isEmpty()) {
            modalAreaCombo.setValue(areas.get(0));
        }
        modalAreaCombo.setMaxWidth(Double.MAX_VALUE);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("CAR", "BIKE");
        typeCombo.setValue("CAR");
        typeCombo.setMaxWidth(Double.MAX_VALUE);

        TextField numField = new TextField(suggestNextSlotNumber(modalAreaCombo.getValue(), typeCombo.getValue()));
        TextField floorField = new TextField("Ground Level");

        Runnable updateSuggested = () -> {
            ParkingArea selA = modalAreaCombo.getValue();
            String selT = typeCombo.getValue() != null ? typeCombo.getValue() : "CAR";
            numField.setText(suggestNextSlotNumber(selA, selT));
            floorField.setText("BIKE".equalsIgnoreCase(selT) ? "North Bay" : "Ground Level");
        };
        modalAreaCombo.setOnAction(ev -> updateSuggested.run());
        typeCombo.setOnAction(ev -> updateSuggested.run());

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("AVAILABLE", "RESERVED", "OCCUPIED", "BLOCKED");
        statusCombo.setValue("AVAILABLE");
        statusCombo.setMaxWidth(Double.MAX_VALUE);

        grid.add(new Label("Parking Zone:"), 0, 0);
        grid.add(modalAreaCombo, 1, 0);
        grid.add(new Label("Slot Type:"), 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(new Label("Slot Identifier:"), 0, 2);
        grid.add(numField, 1, 2);
        grid.add(new Label("Floor / Bay Level:"), 0, 3);
        grid.add(floorField, 1, 3);
        grid.add(new Label("Initial Status:"), 0, 4);
        grid.add(statusCombo, 1, 4);

        HBox btnBox = new HBox(12);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("button-secondary");
        cancelBtn.setOnAction(e -> root.getChildren().remove(overlay));

        Button saveBtn = new Button("Create Slot");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setOnAction(e -> {
            ParkingArea targetArea = modalAreaCombo.getValue();
            if (targetArea == null) return;

            String slotNum = numField.getText() != null ? numField.getText().trim().toUpperCase() : "";
            if (slotNum.isEmpty()) {
                slotNum = suggestNextSlotNumber(targetArea, typeCombo.getValue());
            }

            ParkingSlot newSlot = new ParkingSlot(
                    0,
                    targetArea.getAreaId(),
                    slotNum,
                    typeCombo.getValue() != null ? typeCombo.getValue() : "CAR",
                    floorField.getText() != null && !floorField.getText().trim().isEmpty() ? floorField.getText().trim() : "Ground Level",
                    statusCombo.getValue() != null ? statusCombo.getValue() : "AVAILABLE"
            );
            newSlot.setAreaName(targetArea.getName());
            slotDAO.insert(newSlot);

            root.getChildren().remove(overlay);
            refreshAreaComboAndKeepSelection(targetArea.getAreaId());
            renderSlots();
        });

        btnBox.getChildren().addAll(cancelBtn, saveBtn);
        dialog.getChildren().addAll(titleLbl, subLbl, grid, btnBox);
        overlay.getChildren().add(dialog);
        root.getChildren().add(overlay);
    }

    private String suggestNextSlotNumber(ParkingArea area, String slotType) {
        String code = (area != null && area.getCode() != null && !area.getCode().trim().isEmpty())
                ? area.getCode().trim().toUpperCase()
                : "SJT";
        String typeChar = "BIKE".equalsIgnoreCase(slotType) ? "B" : "C";
        int nextIdx = 1;
        if (area != null) {
            List<ParkingSlot> existing = slotDAO.findByAreaAndType(area.getAreaId(), slotType != null ? slotType : "CAR");
            nextIdx = existing.size() + 1;
        }
        return String.format("%s-%s%02d", code, typeChar, nextIdx);
    }

    private void showStatusChangeDialog(ParkingSlot slot) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("modal-overlay");

        VBox dialog = new VBox(18);
        dialog.getStyleClass().add("modal-dialog");
        dialog.setMaxWidth(420);
        dialog.setAlignment(Pos.CENTER);

        Label titleLbl = new Label("Update Status: " + slot.getSlotNumber());
        titleLbl.getStyleClass().add("heading-1");

        Label curLbl = new Label("Zone: " + slot.getAreaName() + "  •  Current: " + slot.getStatus());
        curLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");

        HBox btnGrid = new HBox(10);
        btnGrid.setAlignment(Pos.CENTER);

        Button availBtn = new Button("🟢 Set Available");
        availBtn.getStyleClass().add("button-secondary");
        availBtn.setOnAction(e -> updateAndClose(slot, "AVAILABLE", overlay));

        Button occBtn = new Button("🔴 Set Occupied");
        occBtn.getStyleClass().add("button-secondary");
        occBtn.setOnAction(e -> updateAndClose(slot, "OCCUPIED", overlay));

        Button blockBtn = new Button("⚪ Set Blocked");
        blockBtn.getStyleClass().add("button-secondary");
        blockBtn.setOnAction(e -> updateAndClose(slot, "BLOCKED", overlay));

        btnGrid.getChildren().addAll(availBtn, occBtn, blockBtn);

        Button cancelBtn = new Button("Close");
        cancelBtn.getStyleClass().add("button-danger");
        cancelBtn.setOnAction(e -> ((StackPane) screenManager.getPrimaryStage().getScene().getRoot()).getChildren().remove(overlay));

        dialog.getChildren().addAll(titleLbl, curLbl, btnGrid, cancelBtn);
        overlay.getChildren().add(dialog);

        ((StackPane) screenManager.getPrimaryStage().getScene().getRoot()).getChildren().add(overlay);
    }

    private void updateAndClose(ParkingSlot slot, String newStatus, StackPane overlay) {
        slotDAO.updateStatus(slot.getSlotId(), newStatus);
        ((StackPane) screenManager.getPrimaryStage().getScene().getRoot()).getChildren().remove(overlay);
        refreshAreaComboAndKeepSelection(slot.getAreaId());
        renderSlots();
    }
}
