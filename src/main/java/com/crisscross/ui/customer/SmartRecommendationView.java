package com.crisscross.ui.customer;

import com.crisscross.dao.EventDAO;
import com.crisscross.models.Event;
import com.crisscross.services.RecommendationService;
import com.crisscross.ui.ScreenManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class SmartRecommendationView extends VBox {
    private final ScreenManager screenManager;
    private final EventDAO eventDAO = new EventDAO();
    private final ComboBox<Event> eventCombo = new ComboBox<>();
    private final ToggleGroup vehicleGroup = new ToggleGroup();
    private RadioButton carRadio;
    private RadioButton bikeRadio;
    private final VBox resultContainer = new VBox(18);

    public SmartRecommendationView(ScreenManager screenManager, Event defaultEvent, String defaultVehicle) {
        this.screenManager = screenManager;
        this.setSpacing(24);
        this.setFillWidth(true);

        // Header
        VBox headerBox = new VBox(6);
        Label pill = new Label("INTELLIGENT CAMPUS PROXIMITY ENGINE");
        pill.setStyle("-fx-background-color: rgba(52, 211, 153, 0.15); -fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4px 12px; -fx-background-radius: 10px;");

        Label title = new Label("CrissCross Smart Parking Recommendation");
        title.getStyleClass().add("heading-1");

        Label subtitle = new Label("Calculates the optimal walking distance and slot availability for SJT, CDMM, and Woody's based on your event venue.");
        subtitle.getStyleClass().add("text-subtle");

        headerBox.getChildren().addAll(pill, title, subtitle);

        // Controls Card
        VBox controlsCard = new VBox(16);
        controlsCard.getStyleClass().add("glass-card");
        controlsCard.setPadding(new Insets(24));

        Label controlTitle = new Label("Select Event & Vehicle Parameters:");
        controlTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #34d399;");

        HBox controlsRow = new HBox(24);
        controlsRow.setAlignment(Pos.CENTER_LEFT);

        // Event Combo
        VBox eventBox = new VBox(6);
        Label eventLbl = new Label("Campus Event");
        eventLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #cbd5e1; -fx-font-weight: 600;");
        List<Event> events = eventDAO.findAll();
        eventCombo.getItems().addAll(events);
        if (defaultEvent != null) {
            for (Event e : events) {
                if (e.getEventId() == defaultEvent.getEventId()) {
                    eventCombo.setValue(e);
                    break;
                }
            }
        }
        if (eventCombo.getValue() == null && !events.isEmpty()) {
            eventCombo.setValue(events.get(0));
        }
        eventCombo.setPrefWidth(380);
        eventBox.getChildren().addAll(eventLbl, eventCombo);

        // Vehicle Radios
        VBox vehicleBox = new VBox(6);
        Label vTypeLbl = new Label("Vehicle Mode");
        vTypeLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #cbd5e1; -fx-font-weight: 600;");

        HBox radioBox = new HBox(16);
        radioBox.setAlignment(Pos.CENTER_LEFT);
        carRadio = new RadioButton("🚗 Four-Wheeler (Car)");
        carRadio.setToggleGroup(vehicleGroup);
        carRadio.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 13px;");

        bikeRadio = new RadioButton("🏍️ Two-Wheeler (Bike)");
        bikeRadio.setToggleGroup(vehicleGroup);
        bikeRadio.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 13px;");

        if ("BIKE".equalsIgnoreCase(defaultVehicle)) {
            bikeRadio.setSelected(true);
        } else {
            carRadio.setSelected(true);
        }

        radioBox.getChildren().addAll(carRadio, bikeRadio);
        vehicleBox.getChildren().addAll(vTypeLbl, radioBox);

        controlsRow.getChildren().addAll(eventBox, vehicleBox);
        controlsCard.getChildren().addAll(controlTitle, controlsRow);

        // Listeners for real-time recalculation
        eventCombo.setOnAction(e -> calculateAndDisplay());
        vehicleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> calculateAndDisplay());

        this.getChildren().addAll(headerBox, controlsCard, resultContainer);
        calculateAndDisplay();
    }

    private void calculateAndDisplay() {
        resultContainer.getChildren().clear();

        Event event = eventCombo.getValue();
        if (event == null) return;

        String vehicleType = carRadio.isSelected() ? "CAR" : "BIKE";
        RecommendationService.RecommendationResult rec = RecommendationService.getInstance().recommend(event, vehicleType);

        // Primary Recommendation Card
        VBox primaryCard = new VBox(16);
        primaryCard.getStyleClass().add("glass-card-highlight");
        primaryCard.setPadding(new Insets(24, 28, 24, 28));

        HBox badgeRow = new HBox(12);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        Label topBadge = new Label("TOP RECOMMENDED PARKING AREA");
        topBadge.setStyle("-fx-background-color: rgba(34, 197, 94, 0.22); -fx-text-fill: #4ade80; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5px 12px; -fx-background-radius: 12px;");

        Label scoreBadge = new Label(rec.getMatchScore() + "% Match Score");
        scoreBadge.setStyle("-fx-background-color: rgba(52, 211, 153, 0.2); -fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5px 12px; -fx-background-radius: 12px;");

        badgeRow.getChildren().addAll(topBadge, scoreBadge);

        Label areaName = new Label("🅿️ " + rec.getPrimaryArea().getName());
        areaName.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

        Label venueProximity = new Label("Directly Servicing: " + event.getVenue() + " (" + rec.getWalkingDistance() + " • ~" + rec.getWalkingMinutes() + " min walk)");
        venueProximity.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #34d399;");

        Label reasonLbl = new Label(rec.getRecommendationReason());
        reasonLbl.setWrapText(true);
        reasonLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1;");

        // Action Row
        HBox primaryAction = new HBox(16);
        primaryAction.setAlignment(Pos.CENTER_RIGHT);

        Button pickSlotBtn = new Button("Choose Slot in " + rec.getPrimaryArea().getName() + " →");
        pickSlotBtn.getStyleClass().add("button-primary");
        pickSlotBtn.setOnAction(e -> screenManager.showSlotSelection(event, rec.getPrimaryArea(), vehicleType));

        primaryAction.getChildren().add(pickSlotBtn);
        primaryCard.getChildren().addAll(badgeRow, areaName, venueProximity, reasonLbl, primaryAction);

        // Backup Area Card
        VBox backupCard = new VBox(14);
        backupCard.getStyleClass().add("glass-card");
        backupCard.setPadding(new Insets(20, 24, 20, 24));

        HBox backupTop = new HBox(12);
        backupTop.setAlignment(Pos.CENTER_LEFT);

        Label backupBadge = new Label("ALTERNATIVE BACKUP AREA");
        backupBadge.setStyle("-fx-background-color: rgba(245, 158, 11, 0.2); -fx-text-fill: #fbbf24; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4px 10px; -fx-background-radius: 10px;");

        Label backupName = new Label(rec.getBackupArea().getName());
        backupName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Region backupSpacer = new Region();
        HBox.setHgrow(backupSpacer, Priority.ALWAYS);

        Button backupBtn = new Button("Select Backup Area");
        backupBtn.getStyleClass().add("button-secondary");
        backupBtn.setOnAction(e -> screenManager.showSlotSelection(event, rec.getBackupArea(), vehicleType));

        backupTop.getChildren().addAll(backupBadge, backupName, backupSpacer, backupBtn);

        Label backupDesc = new Label("Location: " + rec.getBackupArea().getLocationDesc() + " • Secondary backup option if " + rec.getPrimaryArea().getName() + " gets congested during peak arrival.");
        backupDesc.setWrapText(true);
        backupDesc.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        backupCard.getChildren().addAll(backupTop, backupDesc);

        resultContainer.getChildren().addAll(primaryCard, backupCard);
    }
}
