package com.crisscross.ui.admin;

import com.crisscross.dao.UserDAO;
import com.crisscross.database.DBConnection;
import com.crisscross.models.User;
import com.crisscross.services.AuthService;
import com.crisscross.ui.ScreenManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class AdminProfileView extends VBox {
    private final UserDAO userDAO = new UserDAO();

    public AdminProfileView(ScreenManager screenManager) {
        this.setSpacing(24);
        this.setFillWidth(true);

        User currentAdmin = AuthService.getInstance().getCurrentUser();

        // Header
        VBox header = new VBox(4);
        Label title = new Label("Administrator Profile & System Settings");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("Manage administrator credentials, view security access privileges, and verify database connectivity");
        subtitle.getStyleClass().add("text-subtle");
        header.getChildren().addAll(title, subtitle);

        // Profile Form Card
        VBox card = new VBox(20);
        card.getStyleClass().add("glass-card");
        card.setMaxWidth(720);
        card.setPadding(new Insets(28));

        Label feedbackLbl = new Label();
        feedbackLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");
        feedbackLbl.setVisible(false);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(14);

        TextField nameField = new TextField(currentAdmin != null ? currentAdmin.getFullName() : "System Administrator");
        TextField emailField = new TextField(currentAdmin != null ? currentAdmin.getEmail() : "admin@crisscross.com");
        TextField userField = new TextField(currentAdmin != null ? currentAdmin.getUsername() : "admin");
        userField.setDisable(true);
        TextField phoneField = new TextField(currentAdmin != null ? currentAdmin.getPhone() : "+91 9876543210");

        grid.add(createFieldLabel("Administrator Name"), 0, 0);
        grid.add(nameField, 0, 1);

        grid.add(createFieldLabel("Official Admin Email"), 1, 0);
        grid.add(emailField, 1, 1);

        grid.add(createFieldLabel("Username (Root ID)"), 0, 2);
        grid.add(userField, 0, 3);

        grid.add(createFieldLabel("Contact Hotline"), 1, 2);
        grid.add(phoneField, 1, 3);

        // Security / Password update
        VBox passBox = new VBox(8);
        passBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-background-radius: 14px; -fx-padding: 16px;");

        Label passTitle = new Label("Update Administrator Password");
        passTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #34d399;");

        PasswordField newPass = new PasswordField();
        newPass.setPromptText("Leave blank to retain current master password");
        passBox.getChildren().addAll(passTitle, newPass);

        // Database status & maintenance block
        VBox dbBox = new VBox(10);
        dbBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-background-radius: 14px; -fx-padding: 16px;");

        Label dbTitle = new Label("Database Connectivity Diagnostics");
        dbTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #34d399;");

        boolean isDbConn = DBConnection.getInstance().isMySQLConnected();
        Label dbStatus = new Label("Status: " + (isDbConn ? "🟢 MySQL Server Active on port 3306" : "🟡 Running in local in-memory persistence mode"));
        dbStatus.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (isDbConn ? "#4ade80" : "#fbbf24") + ";");

        HBox dbActions = new HBox(12);
        Button testConnBtn = new Button("Test / Reconnect MySQL");
        testConnBtn.getStyleClass().add("button-secondary");
        testConnBtn.setOnAction(e -> {
            boolean ok = DBConnection.getInstance().connect();
            dbStatus.setText("Status: " + (ok ? "🟢 MySQL Server Active on port 3306" : "🟡 Running in local in-memory persistence mode"));
            dbStatus.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (ok ? "#4ade80" : "#fbbf24") + ";");
        });

        Button initSchemaBtn = new Button("Re-apply Database Schema (schema.sql)");
        initSchemaBtn.getStyleClass().add("button-secondary");
        initSchemaBtn.setOnAction(e -> {
            DBConnection.getInstance().executeSchemaSql();
            feedbackLbl.setText("Schema refresh command dispatched.");
            feedbackLbl.setStyle("-fx-text-fill: #4ade80;");
            feedbackLbl.setVisible(true);
        });

        dbActions.getChildren().addAll(testConnBtn, initSchemaBtn);
        dbBox.getChildren().addAll(dbTitle, dbStatus, dbActions);

        // Save Button
        HBox actionBox = new HBox(16);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button saveBtn = new Button("Save Administrator Settings");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setOnAction(e -> {
            if (currentAdmin != null) {
                currentAdmin.setFullName(nameField.getText().trim());
                currentAdmin.setEmail(emailField.getText().trim());
                currentAdmin.setPhone(phoneField.getText().trim());

                String np = newPass.getText().trim();
                if (!np.isEmpty()) {
                    if (np.length() < 4) {
                        feedbackLbl.setText("Password must be at least 4 characters.");
                        feedbackLbl.setStyle("-fx-text-fill: #f87171;");
                        feedbackLbl.setVisible(true);
                        return;
                    }
                    currentAdmin.setPassword(np);
                }

                boolean ok = userDAO.update(currentAdmin);
                if (ok) {
                    feedbackLbl.setText("Administrator profile updated successfully!");
                    feedbackLbl.setStyle("-fx-text-fill: #4ade80;");
                    feedbackLbl.setVisible(true);
                } else {
                    feedbackLbl.setText("Failed to save changes.");
                    feedbackLbl.setStyle("-fx-text-fill: #f87171;");
                    feedbackLbl.setVisible(true);
                }
            }
        });

        actionBox.getChildren().add(saveBtn);
        card.getChildren().addAll(feedbackLbl, grid, passBox, dbBox, actionBox);

        this.getChildren().addAll(header, card);
    }

    private Label createFieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-text-fill: #cbd5e1; -fx-font-weight: 600;");
        return l;
    }
}
