package com.crisscross.ui.admin;

import com.crisscross.dao.NotificationDAO;
import com.crisscross.models.Notification;
import com.crisscross.ui.ScreenManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class NotificationManagementView extends VBox {
    private final ScreenManager screenManager;
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final TableView<Notification> table = new TableView<>();

    public NotificationManagementView(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.setSpacing(20);
        this.setFillWidth(true);

        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Notification & Announcement Management");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("Broadcast real-time campus traffic advisories, gate announcements, and security bulletins");
        subtitle.getStyleClass().add("text-subtle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button broadcastBtn = new Button("📢 Create New Announcement");
        broadcastBtn.getStyleClass().add("button-primary");
        broadcastBtn.setOnAction(e -> showAnnouncementDialog());

        topBox.getChildren().addAll(titleBox, spacer, broadcastBtn);

        setupTable();
        loadNotifications();

        this.getChildren().addAll(topBox, table);
    }

    private void setupTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(480);

        TableColumn<Notification, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getNotificationId()));
        colId.setMaxWidth(60);

        TableColumn<Notification, String> colPriority = new TableColumn<>("Priority");
        colPriority.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Notification n = getTableRow().getItem();
                    Label b = new Label(n.getPriority());
                    if ("URGENT".equalsIgnoreCase(n.getPriority())) b.getStyleClass().add("badge-occupied");
                    else if ("WARNING".equalsIgnoreCase(n.getPriority())) b.getStyleClass().add("badge-reserved");
                    else b.getStyleClass().add("badge-available");
                    setGraphic(b);
                }
            }
        });
        colPriority.setMaxWidth(110);

        TableColumn<Notification, String> colTitle = new TableColumn<>("Subject");
        colTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));

        TableColumn<Notification, String> colMsg = new TableColumn<>("Message Content");
        colMsg.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMessage()));

        TableColumn<Notification, String> colTarget = new TableColumn<>("Target Audience");
        colTarget.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTargetRole()));
        colTarget.setMaxWidth(140);

        TableColumn<Notification, Void> colActions = new TableColumn<>("Delete");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button delBtn = new Button("Delete");
            {
                delBtn.getStyleClass().add("button-danger");
                delBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 10px;");
                delBtn.setOnAction(e -> {
                    Notification n = getTableRow().getItem();
                    if (n != null) {
                        notificationDAO.delete(n.getNotificationId());
                        loadNotifications();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : delBtn);
            }
        });
        colActions.setMaxWidth(90);

        table.getColumns().addAll(List.of(colId, colPriority, colTitle, colMsg, colTarget, colActions));
    }

    private void loadNotifications() {
        table.getItems().clear();
        table.getItems().addAll(notificationDAO.findAll());
    }

    private void showAnnouncementDialog() {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("modal-overlay");

        VBox dialog = new VBox(16);
        dialog.getStyleClass().add("modal-dialog");
        dialog.setMaxWidth(500);

        Label titleLbl = new Label("Broadcast New Announcement");
        titleLbl.getStyleClass().add("heading-1");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);

        TextField titleField = new TextField();
        titleField.setPromptText("e.g. Heavy Congestion Near SJT Gate");

        TextArea msgArea = new TextArea();
        msgArea.setPromptText("Enter full advisory message text...");
        msgArea.setPrefRowCount(4);

        ComboBox<String> priorityCombo = new ComboBox<>();
        priorityCombo.getItems().addAll("INFO", "WARNING", "URGENT");
        priorityCombo.setValue("INFO");

        ComboBox<String> targetCombo = new ComboBox<>();
        targetCombo.getItems().addAll("ALL", "CUSTOMER", "ADMIN");
        targetCombo.setValue("ALL");

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Priority:"), 0, 1);
        grid.add(priorityCombo, 1, 1);
        grid.add(new Label("Audience:"), 0, 2);
        grid.add(targetCombo, 1, 2);
        grid.add(new Label("Message:"), 0, 3);
        grid.add(msgArea, 1, 3);

        HBox btnBox = new HBox(12);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("button-secondary");
        cancelBtn.setOnAction(e -> ((StackPane) screenManager.getPrimaryStage().getScene().getRoot()).getChildren().remove(overlay));

        Button publishBtn = new Button("Publish Announcement 📢");
        publishBtn.getStyleClass().add("button-primary");
        publishBtn.setOnAction(e -> {
            Notification n = new Notification();
            n.setTitle(titleField.getText().trim());
            n.setMessage(msgArea.getText().trim());
            n.setPriority(priorityCombo.getValue());
            n.setTargetRole(targetCombo.getValue());

            notificationDAO.insert(n);

            ((StackPane) screenManager.getPrimaryStage().getScene().getRoot()).getChildren().remove(overlay);
            loadNotifications();
        });

        btnBox.getChildren().addAll(cancelBtn, publishBtn);
        dialog.getChildren().addAll(titleLbl, grid, btnBox);
        overlay.getChildren().add(dialog);

        ((StackPane) screenManager.getPrimaryStage().getScene().getRoot()).getChildren().add(overlay);
    }
}
