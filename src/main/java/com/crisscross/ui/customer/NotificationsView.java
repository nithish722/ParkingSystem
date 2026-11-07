package com.crisscross.ui.customer;

import com.crisscross.dao.NotificationDAO;
import com.crisscross.models.Notification;
import com.crisscross.ui.ScreenManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.util.List;

public class NotificationsView extends VBox {
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final VBox listContainer = new VBox(14);

    public NotificationsView(ScreenManager screenManager) {
        this.setSpacing(20);
        this.setFillWidth(true);

        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Campus Announcements & Alerts");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("Real-time updates regarding event traffic, gate operations, and slot availability");
        subtitle.getStyleClass().add("text-subtle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = new Button("🔄 Refresh Alerts");
        refreshBtn.getStyleClass().add("button-secondary");
        refreshBtn.setOnAction(e -> loadNotifications());

        topBox.getChildren().addAll(titleBox, spacer, refreshBtn);

        this.getChildren().addAll(topBox, listContainer);
        loadNotifications();
    }

    private void loadNotifications() {
        listContainer.getChildren().clear();
        List<Notification> list = notificationDAO.findForRole("CUSTOMER");

        if (list.isEmpty()) {
            VBox empty = new VBox(12);
            empty.getStyleClass().add("glass-card");
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(30));
            Label l = new Label("No active announcements at this time.");
            l.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px;");
            empty.getChildren().add(l);
            listContainer.getChildren().add(empty);
            return;
        }

        for (Notification n : list) {
            VBox card = new VBox(8);
            card.getStyleClass().add("glass-card");
            card.setPadding(new Insets(16, 22, 16, 22));

            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);

            Label badge = new Label(n.getPriority());
            if ("URGENT".equalsIgnoreCase(n.getPriority())) {
                badge.getStyleClass().add("badge-occupied");
            } else if ("WARNING".equalsIgnoreCase(n.getPriority())) {
                badge.getStyleClass().add("badge-reserved");
            } else {
                badge.getStyleClass().add("badge-available");
            }

            Label titleLbl = new Label(n.getTitle());
            titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #ffffff;");

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label timeLbl = new Label(n.getCreatedAt() != null ? n.getCreatedAt().toString().replace("T", " ").substring(0, 16) : "Today");
            timeLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

            row.getChildren().addAll(badge, titleLbl, sp, timeLbl);

            Label msgLbl = new Label(n.getMessage());
            msgLbl.setWrapText(true);
            msgLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1;");

            card.getChildren().addAll(row, msgLbl);
            listContainer.getChildren().add(card);
        }
    }
}
