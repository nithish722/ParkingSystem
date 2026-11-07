package com.crisscross.ui.admin;

import com.crisscross.dao.ReservationDAO;
import com.crisscross.models.Reservation;
import com.crisscross.services.ReportService;
import com.crisscross.ui.ScreenManager;
import com.crisscross.util.PassReceiptGenerator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportsDashboardView extends VBox {
    private final ScreenManager screenManager;
    private final ReportService reportService = ReportService.getInstance();
    private final ReservationDAO reservationDAO = new ReservationDAO();

    private final HBox summaryRow = new HBox(16);
    private final HBox zoneCardsRow = new HBox(16);
    private final HBox bottomAnalyticsRow = new HBox(20);
    private final TableView<Reservation> liveAllocationTable = new TableView<>();
    private final Label liveSyncBadge = new Label();

    private Timeline autoRefreshTimeline;
    private ReportService.SystemStats latestStats;

    public ReportsDashboardView(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.setSpacing(22);
        this.setFillWidth(true);

        // 1. Header with Live Sync Indicator & PDF Export
        HBox topBox = new HBox(14);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Reports & Real-Time Slot Capacity Dashboard");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("Live parking slot capacity, filled vs. free bay counts, Car/Bike distributions, and customer payment ledger");
        subtitle.getStyleClass().add("text-subtle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        liveSyncBadge.setStyle("-fx-background-color: rgba(34, 197, 94, 0.16); -fx-text-fill: #4ade80; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 6px 12px; -fx-background-radius: 12px; -fx-border-color: rgba(74, 222, 128, 0.40); -fx-border-radius: 12px;");

        Button refreshBtn = new Button("↻ Sync Now");
        refreshBtn.getStyleClass().add("button-secondary");
        refreshBtn.setOnAction(e -> refreshDashboardData());

        Button exportBtn = new Button("Export Audit Report (PDF) 📄");
        exportBtn.getStyleClass().add("button-primary");
        exportBtn.setOnAction(e -> {
            try {
                if (latestStats == null) latestStats = reportService.getSystemStats();
                File saved = PassReceiptGenerator.exportAdminAuditReportPdf(getScene().getWindow(), latestStats);
                if (saved != null) {
                    screenManager.showConfirmationDialog(
                            "PDF Audit Report Exported! 📄",
                            "Your real-time capacity and audit report has been saved as a PDF to:\n\n" + saved.getAbsolutePath(),
                            () -> {}
                    );
                }
            } catch (Exception ex) {
                screenManager.showConfirmationDialog("Export Notice", "Could not export PDF report: " + ex.getMessage(), () -> {});
            }
        });

        topBox.getChildren().addAll(titleBox, spacer, liveSyncBadge, refreshBtn, exportBtn);

        // 2. Zone Occupancy Breakdown Section
        VBox zonesSection = new VBox(12);
        HBox zoneHeaderRow = new HBox(12);
        zoneHeaderRow.setAlignment(Pos.CENTER_LEFT);
        Label zoneTitle = new Label("Campus Parking Zones — Live Slot Capacity & How Much Filled");
        zoneTitle.getStyleClass().add("heading-2");
        Label zoneHint = new Label("Updates simultaneously as customers reserve or release parking slots");
        zoneHint.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        Region zSp = new Region();
        HBox.setHgrow(zSp, Priority.ALWAYS);
        zoneHeaderRow.getChildren().addAll(zoneTitle, zSp, zoneHint);
        zonesSection.getChildren().addAll(zoneHeaderRow, zoneCardsRow);

        // 3. Live Customer Slot Allocations & Payment Ledger Table
        VBox ledgerSection = new VBox(10);
        HBox ledgerHeader = new HBox(12);
        ledgerHeader.setAlignment(Pos.CENTER_LEFT);
        Label ledgerTitle = new Label("Live Customer Slot Allocations & Visitor Payment Ledger");
        ledgerTitle.getStyleClass().add("heading-2");
        Region lSp = new Region();
        HBox.setHgrow(lSp, Priority.ALWAYS);
        Button manageSlotsBtn = new Button("Manage Individual Slots →");
        manageSlotsBtn.getStyleClass().add("button-secondary");
        manageSlotsBtn.setOnAction(e -> screenManager.showAdminSlots());
        ledgerHeader.getChildren().addAll(ledgerTitle, lSp, manageSlotsBtn);

        setupLiveAllocationTable();
        ledgerSection.getChildren().addAll(ledgerHeader, liveAllocationTable);

        this.getChildren().addAll(topBox, summaryRow, zonesSection, bottomAnalyticsRow, ledgerSection);

        // Initial Data Load
        refreshDashboardData();

        // Start 2-second Live Auto-Refresh Timeline so it updates simultaneously with customer bookings
        startAutoRefreshTimeline();
    }

    private void startAutoRefreshTimeline() {
        autoRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            if (getScene() != null) {
                refreshDashboardData();
            } else {
                autoRefreshTimeline.stop();
            }
        }));
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();

        this.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null && autoRefreshTimeline != null) {
                autoRefreshTimeline.stop();
            } else if (newScene != null && autoRefreshTimeline != null) {
                autoRefreshTimeline.play();
            }
        });
    }

    public void refreshDashboardData() {
        ReportService.SystemStats s = reportService.getSystemStats();
        this.latestStats = s;

        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm:ss a"));
        liveSyncBadge.setText("● LIVE SYNC ACTIVE • " + timeStr);

        // 1. Update Top KPI Summary Row
        summaryRow.getChildren().clear();
        summaryRow.getChildren().addAll(
                createCard(
                        "Total Slot Capacity & Fill",
                        s.totalOccupied + " / " + s.totalCapacity + " Filled",
                        String.format("%.1f%% Filled  •  %d Empty Bays Available", s.overallOccupancyRate, s.totalAvailable),
                        "#F2A06A"
                ),
                createCard(
                        "Available Empty Slots",
                        s.totalAvailable + " Free Bays",
                        String.format("Car: %d Free  •  Bike: %d Free", s.freeCarSlots, s.freeBikeSlots),
                        "#4ade80"
                ),
                createCard(
                        "Slot Fill Status Breakdown",
                        s.totalReservedSlots + " Reserved  •  " + s.totalOccupiedSlots + " Occupied",
                        s.totalBlockedSlots + " Blocked / Maintenance  •  " + s.confirmedReservations + " Active Passes",
                        "#E8B86D"
                ),
                createCard(
                        "Visitor Payments Collected",
                        "₹ " + String.format("%.2f", s.totalRevenueCollected),
                        s.paidVisitorBookings + " Paid Visitor Passes  •  " + s.freeVitBookings + " Free VIT Passes",
                        "#34d399"
                )
        );

        // 2. Update Zone-by-Zone Slot Capacity Cards
        zoneCardsRow.getChildren().clear();
        for (ReportService.ZoneCapacityDetail z : s.zoneDetails) {
            zoneCardsRow.getChildren().add(createZoneOccupancyCard(z));
        }

        // 3. Update Bottom Vehicle & Capacity Analytics
        bottomAnalyticsRow.getChildren().clear();

        VBox vehCard = new VBox(14);
        vehCard.getStyleClass().add("glass-card");
        HBox.setHgrow(vehCard, Priority.ALWAYS);
        vehCard.setPadding(new Insets(22));

        Label vehTitle = new Label("Car vs. Bike Slot Capacity & Fill Distribution");
        vehTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #E8B86D;");

        double carFillFrac = s.totalCarSlots > 0 ? (double) s.filledCarSlots / s.totalCarSlots : 0.0;
        double bikeFillFrac = s.totalBikeSlots > 0 ? (double) s.filledBikeSlots / s.totalBikeSlots : 0.0;

        VBox carBar = createBarItem(
                "🚗 Four-Wheeler Slots (Car Bays)",
                s.filledCarSlots + " / " + s.totalCarSlots + " Filled (" + s.freeCarSlots + " Available)",
                String.format("%.1f%% Filled", carFillFrac * 100),
                carFillFrac,
                "#F2A06A"
        );
        VBox bikeBar = createBarItem(
                "🏍️ Two-Wheeler Slots (Bike Bays)",
                s.filledBikeSlots + " / " + s.totalBikeSlots + " Filled (" + s.freeBikeSlots + " Available)",
                String.format("%.1f%% Filled", bikeFillFrac * 100),
                bikeFillFrac,
                "#34d399"
        );

        vehCard.getChildren().addAll(vehTitle, carBar, bikeBar);

        VBox routeCard = new VBox(12);
        routeCard.getStyleClass().add("glass-card");
        HBox.setHgrow(routeCard, Priority.ALWAYS);
        routeCard.setPadding(new Insets(22));

        Label routeTitle = new Label("Real-Time Capacity & Payment Summary");
        routeTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #34d399;");

        Label r1 = new Label("• Total Campus Capacity: " + s.totalCapacity + " Slots (" + s.totalOccupied + " Filled / " + s.totalAvailable + " Available)");
        r1.setStyle("-fx-text-fill: #f8fafc; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label r2 = new Label("• Active Customer Reservations: " + s.confirmedReservations + " Confirmed • " + s.completedReservations + " Completed • " + s.cancelledReservations + " Cancelled");
        r2.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");
        Label r3 = new Label("• Non-VIT Visitor Payments: ₹ " + String.format("%.2f", s.totalRevenueCollected) + " across " + s.paidVisitorBookings + " paid reservations");
        r3.setStyle("-fx-text-fill: #F2A06A; -fx-font-size: 13px; -fx-font-weight: bold;");
        Label r4 = new Label("• VIT Community Waivers (vit.ac.in / vitstudent.ac.in): " + s.freeVitBookings + " complimentary passes issued");
        r4.setStyle("-fx-text-fill: #4ade80; -fx-font-size: 13px;");

        routeCard.getChildren().addAll(routeTitle, r1, r2, r3, r4);

        bottomAnalyticsRow.getChildren().addAll(vehCard, routeCard);

        // 4. Update Live Reservations & Payment Table
        List<Reservation> allRes = reservationDAO.findAll();
        liveAllocationTable.getItems().setAll(allRes);
    }

    private VBox createCard(String title, String val, String sub, String hex) {
        VBox c = new VBox(6);
        c.getStyleClass().add("glass-card");
        HBox.setHgrow(c, Priority.ALWAYS);

        Label t = new Label(title);
        t.getStyleClass().add("text-subtle");

        Label v = new Label(val);
        v.setStyle("-fx-font-size: 22px; -fx-font-weight: 800; -fx-text-fill: " + hex + ";");

        Label s = new Label(sub);
        s.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");
        s.setWrapText(true);

        c.getChildren().addAll(t, v, s);
        return c;
    }

    private VBox createZoneOccupancyCard(ReportService.ZoneCapacityDetail z) {
        VBox c = new VBox(12);
        c.getStyleClass().add("glass-card");
        HBox.setHgrow(c, Priority.ALWAYS);
        c.setPadding(new Insets(20));

        double frac = z.totalSlots > 0 ? (double) z.filledSlots / z.totalSlots : 0.0;

        HBox topRow = new HBox(8);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("🅿️ " + z.areaName);
        title.setStyle("-fx-font-weight: 800; -fx-font-size: 15px; -fx-text-fill: #ffffff;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        Label pctBadge = new Label(String.format("%.1f%% FILLED", frac * 100));
        String badgeColor = frac > 0.80 ? "#f87171" : "#F2A06A";
        pctBadge.setStyle("-fx-background-color: rgba(242, 160, 106, 0.18); -fx-text-fill: " + badgeColor + "; -fx-font-size: 10px; -fx-font-weight: 800; -fx-padding: 3px 10px; -fx-background-radius: 10px;");
        topRow.getChildren().addAll(title, sp, pctBadge);

        // Big Capacity & Fill Summary
        HBox bigCountRow = new HBox(10);
        bigCountRow.setAlignment(Pos.BASELINE_LEFT);
        Label filledBig = new Label(z.filledSlots + " / " + z.totalSlots + " Slots Filled");
        filledBig.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #E8B86D;");
        Label freeBig = new Label("(" + z.availableSlots + " Free)");
        freeBig.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #4ade80;");
        bigCountRow.getChildren().addAll(filledBig, freeBig);

        ProgressBar pb = new ProgressBar(frac);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(9);
        pb.setStyle("-fx-accent: " + (frac > 0.85 ? "#ef4444" : "#F16E68") + ";");

        // Breakdown of Reserved / Occupied / Blocked / Available
        HBox stateRow = new HBox(10);
        Label resLbl = new Label("🟡 " + z.reservedSlots + " Reserved");
        resLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #fbbf24; -fx-font-weight: bold;");
        Label occLbl = new Label("🔴 " + z.occupiedSlots + " Occupied");
        occLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #f87171; -fx-font-weight: bold;");
        Label blkLbl = new Label("⚪ " + z.blockedSlots + " Blocked");
        blkLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: bold;");
        stateRow.getChildren().addAll(resLbl, occLbl, blkLbl);

        // Car & Bike Bay Fill Sub-grid
        VBox typeBox = new VBox(4);
        typeBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.035); -fx-background-radius: 10px; -fx-padding: 8px 12px;");
        Label carStat = new Label("🚗 Car Bays:  " + z.carFilled + " / " + z.carTotal + " Filled  (" + z.carFree + " Available)");
        carStat.setStyle("-fx-font-size: 11px; -fx-text-fill: #e2e8f0; -fx-font-weight: 600;");
        Label bikeStat = new Label("🏍️ Bike Bays: " + z.bikeFilled + " / " + z.bikeTotal + " Filled  (" + z.bikeFree + " Available)");
        bikeStat.setStyle("-fx-font-size: 11px; -fx-text-fill: #e2e8f0; -fx-font-weight: 600;");
        typeBox.getChildren().addAll(carStat, bikeStat);

        // Currently Filled Slot Identifiers
        VBox filledListContainer = new VBox(4);
        Label filledHeader = new Label("Filled Slot IDs:");
        filledHeader.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");

        FlowPane pillsPane = new FlowPane(6, 6);
        if (z.filledSlotBadges.isEmpty()) {
            Label noneLbl = new Label("All " + z.totalSlots + " slots currently empty & available");
            noneLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #4ade80;");
            pillsPane.getChildren().add(noneLbl);
        } else {
            for (String badgeText : z.filledSlotBadges) {
                Label pill = new Label(badgeText);
                boolean isRes = badgeText.contains("RESERVED");
                boolean isOcc = badgeText.contains("OCCUPIED");
                String pillBg = isRes ? "rgba(245, 158, 11, 0.18)" : (isOcc ? "rgba(239, 68, 68, 0.18)" : "rgba(148, 163, 184, 0.18)");
                String pillFg = isRes ? "#fbbf24" : (isOcc ? "#f87171" : "#cbd5e1");
                pill.setStyle("-fx-background-color: " + pillBg + "; -fx-text-fill: " + pillFg + "; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2px 8px; -fx-background-radius: 8px;");
                pillsPane.getChildren().add(pill);
            }
        }
        filledListContainer.getChildren().addAll(filledHeader, pillsPane);

        c.getChildren().addAll(topRow, bigCountRow, pb, stateRow, typeBox, filledListContainer);
        return c;
    }

    private VBox createBarItem(String label, String detailText, String pct, double fraction, String hex) {
        VBox b = new VBox(6);
        HBox top = new HBox();
        Label l = new Label(label);
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #f8fafc; -fx-font-size: 13px;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label v = new Label(detailText + " • " + pct);
        v.setStyle("-fx-font-size: 12px; -fx-text-fill: " + hex + "; -fx-font-weight: bold;");
        top.getChildren().addAll(l, sp, v);

        ProgressBar pb = new ProgressBar(fraction);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(7);
        pb.setStyle("-fx-accent: " + hex + ";");

        b.getChildren().addAll(top, pb);
        return b;
    }

    private void setupLiveAllocationTable() {
        liveAllocationTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        liveAllocationTable.setPrefHeight(240);

        TableColumn<Reservation, String> colCode = new TableColumn<>("Booking Code");
        colCode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookingCode()));

        TableColumn<Reservation, String> colUser = new TableColumn<>("Customer");
        colUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUserName()));

        TableColumn<Reservation, String> colZone = new TableColumn<>("Parking Zone");
        colZone.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAreaName()));

        TableColumn<Reservation, String> colSlot = new TableColumn<>("Filled Slot");
        colSlot.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSlotNumber()));

        TableColumn<Reservation, String> colVeh = new TableColumn<>("Vehicle");
        colVeh.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVehicleType() + " (" + d.getValue().getVehicleNumber() + ")"));

        TableColumn<Reservation, String> colPayMode = new TableColumn<>("Payment Mode & Txn");
        colPayMode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getPaymentMethod() + " [" + d.getValue().getTransactionId() + "]"));

        TableColumn<Reservation, String> colAmount = new TableColumn<>("Amount Paid");
        colAmount.setCellValueFactory(d -> {
            double amt = d.getValue().getTotalAmount();
            return new SimpleStringProperty(amt > 0.0 ? ("₹ " + String.format("%.2f", amt) + " (PAID)") : "₹ 0.00 (VIT Free)");
        });

        TableColumn<Reservation, String> colStatus = new TableColumn<>("Pass Status");
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

        liveAllocationTable.getColumns().addAll(List.of(colCode, colUser, colZone, colSlot, colVeh, colPayMode, colAmount, colStatus));
    }
}
