package com.crisscross.ui.customer;

import com.crisscross.services.AuthService;
import com.crisscross.ui.ScreenManager;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class RegistrationView extends StackPane {

    public RegistrationView(ScreenManager screenManager) {
        this.setAlignment(Pos.CENTER);

        VBox card = new VBox(16);
        card.getStyleClass().add("glass-card");
        card.setMaxWidth(520);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-padding: 34px 40px;");

        Label title = new Label("Create CrissCross Account");
        title.getStyleClass().add("heading-1");

        Label subtitle = new Label("Register for seamless smart event parking passes");
        subtitle.getStyleClass().add("text-subtle");

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 13px; -fx-font-weight: bold;");
        errorLbl.setWrapText(true);
        errorLbl.setVisible(false);

        // Fields Grid
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. David Miller");

        TextField emailField = new TextField();
        emailField.setPromptText("e.g. name@vit.ac.in for Free Pass");

        TextField userField = new TextField();
        userField.setPromptText("e.g. davidm");

        TextField phoneField = new TextField();
        phoneField.setPromptText("e.g. +91 9845012345");

        TextField vehicleField = new TextField();
        vehicleField.setPromptText("e.g. TN-22-CR-5544");

        PasswordField passField = new PasswordField();
        passField.setPromptText("Min 12 chars, 1 caps, 1 digit, 1 special");

        PasswordField confirmPassField = new PasswordField();
        confirmPassField.setPromptText("Re-enter password");

        Label passPolicyLbl = new Label("🔒 Password rules: Minimum 12 characters, at least 1 uppercase letter, 1 number, 1 special symbol, and cannot contain your username.");
        passPolicyLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #E8B86D; -fx-font-weight: 500; -fx-padding: 2px 0 4px 0;");
        passPolicyLbl.setWrapText(true);

        Label vitEmailHint = new Label("🎓 Free Parking: Accounts with a @vit.ac.in email receive 100% complimentary parking passes!");
        vitEmailHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #34d399; -fx-font-weight: 600; -fx-padding: 2px 0 4px 0;");
        vitEmailHint.setWrapText(true);

        grid.add(createFieldLabel("Full Name *"), 0, 0);
        grid.add(nameField, 0, 1);

        grid.add(createFieldLabel("Email Address * (vit.ac.in = FREE)"), 1, 0);
        grid.add(emailField, 1, 1);

        grid.add(createFieldLabel("Username *"), 0, 2);
        grid.add(userField, 0, 3);

        grid.add(createFieldLabel("Mobile Number"), 1, 2);
        grid.add(phoneField, 1, 3);

        grid.add(createFieldLabel("Vehicle Plate Number"), 0, 4);
        grid.add(vehicleField, 0, 5);

        grid.add(createFieldLabel("Password * (12+ chars, 1 caps, 1 special, 1 number)"), 0, 6);
        grid.add(passField, 0, 7);

        grid.add(createFieldLabel("Confirm Password *"), 1, 6);
        grid.add(confirmPassField, 1, 7);

        VBox hintsBox = new VBox(4);
        hintsBox.getChildren().addAll(vitEmailHint, passPolicyLbl);

        // Submit Button
        Button registerBtn = new Button("Complete Registration");
        registerBtn.getStyleClass().add("button-primary");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.setOnAction(e -> {
            try {
                AuthService.getInstance().register(
                        nameField.getText(),
                        emailField.getText(),
                        userField.getText(),
                        passField.getText(),
                        confirmPassField.getText(),
                        phoneField.getText(),
                        vehicleField.getText(),
                        "CUSTOMER"
                );
                errorLbl.setVisible(false);
                screenManager.showCustomerDashboard();
            } catch (Exception ex) {
                errorLbl.setText(ex.getMessage());
                errorLbl.setVisible(true);
            }
        });

        // Already have account
        HBox loginPrompt = new HBox(6);
        loginPrompt.setAlignment(Pos.CENTER);
        Label haveAcc = new Label("Already registered?");
        haveAcc.setStyle("-fx-text-fill: #94A3B8;");
        Hyperlink loginLink = new Hyperlink("Sign In Here");
        loginLink.setStyle("-fx-text-fill: #F2A06A; -fx-font-weight: bold;");
        loginLink.setOnAction(e -> screenManager.showLogin());
        loginPrompt.getChildren().addAll(haveAcc, loginLink);

        card.getChildren().addAll(title, subtitle, errorLbl, grid, hintsBox, registerBtn, loginPrompt);
        this.getChildren().add(card);
    }

    private Label createFieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #CBD5E1; -fx-font-weight: 600;");
        return l;
    }
}
