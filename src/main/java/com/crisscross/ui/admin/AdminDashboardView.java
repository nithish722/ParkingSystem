package com.crisscross.ui.admin;

import com.crisscross.dao.ReservationDAO;
import com.crisscross.models.Reservation;
import com.crisscross.services.ReportService;
import com.crisscross.ui.ScreenManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class AdminDashboardView extends VBox {
    private final ScreenManager screenManager;
    private final ReportService reportService = ReportService.getInstance();
    private final ReservationDAO reservationDAO = new ReservationDAO();

    public AdminDashboardView(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.setSpacing(24);
        this.setFillWidth(true);

        ReportService.SystemStats stats = reportService.getSystemStats();

        // Admin Header
        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Administrator Command Center");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("Live campus overview of parking zones, upcoming events, and customer reservations");
        subtitle.getStyleClass().add("text-subtle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button newEventBtn = new Button("+ Add Event");
        newEventBtn.getStyleClass().add("button-primary");
        newEventBtn.setOnAction(e -> screenManager.showAdminEvents());

        Button broadcastBtn = new Button("📢 Announcement");
        broadcastBtn.getStyleClass().add("button-secondary");
        broadcastBtn.setOnAction(e -> screenManager.showAdminNotifications());

        topBox.getChildren().addAll(titleBox, spacer, newEventBtn, broadcastBtn);

        // 4 KPI Stat Cards
        HBox kpiRow = new HBox(16);
        kpiRow.getChildren().addAll(
                createKpiCard("📅", "Active Events", stats.activeEvents + "/" + stats.totalEvents, "Campus schedule",
                        "#34d399"),
                createKpiCard("📋", "Total Bookings", String.valueOf(stats.totalReservations),
                        stats.confirmedReservations + " Confirmed", "#4ade80"),
                createKpiCard("🅿️", "Occupancy Rate", String.format("%.1f%%", stats.overallOccupancyRate),
                        stats.totalOccupied + "/" + stats.totalCapacity + " Occupied", "#6ee7b7"),
                createKpiCard("👥", "Total Users", String.valueOf(stats.totalUsers),
                        stats.totalCustomers + " Customers", "#fbbf24"));

        // Live Occupancy by Area Row
        VBox occupancyBox = new VBox(12);
        Label occTitle = new Label("Campus Zone Capacity & Proximity Status");
        occTitle.getStyleClass().add("heading-2");

        HBox zoneRow = new HBox(16);
        for (var entry : stats.areaOccupancyMap.entrySet()) {
            String name = entry.getKey();
            int occupied = entry.getValue();
            int total = stats.areaTotalMap.getOrDefault(name, 30);
            int free = Math.max(0, total - occupied);
            zoneRow.getChildren().add(createZoneCard(name, occupied, free, total));
        }
        occupancyBox.getChildren().addAll(occTitle, zoneRow);

        // Recent Reservations Table
        VBox tableBox = new VBox(12);
        HBox tableHeader = new HBox(12);
        tableHeader.setAlignment(Pos.CENTER_LEFT);
        Label recTitle = new Label("Recent Customer Event Bookings");
        recTitle.getStyleClass().add("heading-2");
        Region tableSpacer = new Region();
        HBox.setHgrow(tableSpacer, Priority.ALWAYS);
        Button viewAllBtn = new Button("View All Reservations →");
        viewAllBtn.getStyleClass().add("button-secondary");
        viewAllBtn.setOnAction(e -> screenManager.showAdminReservations());
        tableHeader.getChildren().addAll(recTitle, tableSpacer, viewAllBtn);

        TableView<Reservation> recentTable = createRecentTable();
        tableBox.getChildren().addAll(tableHeader, recentTable);

        this.getChildren().addAll(topBox, kpiRow, occupancyBox, tableBox);
    }

    private VBox createKpiCard(String icon, String title, String val, String sub, String accentColor) {
        VBox card = new VBox(8);
        card.getStyleClass().add("glass-card");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 20px;");
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("text-subtle");
        titleLbl.setStyle("-fx-font-weight: 600;");
        top.getChildren().addAll(iconLbl, titleLbl);

        Label valLbl = new Label(val);
        valLbl.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: " + accentColor + ";");

        Label subLbl = new Label(sub);
        subLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

        card.getChildren().addAll(top, valLbl, subLbl);
        return card;
    }

    private VBox createZoneCard(String name, int occupied, int free, int total) {
        VBox card = new VBox(10);
        card.getStyleClass().add("glass-card-interactive");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = new Label("🅿️ " + name);
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #ffffff;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label freeBadge = new Label(free + " free");
        freeBadge.setStyle("-fx-background-color: rgba(34, 197, 94, 0.2); -fx-text-fill: #4ade80; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 3px 8px; -fx-background-radius: 8px;");
        top.getChildren().addAll(nameLbl, spacer, freeBadge);

        double frac = total > 0 ? (double) occupied / total : 0.0;
        ProgressBar pb = new ProgressBar(frac);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(6);
        pb.setStyle("-fx-accent: " + (frac > 0.85 ? "#ef4444" : "#34d399") + ";");

        Label stat = new Label(occupied + " occupied / " + total + " total bays (" + String.format("%.0f%%", frac * 100) + ")");
        stat.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");

        card.getChildren().addAll(top, pb, stat);
        card.setOnMouseClicked(e -> screenManager.showAdminSlots());
        return card;
    }

    private TableView<Reservation> createRecentTable() {
        TableView<Reservation> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(220);

        TableColumn<Reservation, String> colCode = new TableColumn<>("Code");
        colCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBookingCode()));

        TableColumn<Reservation, String> colUser = new TableColumn<>("User");
        colUser.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUserName()));

        TableColumn<Reservation, String> colEvent = new TableColumn<>("Event");
        colEvent.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEventTitle()));

        TableColumn<Reservation, String> colArea = new TableColumn<>("Area");
        colArea.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAreaName()));

        TableColumn<Reservation, String> colSlot = new TableColumn<>("Slot");
        colSlot.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSlotNumber()));

        TableColumn<Reservation, String> colVehicle = new TableColumn<>("Vehicle");
        colVehicle.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getVehicleType() + " (" + data.getValue().getVehicleNumber() + ")"));

        TableColumn<Reservation, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Reservation r = getTableRow().getItem();
                    Label b = new Label(r.getStatus());
                    if ("CONFIRMED".equalsIgnoreCase(r.getStatus())) b.getStyleClass().add("badge-available");
                    else if ("COMPLETED".equalsIgnoreCase(r.getStatus())) b.getStyleClass().add("badge-reserved");
                    else b.getStyleClass().add("badge-occupied");
                    setGraphic(b);
                }
            }
        });

        table.getColumns().addAll(List.of(colCode, colUser, colEvent, colArea, colSlot, colVehicle, colStatus));

        List<Reservation> all = reservationDAO.findAll();
        table.getItems().addAll(all.subList(0, Math.min(5, all.size())));
        return table;
    }
}
