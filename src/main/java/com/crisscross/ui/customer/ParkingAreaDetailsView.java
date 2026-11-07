package com.crisscross.ui.customer;

import com.crisscross.dao.ParkingSlotDAO;
import com.crisscross.models.ParkingArea;
import com.crisscross.models.ParkingSlot;
import com.crisscross.ui.ScreenManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;

import java.util.List;

public class ParkingAreaDetailsView extends VBox {
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();

    public ParkingAreaDetailsView(ScreenManager screenManager, ParkingArea area) {
        this.setSpacing(24);
        this.setFillWidth(true);

        Button backBtn = new Button("← Back to Parking Overview");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setOnAction(e -> screenManager.showParkingMapView());

        // Area Details Hero Card
        VBox heroCard = new VBox(16);
        heroCard.getStyleClass().add("glass-card");
        heroCard.setPadding(new Insets(28));

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label("🅿️ " + area.getName());
        nameLbl.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(area.getStatus());
        statusBadge.getStyleClass().add("badge-available");

        topRow.getChildren().addAll(nameLbl, spacer, statusBadge);

        Label descLbl = new Label(area.getLocationDesc());
        descLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");

        // Stat Grid
        List<ParkingSlot> allSlots = slotDAO.findByArea(area.getAreaId());
        long freeCount = allSlots.stream().filter(ParkingSlot::isAvailable).count();
        long carSlots = allSlots.stream().filter(s -> "CAR".equalsIgnoreCase(s.getSlotType())).count();
        long bikeSlots = allSlots.stream().filter(s -> "BIKE".equalsIgnoreCase(s.getSlotType())).count();

        GridPane grid = new GridPane();
        grid.setHgap(30);
        grid.setVgap(14);
        grid.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-background-radius: 16px; -fx-padding: 18px 24px; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 16px;");

        grid.add(createItem("📍 Primary Servicing Venue", area.getNearestVenue()), 0, 0);
        grid.add(createItem("⏱️ Walking Distance", area.getWalkingTimeMins() + " min direct access"), 1, 0);
        grid.add(createItem("🚗 Four-Wheeler Capacity", carSlots + " Designated Bays"), 0, 1);
        grid.add(createItem("🏍️ Two-Wheeler Capacity", bikeSlots + " Covered Bays"), 1, 1);
        grid.add(createItem("🟢 Currently Available", freeCount + " Free Slots"), 0, 2);
        grid.add(createItem("💳 Event Parking Fee", "Complimentary / Event Pass Included"), 1, 2);

        // Occupancy bar
        double frac = area.getTotalCapacity() > 0 ? (double) area.getOccupiedCount() / area.getTotalCapacity() : 0.0;
        ProgressBar pb = new ProgressBar(frac);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(8);
        pb.setStyle("-fx-accent: " + (frac > 0.85 ? "#ef4444" : "#22c55e") + ";");

        heroCard.getChildren().addAll(topRow, descLbl, grid, pb);

        // Action Box
        HBox actionBox = new HBox(16);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button pickSlotBtn = new Button("Select Slot in " + area.getCode() + " →");
        pickSlotBtn.getStyleClass().add("button-primary");
        pickSlotBtn.setOnAction(e -> screenManager.showSlotSelection(
                screenManager.getSelectedEventForBooking(),
                area,
                screenManager.getSelectedVehicleType()
        ));

        actionBox.getChildren().add(pickSlotBtn);

        this.getChildren().addAll(backBtn, heroCard, actionBox);
    }

    private VBox createItem(String label, String val) {
        VBox box = new VBox(3);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");
        Label v = new Label(val);
        v.setStyle("-fx-font-size: 14px; -fx-text-fill: #f8fafc; -fx-font-weight: bold;");
        box.getChildren().addAll(l, v);
        return box;
    }
}
