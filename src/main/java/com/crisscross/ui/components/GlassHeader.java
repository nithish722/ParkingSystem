package com.crisscross.ui.components;

import com.crisscross.database.DBConnection;
import com.crisscross.database.MockDatabaseStore;
import com.crisscross.models.User;
import com.crisscross.services.AuthService;
import com.crisscross.ui.ScreenManager;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Apple-inspired Frosted Glass Header Bar in Sunset Ember & Terracotta theme.
 * Contains Brand Logo, Database Live Status Badge, Theme Switcher, User Session Info, and Logout.
 */
public class GlassHeader extends HBox {
    private final ScreenManager screenManager;

    public GlassHeader(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.getStyleClass().add("glass-header");
        this.setAlignment(Pos.CENTER_LEFT);
        this.setSpacing(16);

        // Logo & Title
        HBox brandBox = new HBox(12);
        brandBox.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label("⛶");
        iconLbl.setStyle("-fx-font-size: 26px; -fx-text-fill: #F2A06A; -fx-font-weight: bold;");

        VBox titleBox = new VBox(2);
        Label titleLbl = new Label("CrissCross");
        titleLbl.setStyle("-fx-font-size: 19px; -fx-font-weight: 800; -fx-text-fill: #F2A06A;");
        Label subLbl = new Label("Smart Event Parking System");
        subLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #CBD5E1; -fx-font-weight: 500;");
        titleBox.getChildren().addAll(titleLbl, subLbl);

        brandBox.getChildren().addAll(iconLbl, titleBox);

        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Database status pill
        HBox dbPill = createDatabaseStatusPill();

        // Theme Toggle Button
        Button themeBtn = new Button(screenManager.isDarkMode() ? "☀️ Light Glass" : "🌙 Dark Glass");
        themeBtn.getStyleClass().add("button-secondary");
        themeBtn.setStyle("-fx-font-size: 12px; -fx-padding: 7px 16px;");
        themeBtn.setOnAction(e -> {
            screenManager.toggleTheme();
            themeBtn.setText(screenManager.isDarkMode() ? "☀️ Light Glass" : "🌙 Dark Glass");
        });

        // User session badge
        User user = AuthService.getInstance().getCurrentUser();
        HBox userBox = new HBox(10);
        userBox.setAlignment(Pos.CENTER);
        userBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.08); -fx-background-radius: 16px; -fx-padding: 6px 14px; -fx-border-color: rgba(255, 255, 255, 0.35) rgba(242, 122, 89, 0.30) rgba(224, 109, 83, 0.15) rgba(255, 255, 255, 0.25); -fx-border-radius: 16px;");

        Label avatarLbl = new Label(user != null && user.isAdmin() ? "🛡" : "👤");
        avatarLbl.setStyle("-fx-font-size: 14px;");

        VBox userInfo = new VBox(1);
        Label userNameLbl = new Label(user != null ? user.getFullName() : "Guest User");
        userNameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #fdf8f5;");
        Label userRoleLbl = new Label(user != null ? (user.isAdmin() ? "Administrator" : "Customer") : "Visitor");
        userRoleLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #CBD5E1; -fx-font-weight: 600;");
        userInfo.getChildren().addAll(userNameLbl, userRoleLbl);

        userBox.getChildren().addAll(avatarLbl, userInfo);

        // Logout Button
        Button logoutBtn = new Button("Logout");
        logoutBtn.getStyleClass().add("button-danger");
        logoutBtn.setStyle("-fx-font-size: 12px; -fx-padding: 7px 16px;");
        logoutBtn.setOnAction(e -> screenManager.showLogoutConfirmation());

        this.getChildren().addAll(brandBox, spacer, dbPill, themeBtn, userBox, logoutBtn);
    }

    private HBox createDatabaseStatusPill() {
        HBox pill = new HBox(8);
        pill.setAlignment(Pos.CENTER);
        pill.setStyle("-fx-background-color: rgba(255, 255, 255, 0.08); -fx-background-radius: 16px; -fx-padding: 6px 14px; -fx-border-color: rgba(255, 255, 255, 0.30) rgba(242, 122, 89, 0.25) rgba(224, 109, 83, 0.12) rgba(255, 255, 255, 0.20); -fx-border-radius: 16px;");

        boolean isConnected = DBConnection.getInstance().isMySQLConnected();
        Label dot = new Label("●");
        dot.setStyle("-fx-font-size: 14px; -fx-text-fill: #4ade80;");

        Label text = new Label(isConnected ? "MySQL Connected" : "Local DB Saved");
        text.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #4ade80;");

        pill.getChildren().addAll(dot, text);

        pill.setOnMouseClicked(e -> {
            MockDatabaseStore.getInstance().saveToDisk();
            if (!isConnected) {
                boolean reconnected = DBConnection.getInstance().connect();
                screenManager.showConfirmationDialog("Database Persistence Verified ✓",
                        reconnected
                                ? "Successfully connected to MySQL Database and synchronized local store!"
                                : "All Parking Areas, Slots, Status Changes, Users, Events, and Paid Reservations are automatically saved to disk (crisscross_local_db.dat).",
                        () -> {});
            } else {
                screenManager.showConfirmationDialog("Database Persistence Verified ✓",
                        "Connected to MySQL Database (crisscross_db) with automatic local backup.",
                        () -> {});
            }
        });
        return pill;
    }
}
