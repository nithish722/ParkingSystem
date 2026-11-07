package com.crisscross.ui.customer;

import com.crisscross.dao.ParkingAreaDAO;
import com.crisscross.dao.ParkingSlotDAO;
import com.crisscross.models.Event;
import com.crisscross.models.ParkingArea;
import com.crisscross.models.ParkingSlot;
import com.crisscross.services.RecommendationService;
import com.crisscross.ui.ScreenManager;
import com.crisscross.ui.components.VitCampusLiveMapView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;

import java.util.List;

public class ParkingAvailabilityView extends VBox {
    private final ScreenManager screenManager;
    private final ParkingAreaDAO areaDAO = new ParkingAreaDAO();
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();

    private final StackPane viewSwitcherContainer = new StackPane();
    private final HBox areasCardBox = new HBox(18);
    private final VBox liveMapBox = new VBox(14);
    private final VitCampusLiveMapView liveMapView;

    private ParkingArea mapSelectedArea;
    private final Label mapSelectionLabel = new Label();
    private final Button mapProceedBtn = new Button();

    public ParkingAvailabilityView(ScreenManager screenManager, Event event, String vehicleType, String vehicleNumber) {
        this.screenManager = screenManager;
        this.setSpacing(22);
        this.setFillWidth(true);

        Button backBtn = new Button("← Change Vehicle");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setOnAction(e -> screenManager.showVehicleSelection(event));

        // Header
        VBox headerBox = new VBox(6);
        Label stepPill = new Label("STEP 2 OF 3: PARKING AVAILABILITY & CAPACITY");
        stepPill.setStyle("-fx-background-color: rgba(242, 160, 106, 0.16); -fx-text-fill: #F2A06A; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4px 12px; -fx-background-radius: 10px;");

        Label title = new Label("Available Parking Zones for " + event.getTitle());
        title.getStyleClass().add("heading-1");

        Label subtitle = new Label("Venue: " + event.getVenue() + "  •  Vehicle: " + vehicleType + " (" + vehicleNumber + ")");
        subtitle.getStyleClass().add("text-subtle");

        headerBox.getChildren().addAll(stepPill, title, subtitle);

        // Smart Recommendation Highlight Banner
        var rec = RecommendationService.getInstance().recommend(event, vehicleType);
        VBox recHighlight = new VBox(10);
        recHighlight.getStyleClass().add("glass-card-highlight");
        recHighlight.setPadding(new Insets(18, 24, 18, 24));

        HBox recTop = new HBox(12);
        recTop.setAlignment(Pos.CENTER_LEFT);
        Label recIcon = new Label("⭐");
        recIcon.setStyle("-fx-font-size: 20px;");
        Label recTitle = new Label("Smart Recommended Choice: " + rec.getPrimaryArea().getName());
        recTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #F2A06A;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label recScore = new Label(rec.getMatchScore() + "% Proximity Match");
        recScore.setStyle("-fx-background-color: rgba(74, 222, 128, 0.2); -fx-text-fill: #4ade80; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4px 12px; -fx-background-radius: 12px;");
        recTop.getChildren().addAll(recIcon, recTitle, spacer, recScore);

        Label recDesc = new Label(rec.getRecommendationReason() + " Walking distance: " + rec.getWalkingDistance() + " (" + rec.getWalkingMinutes() + " min walk).");
        recDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #fdf8f5;");

        recHighlight.getChildren().addAll(recTop, recDesc);

        // View Mode Switcher Header
        HBox viewToggleRow = new HBox(12);
        viewToggleRow.setAlignment(Pos.CENTER_LEFT);

        Label viewPrompt = new Label("Choose Destination Parking Area:");
        viewPrompt.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Region toggleSpacer = new Region();
        HBox.setHgrow(toggleSpacer, Priority.ALWAYS);

        Button blueprintBtn = new Button("📐 2D Floor Plan Blueprint →");
        blueprintBtn.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #E8B86D 0%, #F2A06A 50%, #F16E68 100%); -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 7px 16px; -fx-background-radius: 12px; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(241, 110, 104, 0.40), 10, 0.2, 0, 2);");
        blueprintBtn.setOnAction(e -> screenManager.showParkingMapView());

        viewToggleRow.getChildren().addAll(viewPrompt, toggleSpacer, blueprintBtn);

        // 1. Build 3 Parking Areas Cards
        List<ParkingArea> areas = areaDAO.findAll();
        for (ParkingArea area : areas) {
            boolean isPrimaryRec = (area.getAreaId() == rec.getPrimaryArea().getAreaId());
            boolean isBackupRec = (area.getAreaId() == rec.getBackupArea().getAreaId());

            VBox card = createAreaAvailabilityCard(area, event, vehicleType, vehicleNumber, isPrimaryRec, isBackupRec);
            areasCardBox.getChildren().add(card);
        }

        // 2. Build Live Map Section
        this.mapSelectedArea = rec.getPrimaryArea();
        liveMapView = new VitCampusLiveMapView(event, selected -> updateMapSelection(selected, event, vehicleType));
        liveMapView.setMinHeight(500);
        liveMapView.setPrefHeight(580);
        VBox.setVgrow(liveMapView, Priority.ALWAYS);

        HBox mapSelectionStrip = createMapSelectionStrip(event, vehicleType);
        liveMapBox.getChildren().addAll(liveMapView, mapSelectionStrip);
        VBox.setVgrow(liveMapBox, Priority.ALWAYS);

        updateMapSelection(rec.getPrimaryArea(), event, vehicleType);

        // Default view: Cards
        viewSwitcherContainer.getChildren().setAll(areasCardBox);

        this.getChildren().addAll(backBtn, headerBox, recHighlight, viewToggleRow, viewSwitcherContainer);
    }

