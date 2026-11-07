package com.crisscross.ui.admin;

import com.crisscross.dao.ParkingAreaDAO;
import com.crisscross.dao.ParkingSlotDAO;
import com.crisscross.models.ParkingArea;
import com.crisscross.models.ParkingSlot;
import com.crisscross.ui.ScreenManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.List;
import java.util.stream.Collectors;

public class SlotManagementView extends VBox {
    private final ScreenManager screenManager;
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final ParkingAreaDAO areaDAO = new ParkingAreaDAO();
    private final TableView<ParkingSlot> table = new TableView<>();

    private final ComboBox<String> areaFilter = new ComboBox<>();
    private final ComboBox<String> typeFilter = new ComboBox<>();
    private final ComboBox<String> statusFilter = new ComboBox<>();

    public SlotManagementView(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.setSpacing(20);
        this.setFillWidth(true);

        // Header
        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Parking Slot Management");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("Configure slot identifiers, bay types (Car/Bike), floor levels, and availability states");
        subtitle.getStyleClass().add("text-subtle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("+ Add New Slot");
        addBtn.getStyleClass().add("button-primary");
        addBtn.setOnAction(e -> showSlotDialog(null));

        topBox.getChildren().addAll(titleBox, spacer, addBtn);

        // Filter Bar
        HBox filterBar = new HBox(14);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("glass-card");
        filterBar.setPadding(new Insets(12, 18, 12, 18));

        Label filterLbl = new Label("Filters:");
        filterLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #cbd5e1;");

        refreshAreaFilterItems();
        areaFilter.setValue("ALL AREAS");

        typeFilter.getItems().addAll("ALL TYPES", "CAR", "BIKE");
        typeFilter.setValue("ALL TYPES");

        statusFilter.getItems().addAll("ALL STATUSES", "AVAILABLE", "RESERVED", "OCCUPIED", "BLOCKED");
        statusFilter.setValue("ALL STATUSES");

        areaFilter.setOnAction(e -> loadSlots());
        typeFilter.setOnAction(e -> loadSlots());
        statusFilter.setOnAction(e -> loadSlots());

        filterBar.getChildren().addAll(filterLbl, areaFilter, typeFilter, statusFilter);

        setupTable();
        loadSlots();

        this.getChildren().addAll(topBox, filterBar, table);
    }

    private void refreshAreaFilterItems() {
        String current = areaFilter.getValue();
        areaFilter.getItems().clear();
        areaFilter.getItems().add("ALL AREAS");
        for (ParkingArea a : areaDAO.findAll()) {
            if (a.getName() != null && !areaFilter.getItems().contains(a.getName())) {
                areaFilter.getItems().add(a.getName());
            }
        }
        if (current != null && areaFilter.getItems().contains(current)) {
            areaFilter.setValue(current);
        } else {
            areaFilter.setValue("ALL AREAS");
        }
    }

    private void setupTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(480);

        TableColumn<ParkingSlot, Number> colId = new TableColumn<>("Slot ID");
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getSlotId()));
        colId.setMaxWidth(70);

        TableColumn<ParkingSlot, String> colArea = new TableColumn<>("Parking Zone");
        colArea.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAreaName() != null ? d.getValue().getAreaName() : "Zone " + d.getValue().getAreaId()));

        TableColumn<ParkingSlot, String> colNum = new TableColumn<>("Slot Number");
        colNum.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSlotNumber()));

        TableColumn<ParkingSlot, String> colType = new TableColumn<>("Bay Type");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSlotType()));

        TableColumn<ParkingSlot, String> colFloor = new TableColumn<>("Deck / Bay Level");
        colFloor.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFloorLevel()));

        TableColumn<ParkingSlot, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    ParkingSlot s = getTableRow().getItem();
                    Label b = new Label(s.getStatus());
                    if ("AVAILABLE".equalsIgnoreCase(s.getStatus())) b.getStyleClass().add("badge-available");
                    else if ("RESERVED".equalsIgnoreCase(s.getStatus())) b.getStyleClass().add("badge-reserved");
                    else if ("OCCUPIED".equalsIgnoreCase(s.getStatus())) b.getStyleClass().add("badge-occupied");
                    else b.getStyleClass().add("badge-blocked");
                    setGraphic(b);
                }
            }
        });

        TableColumn<ParkingSlot, Void> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Delete");
            private final HBox box = new HBox(6, editBtn, delBtn);

            {
                editBtn.getStyleClass().add("button-secondary");
                editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px;");
                delBtn.getStyleClass().add("button-danger");
                delBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px;");

                editBtn.setOnAction(e -> {
                    ParkingSlot s = getTableRow().getItem();
                    if (s != null) showSlotDialog(s);
                });

                delBtn.setOnAction(e -> {
                    ParkingSlot s = getTableRow().getItem();
                    if (s != null) {
                        screenManager.showConfirmationDialog("Delete Slot", "Delete slot " + s.getSlotNumber() + "?", () -> {
                            slotDAO.delete(s.getSlotId());
                            loadSlots();
                        });
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(List.of(colId, colArea, colNum, colType, colFloor, colStatus, colActions));
    }

    private void loadSlots() {
        table.getItems().clear();
        String areaSel = areaFilter.getValue();
        String typeSel = typeFilter.getValue();
        String statusSel = statusFilter.getValue();

        List<ParkingSlot> all = slotDAO.findAll();
        List<ParkingSlot> filtered = all.stream().filter(s -> {
            boolean matchesArea = "ALL AREAS".equals(areaSel) || (s.getAreaName() != null && s.getAreaName().equalsIgnoreCase(areaSel));
            boolean matchesType = "ALL TYPES".equals(typeSel) || s.getSlotType().equalsIgnoreCase(typeSel);
            boolean matchesStatus = "ALL STATUSES".equals(statusSel) || s.getStatus().equalsIgnoreCase(statusSel);
            return matchesArea && matchesType && matchesStatus;
        }).collect(Collectors.toList());

        table.getItems().addAll(filtered);
    }

    private void showSlotDialog(ParkingSlot existing) {
        boolean isEdit = (existing != null);

        StackPane root = (StackPane) screenManager.getPrimaryStage().getScene().getRoot();
        Node underlyingContent = root.getChildren().isEmpty() ? null : root.getChildren().get(0);

        // Apply frosted mist blur strictly to the background beneath this modal port
        if (underlyingContent != null) {
            underlyingContent.setEffect(new GaussianBlur(18.0));
        }

        StackPane overlay = new StackPane();
        overlay.getStyleClass().addAll("modal-overlay", "modal-overlay-mist");
        overlay.setOpacity(0.0);

        VBox dialog = new VBox(20);
        dialog.getStyleClass().addAll("modal-dialog", "modal-dialog-mist");
        dialog.setMaxWidth(490);
        dialog.setPrefWidth(490);
        dialog.setScaleX(0.92);
        dialog.setScaleY(0.92);
        dialog.setOpacity(0.0);

        // Misty Header with Badge & Luminous Accents
        VBox headerBox = new VBox(8);
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label(isEdit ? "✦ EDIT CONFIG PORT" : "✦ ADD SLOT PORT");
        badge.getStyleClass().add("modal-mist-badge");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        topRow.getChildren().addAll(badge, topSpacer);

        Label titleLbl = new Label(isEdit ? "Edit Slot Configuration" : "Add New Parking Slot");
        titleLbl.getStyleClass().add("heading-1");
        titleLbl.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 21px; -fx-font-weight: 800;");

        Label subtitleLbl = new Label("Configure zone assignment, bay type, deck level, and availability state.");
        subtitleLbl.getStyleClass().add("modal-form-sublabel");

        Region divider = new Region();
        divider.getStyleClass().add("modal-mist-divider");

        headerBox.getChildren().addAll(topRow, titleLbl, subtitleLbl, divider);

        // Form Layout
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);

        ColumnConstraints col0 = new ColumnConstraints();
        col0.setMinWidth(155);
        col0.setPrefWidth(165);
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1);

        // Input Controls
        ComboBox<ParkingArea> areaCombo = new ComboBox<>();
        List<ParkingArea> areas = areaDAO.findAll();
        areaCombo.getItems().addAll(areas);
        if (isEdit) {
            for (ParkingArea a : areas) {
                if (a.getAreaId() == existing.getAreaId()) areaCombo.setValue(a);
            }
        } else {
            String currentAreaFilter = areaFilter.getValue();
            if (currentAreaFilter != null && !"ALL AREAS".equals(currentAreaFilter)) {
                for (ParkingArea a : areas) {
                    if (currentAreaFilter.equalsIgnoreCase(a.getName())) {
                        areaCombo.setValue(a);
                        break;
                    }
                }
            }
            if (areaCombo.getValue() == null && screenManager.getSelectedParkingArea() != null) {
                for (ParkingArea a : areas) {
                    if (a.getAreaId() == screenManager.getSelectedParkingArea().getAreaId()) {
                        areaCombo.setValue(a);
                        break;
                    }
                }
            }
        }
        if (areaCombo.getValue() == null && !areas.isEmpty()) areaCombo.setValue(areas.get(0));
        areaCombo.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(areaCombo, Priority.ALWAYS);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("CAR", "BIKE");
        typeCombo.setValue(isEdit ? existing.getSlotType() : "CAR");
        typeCombo.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(typeCombo, Priority.ALWAYS);

        TextField numField = new TextField(isEdit ? existing.getSlotNumber() : suggestNextSlotNumber(areaCombo.getValue(), typeCombo.getValue()));
        numField.setPromptText("e.g. SJT-C16");
        numField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(numField, Priority.ALWAYS);

        TextField floorField = new TextField(isEdit ? existing.getFloorLevel() : "Ground Level");
        floorField.setPromptText("e.g. Ground Level, Deck B");
        floorField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(floorField, Priority.ALWAYS);

        if (!isEdit) {
            Runnable updateSuggestedId = () -> {
                ParkingArea selArea = areaCombo.getValue();
                String selType = typeCombo.getValue() != null ? typeCombo.getValue() : "CAR";
                numField.setText(suggestNextSlotNumber(selArea, selType));
                floorField.setText("BIKE".equalsIgnoreCase(selType) ? "North Bay" : "Ground Level");
            };
            areaCombo.setOnAction(ev -> updateSuggestedId.run());
            typeCombo.setOnAction(ev -> updateSuggestedId.run());
        }

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("AVAILABLE", "RESERVED", "OCCUPIED", "BLOCKED");
        statusCombo.setValue(isEdit ? existing.getStatus() : "AVAILABLE");
        statusCombo.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(statusCombo, Priority.ALWAYS);

        // Form Labels - Crisp, bold white text with luminous icon identifiers
        Label zoneLbl = createFormLabel("Parking Zone:", "📍");
        Label numLbl = createFormLabel("Slot Identifier:", "🏷️");
        Label typeLbl = createFormLabel("Slot Type:", "🚗");
        Label floorLbl = createFormLabel("Floor / Bay Level:", "🏢");
        Label statusLbl = createFormLabel("Initial Status:", "🚦");

        grid.add(zoneLbl, 0, 0);
        grid.add(areaCombo, 1, 0);

        grid.add(numLbl, 0, 1);
        grid.add(numField, 1, 1);

        grid.add(typeLbl, 0, 2);
        grid.add(typeCombo, 1, 2);

        grid.add(floorLbl, 0, 3);
        grid.add(floorField, 1, 3);

        grid.add(statusLbl, 0, 4);
        grid.add(statusCombo, 1, 4);

        // Action Buttons
        HBox btnBox = new HBox(12);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(6, 0, 0, 0));

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("button-secondary");

        Button saveBtn = new Button(isEdit ? "Update Slot" : "Create Slot");
        saveBtn.getStyleClass().add("button-primary");

        // Graceful Dismissal: smooth mist-fade & restoration of underlying screen effect
        Runnable closeAction = () -> {
            FadeTransition fadeOverlay = new FadeTransition(Duration.millis(160), overlay);
            fadeOverlay.setFromValue(1.0);
            fadeOverlay.setToValue(0.0);

            ScaleTransition scaleDialog = new ScaleTransition(Duration.millis(160), dialog);
            scaleDialog.setFromX(1.0);
            scaleDialog.setFromY(1.0);
            scaleDialog.setToX(0.93);
            scaleDialog.setToY(0.93);

            FadeTransition fadeDialog = new FadeTransition(Duration.millis(160), dialog);
            fadeDialog.setFromValue(1.0);
            fadeDialog.setToValue(0.0);

            ParallelTransition exitAnim = new ParallelTransition(fadeOverlay, scaleDialog, fadeDialog);
            exitAnim.setOnFinished(ev -> {
                if (underlyingContent != null) {
                    underlyingContent.setEffect(null);
                }
                root.getChildren().remove(overlay);
            });
            exitAnim.play();
        };

        cancelBtn.setOnAction(e -> closeAction.run());
        overlay.setOnMouseClicked(e -> {
            if (e.getTarget() == overlay) {
                closeAction.run();
            }
        });

        saveBtn.setOnAction(e -> {
            ParkingSlot s = isEdit ? existing : new ParkingSlot();
            String enteredNum = numField.getText() != null ? numField.getText().trim().toUpperCase() : "";
            ParkingArea selectedArea = areaCombo.getValue();

            // If user typed a zone code prefix (e.g. "PRP-C01"), match that area if available
            if (!isEdit && enteredNum.contains("-")) {
                String prefix = enteredNum.substring(0, enteredNum.indexOf('-')).trim();
                for (ParkingArea a : areas) {
                    if (a.getCode() != null && a.getCode().equalsIgnoreCase(prefix)) {
                        selectedArea = a;
                        break;
                    }
                }
            }

            if (selectedArea != null) {
                s.setAreaId(selectedArea.getAreaId());
                s.setAreaName(selectedArea.getName());
                screenManager.setSelectedParkingArea(selectedArea);
            }
            if (enteredNum.isEmpty()) {
                enteredNum = suggestNextSlotNumber(selectedArea, typeCombo.getValue());
            }
            s.setSlotNumber(enteredNum);
            s.setSlotType(typeCombo.getValue() != null ? typeCombo.getValue() : "CAR");
            s.setFloorLevel(floorField.getText() != null && !floorField.getText().trim().isEmpty()
                    ? floorField.getText().trim()
                    : "Ground Level");
            s.setStatus(statusCombo.getValue() != null ? statusCombo.getValue() : "AVAILABLE");

            if (isEdit) {
                slotDAO.update(s);
            } else {
                slotDAO.insert(s);
            }

            closeAction.run();
            refreshAreaFilterItems();
            loadSlots();
        });

        btnBox.getChildren().addAll(cancelBtn, saveBtn);
        dialog.getChildren().addAll(headerBox, grid, btnBox);
        overlay.getChildren().add(dialog);

        root.getChildren().add(overlay);

        // Smooth Mist-In Entrance Animation
        FadeTransition fadeInOverlay = new FadeTransition(Duration.millis(220), overlay);
        fadeInOverlay.setFromValue(0.0);
        fadeInOverlay.setToValue(1.0);

        ScaleTransition scaleInDialog = new ScaleTransition(Duration.millis(240), dialog);
        scaleInDialog.setFromX(0.92);
        scaleInDialog.setFromY(0.92);
        scaleInDialog.setToX(1.0);
        scaleInDialog.setToY(1.0);
        scaleInDialog.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fadeInDialog = new FadeTransition(Duration.millis(240), dialog);
        fadeInDialog.setFromValue(0.0);
        fadeInDialog.setToValue(1.0);

        ParallelTransition enterAnim = new ParallelTransition(fadeInOverlay, scaleInDialog, fadeInDialog);
        enterAnim.play();
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

    private Label createFormLabel(String text, String icon) {
        Label lbl = new Label(icon + "  " + text);
        lbl.getStyleClass().add("modal-form-label");
        lbl.setStyle("-fx-text-fill: #F8FAFC; -fx-font-weight: 700; -fx-font-size: 13px;");
        return lbl;
    }
}
