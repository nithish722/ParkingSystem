package com.crisscross.ui.customer;

import com.crisscross.dao.UserDAO;
import com.crisscross.models.User;
import com.crisscross.services.AuthService;
import com.crisscross.ui.ScreenManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class ProfileView extends VBox {
    private final UserDAO userDAO = new UserDAO();

    public ProfileView(ScreenManager screenManager) {
        this.setSpacing(24);
        this.setFillWidth(true);

        User currentUser = AuthService.getInstance().getCurrentUser();

        // Header
        VBox header = new VBox(4);
        Label title = new Label("My Profile & Vehicle Settings");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("Update your personal contact details, default vehicle plate number, or change security credentials");
        subtitle.getStyleClass().add("text-subtle");
        header.getChildren().addAll(title, subtitle);

        // Profile Form Card
        VBox card = new VBox(20);
        card.getStyleClass().add("glass-card");
        card.setMaxWidth(680);
        card.setPadding(new Insets(28));

        Label feedbackLbl = new Label();
        feedbackLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        feedbackLbl.setVisible(false);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(14);

        TextField nameField = new TextField(currentUser != null ? currentUser.getFullName() : "");
        TextField emailField = new TextField(currentUser != null ? currentUser.getEmail() : "");
        TextField userField = new TextField(currentUser != null ? currentUser.getUsername() : "");
        userField.setDisable(true); // Username immutable
        TextField phoneField = new TextField(currentUser != null ? currentUser.getPhone() : "");
        TextField vehicleField = new TextField(currentUser != null ? currentUser.getVehicleNumber() : "");

        grid.add(createFieldLabel("Full Name"), 0, 0);
        grid.add(nameField, 0, 1);

        grid.add(createFieldLabel("Email Address"), 1, 0);
        grid.add(emailField, 1, 1);

        grid.add(createFieldLabel("Username (Account ID)"), 0, 2);
        grid.add(userField, 0, 3);

        grid.add(createFieldLabel("Phone Number"), 1, 2);
        grid.add(phoneField, 1, 3);

        grid.add(createFieldLabel("Default Vehicle Plate Number"), 0, 4);
        grid.add(vehicleField, 0, 5);

        // Password change section
        VBox passSection = new VBox(10);
        passSection.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-background-radius: 14px; -fx-padding: 16px;");

        Label passTitle = new Label("Change Account Password (Optional)");
        passTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #34d399;");

        PasswordField newPassField = new PasswordField();
        newPassField.setPromptText("Leave blank to keep current password");

        passSection.getChildren().addAll(passTitle, newPassField);

        // Save Button
        HBox actionBox = new HBox(16);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button saveBtn = new Button("Save Changes");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setOnAction(e -> {
            if (currentUser != null) {
                currentUser.setFullName(nameField.getText().trim());
                currentUser.setEmail(emailField.getText().trim());
                currentUser.setPhone(phoneField.getText().trim());
                currentUser.setVehicleNumber(vehicleField.getText().trim());

                String newPass = newPassField.getText().trim();
                if (!newPass.isEmpty()) {
                    if (newPass.length() < 4) {
                        feedbackLbl.setText("Password must be at least 4 characters.");
                        feedbackLbl.setStyle("-fx-text-fill: #f87171;");
                        feedbackLbl.setVisible(true);
                        return;
                    }
                    currentUser.setPassword(newPass);
                }

                boolean ok = userDAO.update(currentUser);
                if (ok) {
                    feedbackLbl.setText("Profile settings saved successfully!");
                    feedbackLbl.setStyle("-fx-text-fill: #4ade80;");
                    feedbackLbl.setVisible(true);
                } else {
                    feedbackLbl.setText("Failed to save profile. Please try again.");
                    feedbackLbl.setStyle("-fx-text-fill: #f87171;");
                    feedbackLbl.setVisible(true);
                }
            }
        });

        actionBox.getChildren().add(saveBtn);
        card.getChildren().addAll(feedbackLbl, grid, passSection, actionBox);

        this.getChildren().addAll(header, card);
    }

    private Label createFieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #cbd5e1; -fx-font-weight: 600;");
        return l;
    }
}