    private HBox createMapSelectionStrip(Event event, String vehicleType) {
        HBox strip = new HBox(16);
        strip.setAlignment(Pos.CENTER_LEFT);
        strip.getStyleClass().add("glass-card-highlight");
        strip.setPadding(new Insets(14, 20, 14, 20));
        strip.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, rgba(255, 255, 255, 0.07) 0%, rgba(18, 42, 28, 0.22) 50%, rgba(6, 16, 10, 0.30) 100%); -fx-background-radius: 18px; -fx-border-color: rgba(255, 255, 255, 0.36) rgba(52, 211, 153, 0.45) rgba(52, 211, 153, 0.15) rgba(255, 255, 255, 0.24); -fx-border-radius: 18px; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.22), 20, 0.08, 0, 6);");

        VBox textBox = new VBox(2);
        Label titleLbl = new Label("DESTINATION SELECTED FROM VIT LIVE MAP:");
        titleLbl.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #34d399;");

        mapSelectionLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        textBox.getChildren().addAll(titleLbl, mapSelectionLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        mapProceedBtn.getStyleClass().add("button-primary");
        mapProceedBtn.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8px 20px;");
        mapProceedBtn.setOnAction(e -> {
            if (mapSelectedArea != null) {
                screenManager.showSlotSelection(event, mapSelectedArea, vehicleType);
            }
        });

        strip.getChildren().addAll(textBox, spacer, mapProceedBtn);
        return strip;
    }

    private void updateMapSelection(ParkingArea area, Event event, String vehicleType) {
        if (area == null) return;
        this.mapSelectedArea = area;
        mapSelectionLabel.setText("📍 " + area.getName() + "  •  " + area.getAvailableCapacity() + "/" + area.getTotalCapacity() + " slots free (" + area.getWalkingTimeMins() + " min walk to " + area.getNearestVenue() + ")");
        mapProceedBtn.setText("Confirm " + area.getCode() + " & Pick Slot →");
    }

    private VBox createAreaAvailabilityCard(ParkingArea area, Event event, String vehicleType, String vehicleNumber,
                                            boolean isPrimary, boolean isBackup) {
        VBox card = new VBox(12);
        card.getStyleClass().add(isPrimary ? "glass-card-highlight" : "glass-card");
        card.setPadding(new Insets(20));
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label(area.getName());
        nameLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (isPrimary) {
            Label badge = new Label("BEST MATCH");
            badge.setStyle("-fx-background-color: rgba(34, 197, 94, 0.22); -fx-text-fill: #4ade80; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 8px;");
            topRow.getChildren().addAll(nameLbl, spacer, badge);
        } else if (isBackup) {
            Label badge = new Label("BACKUP AREA");
            badge.setStyle("-fx-background-color: rgba(0, 20, 255, 0.20); -fx-text-fill: #38BDF8; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 8px;");
            topRow.getChildren().addAll(nameLbl, spacer, badge);
        } else {
            topRow.getChildren().addAll(nameLbl, spacer);
        }

        Label locLbl = new Label("📍 " + area.getLocationDesc());
        locLbl.setWrapText(true);
        locLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94A3B8;");

        // Specific slots available for this vehicle type
        List<ParkingSlot> slots = slotDAO.findByAreaAndType(area.getAreaId(), vehicleType);
        long availableVehicleSlots = slots.stream().filter(ParkingSlot::isAvailable).count();

        HBox slotStat = new HBox(8);
        slotStat.setAlignment(Pos.BASELINE_LEFT);
        Label availNum = new Label(String.valueOf(availableVehicleSlots));
        availNum.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #4ade80;");
        Label availDesc = new Label("Available " + vehicleType + " Slots (out of " + slots.size() + ")");
        availDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #CBD5E1;");
        slotStat.getChildren().addAll(availNum, availDesc);

        double fraction = slots.isEmpty() ? 0.0 : (double) (slots.size() - availableVehicleSlots) / slots.size();
        ProgressBar pb = new ProgressBar(fraction);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(6);
        pb.setStyle("-fx-accent: " + (fraction > 0.85 ? "#ef4444" : "#F16E68") + ";");

        Button selectBtn = new Button("Select " + area.getCode() + " & Pick Slot →");
        selectBtn.getStyleClass().add(isPrimary ? "button-primary" : "button-secondary");
        selectBtn.setMaxWidth(Double.MAX_VALUE);
        selectBtn.setOnAction(e -> screenManager.showSlotSelection(event, area, vehicleType));

        card.getChildren().addAll(topRow, locLbl, slotStat, pb, selectBtn);
        return card;
    }
}
