package com.crisscross.ui.customer;

import com.crisscross.dao.ParkingAreaDAO;
import com.crisscross.dao.ParkingSlotDAO;
import com.crisscross.models.Event;
import com.crisscross.models.ParkingArea;
import com.crisscross.models.ParkingSlot;
import com.crisscross.ui.ScreenManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;

import java.util.List;

public class SlotSelectionView extends VBox {
    private final ScreenManager screenManager;
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final ParkingAreaDAO areaDAO = new ParkingAreaDAO();

    private final Event event;
    private final ParkingArea area;
    private String vehicleType;
    private ParkingSlot selectedSlot;

    private final FlowPane slotGrid = new FlowPane();
    private final Label selectedSlotLbl = new Label("None selected (Click an available green slot)");
    private final Button proceedBtn = new Button("Proceed to Booking Summary →");

    public SlotSelectionView(ScreenManager screenManager, Event event, ParkingArea area, String vehicleType) {
        this.screenManager = screenManager;
        this.event = (event != null) ? event : screenManager.getSelectedEventForBooking();
        this.area = (area != null) ? area : areaDAO.findAll().get(0);
        this.vehicleType = (vehicleType != null) ? vehicleType : "CAR";

        this.setSpacing(22);
        this.setFillWidth(true);

        Button backBtn = new Button("← Back to Parking Availability");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setOnAction(e -> {
            if (this.event != null) {
                screenManager.showParkingAvailability(this.event, this.vehicleType, screenManager.getUserVehicleNumber());
            } else {
                screenManager.showEventList();
            }
        });

        // Header Box
        VBox header = new VBox(6);
        Label stepPill = new Label("STEP 3 OF 3: INTERACTIVE SLOT SELECTION");
        stepPill.setStyle("-fx-background-color: rgba(242, 160, 106, 0.16); -fx-text-fill: #F2A06A; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4px 12px; -fx-background-radius: 10px;");

        Label title = new Label("Select Your Designated Slot in " + this.area.getName());
        title.getStyleClass().add("heading-1");

        String eventInfo = (this.event != null) ? "For Event: " + this.event.getTitle() + " @ " + this.event.getVenue() : "Campus Event Parking";
        Label subtitle = new Label(eventInfo + "  •  Zone: " + this.area.getLocationDesc());
        subtitle.getStyleClass().add("text-subtle");

        header.getChildren().addAll(stepPill, title, subtitle);

        // Vehicle Filter Bar & Legend
        HBox filterBar = new HBox(20);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("glass-card");
        filterBar.setPadding(new Insets(14, 20, 14, 20));

        Label filterLbl = new Label("Slot Category:");
        filterLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #fdf8f5; -fx-font-size: 13px;");

        ToggleGroup tg = new ToggleGroup();
        RadioButton carRadio = new RadioButton("🚗 Four-Wheeler Bays (Car)");
        carRadio.setToggleGroup(tg);
        carRadio.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 13px;");

        RadioButton bikeRadio = new RadioButton("🏍️ Two-Wheeler Bays (Bike)");
        bikeRadio.setToggleGroup(tg);
        bikeRadio.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 13px;");

        if ("BIKE".equalsIgnoreCase(this.vehicleType)) {
            bikeRadio.setSelected(true);
        } else {
            carRadio.setSelected(true);
        }

        tg.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            this.vehicleType = carRadio.isSelected() ? "CAR" : "BIKE";
            this.selectedSlot = null;
            updateProceedButton();
            renderSlots();
        });

        Region filterSpacer = new Region();
        HBox.setHgrow(filterSpacer, Priority.ALWAYS);

        // Legend
        HBox legend = new HBox(14);
        legend.setAlignment(Pos.CENTER);
        legend.getChildren().addAll(
                createStatusLegend("AVAILABLE", "#4ade80"),
                createStatusLegend("RESERVED", "#fbbf24"),
                createStatusLegend("OCCUPIED", "#f87171"),
                createStatusLegend("BLOCKED", "#94a3b8")
        );

        filterBar.getChildren().addAll(filterLbl, carRadio, bikeRadio, filterSpacer, legend);

        // Slots Grid FlowPane
        slotGrid.setHgap(14);
        slotGrid.setVgap(14);
        slotGrid.setPadding(new Insets(20));
        slotGrid.getStyleClass().add("glass-card");
        slotGrid.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, rgba(255, 255, 255, 0.05) 0%, rgba(74, 40, 32, 0.25) 50%, rgba(28, 15, 13, 0.38) 100%); -fx-background-radius: 20px; -fx-border-color: rgba(255, 255, 255, 0.30) rgba(242, 122, 89, 0.30) rgba(224, 109, 83, 0.15) rgba(255, 255, 255, 0.20); -fx-border-radius: 20px; -fx-border-width: 1px;");

        // Bottom Selection Summary Bar
        HBox bottomBar = new HBox(20);
        bottomBar.getStyleClass().add("glass-card-highlight");
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(16, 24, 16, 24));

        VBox slotInfo = new VBox(3);
        Label chosenHeader = new Label("Chosen Parking Bay:");
        chosenHeader.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");
        selectedSlotLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 800; -fx-text-fill: #F2A06A;");
        slotInfo.getChildren().addAll(chosenHeader, selectedSlotLbl);

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        proceedBtn.getStyleClass().add("button-primary");
        proceedBtn.setDisable(true);
        proceedBtn.setOnAction(e -> {
            if (selectedSlot != null) {
                String plate = screenManager.getUserVehicleNumber();
                if (plate == null || plate.isEmpty()) plate = "TN-22-CR-1024";
                screenManager.showBookingSummary(this.event, this.area, selectedSlot, this.vehicleType, plate);
            }
        });

        bottomBar.getChildren().addAll(slotInfo, bottomSpacer, proceedBtn);

        this.getChildren().addAll(backBtn, header, filterBar, slotGrid, bottomBar);
        renderSlots();
    }

    private void renderSlots() {
        slotGrid.getChildren().clear();
        List<ParkingSlot> slots = slotDAO.findByAreaAndType(area.getAreaId(), vehicleType);

        for (ParkingSlot slot : slots) {
            Button btn = new Button();
            btn.getStyleClass().add("slot-btn");

            String icon = "CAR".equalsIgnoreCase(slot.getSlotType()) ? "🚗" : "🏍️";
            btn.setText(icon + "\n" + slot.getSlotNumber() + "\n" + slot.getStatus());

            if (slot.isAvailable()) {
                btn.getStyleClass().add("slot-btn-available");
                btn.setOnAction(e -> {
                    selectedSlot = slot;
                    selectedSlotLbl.setText(slot.getSlotNumber() + "  (" + slot.getSlotType() + " • " + slot.getFloorLevel() + ")");
                    updateProceedButton();
                    // Highlight selected
                    for (var node : slotGrid.getChildren()) {
                        if (node instanceof Button b && b.getText().contains("AVAILABLE")) {
                            b.getStyleClass().remove("slot-btn-selected");
                            if (!b.getStyleClass().contains("slot-btn-available")) b.getStyleClass().add("slot-btn-available");
                        }
                    }
                    btn.getStyleClass().remove("slot-btn-available");
                    btn.getStyleClass().add("slot-btn-selected");
                });
            } else if ("RESERVED".equalsIgnoreCase(slot.getStatus())) {
                btn.getStyleClass().add("slot-btn-reserved");
                btn.setDisable(true);
            } else if ("OCCUPIED".equalsIgnoreCase(slot.getStatus())) {
                btn.getStyleClass().add("slot-btn-occupied");
                btn.setDisable(true);
            } else {
                btn.getStyleClass().add("slot-btn-blocked");
                btn.setDisable(true);
            }

            slotGrid.getChildren().add(btn);
        }
    }

    private void updateProceedButton() {
        proceedBtn.setDisable(selectedSlot == null);
    }

    private HBox createStatusLegend(String text, String hexColor) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER);
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: " + hexColor + "; -fx-font-size: 14px;");
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");
        box.getChildren().addAll(dot, lbl);
        return box;
    }
}
