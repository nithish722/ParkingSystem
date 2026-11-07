package com.crisscross.ui.customer;

import com.crisscross.models.Event;
import com.crisscross.models.User;
import com.crisscross.services.AuthService;
import com.crisscross.ui.ScreenManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

public class VehicleSelectionView extends VBox {
    private String selectedVehicleType = "CAR";
    private VBox carCard;
    private VBox bikeCard;

    public VehicleSelectionView(ScreenManager screenManager, Event event) {
        this.setSpacing(24);
        this.setFillWidth(true);

        User currentUser = AuthService.getInstance().getCurrentUser();
        String initialPlate = currentUser != null && currentUser.getVehicleNumber() != null ? currentUser.getVehicleNumber() : "TN-22-CR-1024";

        Button backBtn = new Button("← Back to Event Details");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setOnAction(e -> screenManager.showEventDetails(event));

        // Header
        VBox headerBox = new VBox(6);
        Label stepPill = new Label("STEP 1 OF 3: VEHICLE CLASSIFICATION");
        stepPill.setStyle("-fx-background-color: rgba(52, 211, 153, 0.15); -fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4px 12px; -fx-background-radius: 10px;");

        Label title = new Label("Select Your Vehicle Type");
        title.getStyleClass().add("heading-1");

        Label subtitle = new Label("CrissCross designates tailored parking zones for Four-Wheelers and Two-Wheelers near " + event.getVenue() + ".");
        subtitle.getStyleClass().add("text-subtle");

        headerBox.getChildren().addAll(stepPill, title, subtitle);

        // Vehicle Cards HBox
        HBox cardsBox = new HBox(20);
        cardsBox.setAlignment(Pos.CENTER);

        carCard = createVehicleOptionCard("🚗", "Four-Wheeler (Car)", "Sedan, SUV, Hatchback or EV. Allocates standard ground and deck automotive bays.", "CAR");
        bikeCard = createVehicleOptionCard("🏍️", "Two-Wheeler (Bike)", "Motorcycle, Scooter or Moped. Allocates shaded perimeter canopy slots with quick campus exit.", "BIKE");

        cardsBox.getChildren().addAll(carCard, bikeCard);
        updateCardStyles();

        // Vehicle Plate Input
        VBox plateCard = new VBox(10);
        plateCard.getStyleClass().add("glass-card");
        plateCard.setMaxWidth(600);
        plateCard.setAlignment(Pos.CENTER_LEFT);

        Label plateLabel = new Label("Vehicle Registration / Plate Number");
        plateLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #f8fafc;");

        TextField plateField = new TextField(initialPlate);
        plateField.setPromptText("e.g. TN-22-CR-1024");
        plateField.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #34d399;");

        Label plateNote = new Label("This registration number will be encoded onto your official event entry pass.");
        plateNote.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        plateCard.getChildren().addAll(plateLabel, plateField, plateNote);

        // Actions
        HBox actionBox = new HBox(16);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button continueBtn = new Button("View Parking Recommendations →");
        continueBtn.getStyleClass().add("button-primary");
        continueBtn.setOnAction(e -> {
            String plate = plateField.getText().trim();
            if (plate.isEmpty()) plate = "TN-22-CR-1024";
            screenManager.showParkingAvailability(event, selectedVehicleType, plate);
        });

        actionBox.getChildren().add(continueBtn);

        this.getChildren().addAll(backBtn, headerBox, cardsBox, plateCard, actionBox);
    }

    private VBox createVehicleOptionCard(String icon, String title, String desc, String type) {
        VBox card = new VBox(12);
        card.getStyleClass().add("glass-card-interactive");
        card.setPadding(new Insets(24));
        card.setPrefWidth(280);
        card.setAlignment(Pos.CENTER);
        HBox.setHgrow(card, Priority.ALWAYS);

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 42px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");

        Label descLbl = new Label(desc);
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-text-alignment: center;");

        card.getChildren().addAll(iconLbl, titleLbl, descLbl);

        card.setOnMouseClicked(e -> {
            selectedVehicleType = type;
            updateCardStyles();
        });

        return card;
    }

    private void updateCardStyles() {
        if ("CAR".equalsIgnoreCase(selectedVehicleType)) {
            carCard.setStyle("-fx-background-color: rgba(52, 211, 153, 0.16); -fx-border-color: #34d399; -fx-border-width: 2px; -fx-background-radius: 20px; -fx-border-radius: 20px;");
            bikeCard.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-border-color: rgba(255, 255, 255, 0.12); -fx-border-width: 1px; -fx-background-radius: 20px; -fx-border-radius: 20px;");
        } else {
            bikeCard.setStyle("-fx-background-color: rgba(52, 211, 153, 0.16); -fx-border-color: #34d399; -fx-border-width: 2px; -fx-background-radius: 20px; -fx-border-radius: 20px;");
            carCard.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-border-color: rgba(255, 255, 255, 0.12); -fx-border-width: 1px; -fx-background-radius: 20px; -fx-border-radius: 20px;");
        }
    }
}
