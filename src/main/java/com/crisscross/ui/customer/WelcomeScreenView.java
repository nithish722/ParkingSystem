package com.crisscross.ui.customer;

import com.crisscross.services.AuthService;
import com.crisscross.ui.ScreenManager;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class WelcomeScreenView extends StackPane {

    public WelcomeScreenView(ScreenManager screenManager) {
        this.setAlignment(Pos.CENTER);

        VBox contentBox = new VBox(28);
        contentBox.setMaxWidth(880);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setStyle("-fx-padding: 30px;");

        // Hero Card
        VBox heroCard = new VBox(18);
        heroCard.getStyleClass().add("glass-card");
        heroCard.setAlignment(Pos.CENTER);
        heroCard.setStyle("-fx-padding: 40px;");

        Label badge = new Label("SMART CAMPUS EVENT PARKING");
        badge.setStyle("-fx-background-color: rgba(242, 160, 106, 0.16); -fx-text-fill: #F2A06A; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6px 18px; -fx-background-radius: 14px; -fx-border-color: rgba(242, 160, 106, 0.40); -fx-border-radius: 14px;");

        Label title = new Label("Welcome to CrissCross");
        title.getStyleClass().add("heading-hero");
        title.setStyle("-fx-font-size: 34px; -fx-text-fill: #ffffff;");

        Label subtitle = new Label("Next-generation intelligent event parking allocation. Powered by proximity-based recommendations for SJT, CDMM, and Woody's parking zones.");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(680);
        subtitle.setStyle("-fx-font-size: 15px; -fx-text-fill: #cbd5e1; -fx-text-alignment: center; -fx-line-spacing: 4px;");

        // 3 Feature Cards in an HBox
        HBox featuresBox = new HBox(18);
        featuresBox.setAlignment(Pos.CENTER);

        VBox card1 = createFeatureCard("🅿️", "SJT Parking", "Closest to Silver Jubilee Tower, North Quad & Tech Auditorium (< 1 min walk).");
        VBox card2 = createFeatureCard("🏢", "CDMM Parking", "Dedicated basement & ramp parking for CDMM Hall & Architecture Block.");
        VBox card3 = createFeatureCard("🌳", "Woody's Parking", "Open perimeter grounds ideal for sports arena, food court & outdoor amphitheater.");

        featuresBox.getChildren().addAll(card1, card2, card3);

        // Actions
        HBox actionsBox = new HBox(16);
        actionsBox.setAlignment(Pos.CENTER);

        Button loginBtn = new Button("Sign In to Your Account");
        loginBtn.getStyleClass().add("button-primary");
        loginBtn.setOnAction(e -> screenManager.showLogin());

        Button registerBtn = new Button("Register New Account");
        registerBtn.getStyleClass().add("button-secondary");
        registerBtn.setOnAction(e -> screenManager.showRegister());

        actionsBox.getChildren().addAll(loginBtn, registerBtn);

        // Quick Demo Launcher Strip for examiners
        HBox demoStrip = new HBox(14);
        demoStrip.setAlignment(Pos.CENTER);
        demoStrip.setStyle("-fx-padding: 12px 0 0 0;");

        Label demoLbl = new Label("Instant Course Demo:");
        demoLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");

        Button quickCustomer = new Button("Demo Customer (Alex)");
        quickCustomer.getStyleClass().add("button-accent");
        quickCustomer.setStyle("-fx-font-size: 11px; -fx-padding: 6px 14px;");
        quickCustomer.setOnAction(e -> {
            try {
                AuthService.getInstance().login("alex", "alex123");
                screenManager.showCustomerDashboard();
            } catch (Exception ex) {
                screenManager.showLogin();
            }
        });

        Button quickAdmin = new Button("Demo Admin (Administrator)");
        quickAdmin.getStyleClass().add("button-accent");
        quickAdmin.setStyle("-fx-font-size: 11px; -fx-padding: 6px 14px; -fx-background-color: linear-gradient(to right, #ea580c, #f97316);");
        quickAdmin.setOnAction(e -> {
            try {
                AuthService.getInstance().login("admin", "admin123");
                screenManager.showAdminDashboard();
            } catch (Exception ex) {
                screenManager.showLogin();
            }
        });

        demoStrip.getChildren().addAll(demoLbl, quickCustomer, quickAdmin);

        heroCard.getChildren().addAll(badge, title, subtitle, featuresBox, actionsBox, demoStrip);
        contentBox.getChildren().add(heroCard);

        this.getChildren().add(contentBox);
    }

    private VBox createFeatureCard(String icon, String title, String desc) {
        VBox card = new VBox(10);
        card.getStyleClass().add("glass-card-interactive");
        card.setPrefWidth(240);
        card.setAlignment(Pos.TOP_LEFT);

        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 24px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #F2A06A;");

        Label descLbl = new Label(desc);
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");

        card.getChildren().addAll(iconLbl, titleLbl, descLbl);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }
}
