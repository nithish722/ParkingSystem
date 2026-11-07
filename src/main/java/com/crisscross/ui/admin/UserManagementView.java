package com.crisscross.ui.admin;

import com.crisscross.dao.UserDAO;
import com.crisscross.models.User;
import com.crisscross.ui.ScreenManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class UserManagementView extends VBox {
    private final UserDAO userDAO = new UserDAO();
    private final TableView<User> table = new TableView<>();
    private final TextField searchField = new TextField();

    public UserManagementView(ScreenManager screenManager) {
        this.setSpacing(20);
        this.setFillWidth(true);

        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("User Account Management");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("Inspect registered customers, administrator credentials, and access permissions");
        subtitle.getStyleClass().add("text-subtle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField.setPromptText("🔍 Search name, email, or role...");
        searchField.setPrefWidth(240);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadUsers());

        topBox.getChildren().addAll(titleBox, spacer, searchField);

        setupTable();
        loadUsers();

        this.getChildren().addAll(topBox, table);
    }

    private void setupTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(480);

        TableColumn<User, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getUserId()));
        colId.setMaxWidth(60);

        TableColumn<User, String> colName = new TableColumn<>("Full Name");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));

        TableColumn<User, String> colEmail = new TableColumn<>("Email Address");
        colEmail.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()));

        TableColumn<User, String> colUser = new TableColumn<>("Username");
        colUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));

        TableColumn<User, String> colPhone = new TableColumn<>("Phone");
        colPhone.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPhone()));

        TableColumn<User, String> colPlate = new TableColumn<>("Vehicle Plate");
        colPlate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVehicleNumber()));

        TableColumn<User, String> colRole = new TableColumn<>("Role");
        colRole.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    User u = getTableRow().getItem();
                    Label b = new Label(u.getRole());
                    if (u.isAdmin()) b.getStyleClass().add("badge-available");
                    else b.getStyleClass().add("badge-reserved");
                    setGraphic(b);
                }
            }
        });

        TableColumn<User, String> colStatus = new TableColumn<>("Account Status");
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    User u = getTableRow().getItem();
                    Label b = new Label(u.getStatus());
                    if ("ACTIVE".equalsIgnoreCase(u.getStatus())) b.getStyleClass().add("badge-available");
                    else b.getStyleClass().add("badge-occupied");
                    setGraphic(b);
                }
            }
        });

        TableColumn<User, Void> colActions = new TableColumn<>("Status Toggle");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button toggleBtn = new Button("Toggle Status");
            {
                toggleBtn.getStyleClass().add("button-secondary");
                toggleBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px;");
                toggleBtn.setOnAction(e -> {
                    User u = getTableRow().getItem();
                    if (u != null) {
                        String newStatus = "ACTIVE".equalsIgnoreCase(u.getStatus()) ? "SUSPENDED" : "ACTIVE";
                        u.setStatus(newStatus);
                        userDAO.update(u);
                        loadUsers();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : toggleBtn);
            }
        });

        table.getColumns().addAll(List.of(colId, colName, colEmail, colUser, colPhone, colPlate, colRole, colStatus, colActions));
    }

    private void loadUsers() {
        table.getItems().clear();
        String q = searchField.getText().toLowerCase().trim();
        List<User> list = userDAO.findAll();
        for (User u : list) {
            if (q.isEmpty() || u.getFullName().toLowerCase().contains(q) || u.getEmail().toLowerCase().contains(q) || u.getUsername().toLowerCase().contains(q) || u.getRole().toLowerCase().contains(q)) {
                table.getItems().add(u);
            }
        }
    }
}







