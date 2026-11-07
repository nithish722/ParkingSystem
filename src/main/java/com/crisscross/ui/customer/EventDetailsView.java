package com.crisscross.ui.customer;

import com.crisscross.models.Event;
import com.crisscross.services.RecommendationService;
import com.crisscross.ui.ScreenManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class EventDetailsView extends VBox {

    public EventDetailsView(ScreenManager screenManager, Event event) {
        this.setSpacing(24);
        this.setFillWidth(true);

        Button backBtn = new Button("← Back to Events");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setOnAction(e -> screenManager.showEventList());

        // Hero Details Card
        VBox heroCard = new VBox(16);
        heroCard.getStyleClass().add("glass-card");
        heroCard.setPadding(new Insets(28));

        HBox topBadges = new HBox(12);
        topBadges.setAlignment(Pos.CENTER_LEFT);
        Label catBadge = new Label(event.getCategory());
        catBadge.setStyle("-fx-background-color: rgba(242, 160, 106, 0.16); -fx-text-fill: #F2A06A; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 5px 12px; -fx-background-radius: 12px;");

        Label statusBadge = new Label(event.getStatus());
        statusBadge.getStyleClass().add("badge-available");

        topBadges.getChildren().addAll(catBadge, statusBadge);

        Label title = new Label(event.getTitle());
        title.getStyleClass().add("heading-hero");
        title.setStyle("-fx-font-size: 28px; -fx-text-fill: #ffffff;");

        Label desc = new Label(event.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1; -fx-line-spacing: 5px;");

        // Key facts grid
        GridPane factsGrid = new GridPane();
        factsGrid.setHgap(30);
        factsGrid.setVgap(14);
        factsGrid.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-background-radius: 16px; -fx-padding: 18px 24px; -fx-border-color: rgba(255, 255, 255, 0.08); -fx-border-radius: 16px;");

        factsGrid.add(createFactItem("📍 Event Venue", event.getVenue()), 0, 0);
        factsGrid.add(createFactItem("📅 Date & Schedule", event.getEventDate() + " at " + event.getEventTime()), 1, 0);
        factsGrid.add(createFactItem("🏛️ Host / Organizer", event.getOrganizer()), 0, 1);
        factsGrid.add(createFactItem("👥 Expected Attendees", event.getExpectedAttendees() + " participants"), 1, 1);

        heroCard.getChildren().addAll(topBadges, title, desc, factsGrid);

        // Smart Proximity Recommendation Preview
        var rec = RecommendationService.getInstance().recommend(event, "CAR");
        VBox recCard = new VBox(12);
        recCard.getStyleClass().add("glass-card-highlight");
        recCard.setPadding(new Insets(20, 24, 20, 24));

        HBox recHeader = new HBox(12);
        recHeader.setAlignment(Pos.CENTER_LEFT);
        Label recIcon = new Label("🎯");
        recIcon.setStyle("-fx-font-size: 22px;");
        Label recTitle = new Label("CrissCross Proximity Match: " + rec.getPrimaryArea().getName());
        recTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #34d399;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label scoreBadge = new Label(rec.getMatchScore() + "% Proximity Score");
        scoreBadge.setStyle("-fx-background-color: rgba(34, 197, 94, 0.2); -fx-text-fill: #4ade80; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 4px 12px; -fx-background-radius: 12px;");

        recHeader.getChildren().addAll(recIcon, recTitle, spacer, scoreBadge);

        Label recDetails = new Label("Walking Distance: " + rec.getWalkingDistance() + " (" + rec.getWalkingMinutes() + " min walk)  •  " + rec.getRecommendationReason());
        recDetails.setWrapText(true);
        recDetails.setStyle("-fx-font-size: 13px; -fx-text-fill: #e2e8f0;");

        HBox actionRow = new HBox(16);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        Button proceedBtn = new Button("Proceed to Vehicle & Slot Booking →");
        proceedBtn.getStyleClass().add("button-primary");
        proceedBtn.setOnAction(e -> screenManager.showVehicleSelection(event));

        actionRow.getChildren().add(proceedBtn);
        recCard.getChildren().addAll(recHeader, recDetails, actionRow);

        this.getChildren().addAll(backBtn, heroCard, recCard);
    }

    private VBox createFactItem(String label, String value) {
        VBox box = new VBox(4);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 14px; -fx-text-fill: #f8fafc; -fx-font-weight: bold;");
        box.getChildren().addAll(l, v);
        return box;
    }
}
