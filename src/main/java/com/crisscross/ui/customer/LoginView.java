package com.crisscross.ui.customer;

import com.crisscross.models.User;
import com.crisscross.services.AuthService;
import com.crisscross.ui.ScreenManager;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class LoginView extends StackPane {

    public LoginView(ScreenManager screenManager) {
        this.setAlignment(Pos.CENTER);

        // Glass Card Container
        VBox card = new VBox(20);
        card.getStyleClass().add("glass-card");
        card.setMaxWidth(440);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-padding: 38px;");

        // Header
        Label logo = new Label("⛶");
        logo.setStyle("-fx-font-size: 36px; -fx-text-fill: #F2A06A;");

        Label title = new Label("Welcome Back");
        title.getStyleClass().add("heading-1");

        Label subtitle = new Label("Sign in to manage event parking & reservations");
        subtitle.getStyleClass().add("text-subtle");

        // Error message label
        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 13px; -fx-font-weight: bold;");
        errorLbl.setWrapText(true);
        errorLbl.setVisible(false);

        // Fields
        VBox form = new VBox(14);

        VBox userBox = new VBox(6);
        Label userLabel = new Label("Username or Email");
        userLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #CBD5E1; -fx-font-weight: 600;");
        TextField userField = new TextField();
        userField.setPromptText("e.g. alex or alex@campus.edu");
        userBox.getChildren().addAll(userLabel, userField);

        VBox passBox = new VBox(6);
        Label passLabel = new Label("Password");
        passLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #CBD5E1; -fx-font-weight: 600;");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter your password");
        passBox.getChildren().addAll(passLabel, passField);

        form.getChildren().addAll(userBox, passBox);

        // Quick Auto-fill bar for quick demo
        HBox quickFillBox = new HBox(8);
        quickFillBox.setAlignment(Pos.CENTER_LEFT);
        Label quickLbl = new Label("Fill Demo:");
        quickLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        Button fillCustomer = new Button("Customer (alex)");
        fillCustomer.setStyle("-fx-font-size: 11px; -fx-padding: 4px 10px; -fx-background-color: rgba(242, 160, 106, 0.16); -fx-text-fill: #F2A06A; -fx-background-radius: 10px; -fx-cursor: hand;");
        fillCustomer.setOnAction(e -> {
            userField.setText("alex");
            passField.setText("alex123");
            errorLbl.setVisible(false);
        });

        Button fillVit = new Button("VIT Free (priyavit)");
        fillVit.setStyle("-fx-font-size: 11px; -fx-padding: 4px 10px; -fx-background-color: rgba(52, 211, 153, 0.18); -fx-text-fill: #34d399; -fx-background-radius: 10px; -fx-cursor: hand;");
        fillVit.setOnAction(e -> {
            userField.setText("priyavit");
            passField.setText("Priya#Pass2026");
            errorLbl.setVisible(false);
        });

        Button fillAdmin = new Button("Admin (admin)");
        fillAdmin.setStyle("-fx-font-size: 11px; -fx-padding: 4px 10px; -fx-background-color: rgba(0, 20, 255, 0.22); -fx-text-fill: #38BDF8; -fx-background-radius: 10px; -fx-cursor: hand;");
        fillAdmin.setOnAction(e -> {
            userField.setText("admin");
            passField.setText("admin123");
            errorLbl.setVisible(false);
        });

        quickFillBox.getChildren().addAll(quickLbl, fillCustomer, fillVit, fillAdmin);

        // Links
        HBox optionsBox = new HBox();
        optionsBox.setAlignment(Pos.CENTER_RIGHT);
        Hyperlink forgotLink = new Hyperlink("Forgot Password?");
        forgotLink.setStyle("-fx-text-fill: #F2A06A; -fx-font-size: 12px;");
        forgotLink.setOnAction(e -> screenManager.showForgotPassword());
        optionsBox.getChildren().add(forgotLink);

        // Sign In Button
        Button signInBtn = new Button("Sign In");
        signInBtn.getStyleClass().add("button-primary");
        signInBtn.setMaxWidth(Double.MAX_VALUE);
        signInBtn.setOnAction(e -> {
            try {
                User u = AuthService.getInstance().login(userField.getText(), passField.getText());
                errorLbl.setVisible(false);
                if (u.isAdmin()) {
                    screenManager.showAdminDashboard();
                } else {
                    screenManager.showCustomerDashboard();
                }
            } catch (Exception ex) {
                errorLbl.setText(ex.getMessage());
                errorLbl.setVisible(true);
            }
        });

        // Trigger on Enter
        passField.setOnAction(e -> signInBtn.fire());
        userField.setOnAction(e -> passField.requestFocus());

        // Register Prompt
        HBox registerPrompt = new HBox(6);
        registerPrompt.setAlignment(Pos.CENTER);
        Label noAccLbl = new Label("Don't have an account?");
        noAccLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 13px;");
        Hyperlink registerLink = new Hyperlink("Register Now");
        registerLink.setStyle("-fx-text-fill: #F2A06A; -fx-font-weight: bold; -fx-font-size: 13px;");
        registerLink.setOnAction(e -> screenManager.showRegister());
        registerPrompt.getChildren().addAll(noAccLbl, registerLink);

        // Top bar for back / theme
        HBox topToolbar = new HBox();
        topToolbar.setAlignment(Pos.CENTER_RIGHT);
        Button themeBtn = new Button(screenManager.isDarkMode() ? "☀️" : "🌙");
        themeBtn.getStyleClass().add("button-secondary");
        themeBtn.setStyle("-fx-font-size: 12px; -fx-padding: 4px 10px;");
        themeBtn.setOnAction(e -> {
            screenManager.toggleTheme();
            themeBtn.setText(screenManager.isDarkMode() ? "☀️" : "🌙");
        });
        topToolbar.getChildren().add(themeBtn);

        card.getChildren().addAll(topToolbar, logo, title, subtitle, errorLbl, form, quickFillBox, optionsBox, signInBtn, registerPrompt);
        this.getChildren().add(card);
    }
}
