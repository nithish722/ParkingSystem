package com.crisscross.ui.customer;

import com.crisscross.services.AuthService;
import com.crisscross.ui.ScreenManager;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ForgotPasswordView extends StackPane {

    public ForgotPasswordView(ScreenManager screenManager) {
        this.setAlignment(Pos.CENTER);

        VBox card = new VBox(20);
        card.getStyleClass().add("glass-card");
        card.setMaxWidth(440);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-padding: 38px;");

        Label icon = new Label("🔑");
        icon.setStyle("-fx-font-size: 32px;");

        Label title = new Label("Password Recovery");
        title.getStyleClass().add("heading-1");

        Label subtitle = new Label("Enter your registered email or username to update your password");
        subtitle.getStyleClass().add("text-subtle");
        subtitle.setWrapText(true);

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 13px; -fx-font-weight: bold;");
        errorLbl.setWrapText(true);
        errorLbl.setVisible(false);

        Label successLbl = new Label();
        successLbl.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 13px; -fx-font-weight: bold;");
        successLbl.setWrapText(true);
        successLbl.setVisible(false);

        VBox form = new VBox(14);

        VBox userBox = new VBox(6);
        Label userLabel = new Label("Username or Email");
        userLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1; -fx-font-weight: 600;");
        TextField userField = new TextField();
        userField.setPromptText("Enter your email or username");
        userBox.getChildren().addAll(userLabel, userField);

        VBox passBox = new VBox(6);
        Label passLabel = new Label("New Password");
        passLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1; -fx-font-weight: 600;");
        PasswordField passField = new PasswordField();
        passField.setPromptText("Enter new password (min 4 chars)");
        passBox.getChildren().addAll(passLabel, passField);

        VBox confirmBox = new VBox(6);
        Label confirmLabel = new Label("Confirm New Password");
        confirmLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1; -fx-font-weight: 600;");
        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("Re-enter new password");
        confirmBox.getChildren().addAll(confirmLabel, confirmPassField);

        form.getChildren().addAll(userBox, passBox, confirmBox);

        Button resetBtn = new Button("Reset Password");
        resetBtn.getStyleClass().add("button-primary");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setOnAction(e -> {
            String p1 = passField.getText();
            String p2 = confirmPassField.getText();
            if (p1 == null || p1.length() < 12) {
                errorLbl.setText("Password must be at least 12 characters long.");
                errorLbl.setVisible(true);
                return;
            }
            if (!p1.equals(p2)) {
                errorLbl.setText("Passwords do not match.");
                errorLbl.setVisible(true);
                return;
            }
            try {
                boolean ok = AuthService.getInstance().resetPassword(userField.getText(), p1);
                if (ok) {
                    errorLbl.setVisible(false);
                    successLbl.setText("Password successfully updated! You can now sign in.");
                    successLbl.setVisible(true);
                } else {
                    errorLbl.setText("Could not update password. Please check username/email.");
                    errorLbl.setVisible(true);
                }
            } catch (Exception ex) {
                errorLbl.setText(ex.getMessage());
                errorLbl.setVisible(true);
            }
        });

        Hyperlink backToLogin = new Hyperlink("← Return to Sign In");
        backToLogin.setStyle("-fx-text-fill: #F2A06A; -fx-font-size: 13px;");
        backToLogin.setOnAction(e -> screenManager.showLogin());

        card.getChildren().addAll(icon, title, subtitle, errorLbl, successLbl, form, resetBtn, backToLogin);
        this.getChildren().add(card);
    }
}
