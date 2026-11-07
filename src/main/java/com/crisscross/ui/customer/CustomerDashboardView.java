package com.crisscross.ui.customer;

import com.crisscross.dao.EventDAO;
import com.crisscross.dao.ParkingAreaDAO;
import com.crisscross.dao.ReservationDAO;
import com.crisscross.models.*;
import com.crisscross.services.AuthService;
import com.crisscross.ui.ScreenManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;

import java.util.List;

public class CustomerDashboardView extends VBox {
    private final ScreenManager screenManager;
    private final ParkingAreaDAO areaDAO = new ParkingAreaDAO();
    private final EventDAO eventDAO = new EventDAO();
    private final ReservationDAO resDAO = new ReservationDAO();

    public CustomerDashboardView(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.setSpacing(24);
        this.setFillWidth(true);

        User currentUser = AuthService.getInstance().getCurrentUser();
        String firstName = currentUser != null ? currentUser.getFullName().split(" ")[0] : "Student";

        // Welcome Hero Banner
        VBox heroBanner = new VBox(8);
        heroBanner.getStyleClass().add("glass-card-highlight");
        heroBanner.setPadding(new Insets(24, 28, 24, 28));

        Label greetLbl = new Label("Welcome back, " + firstName + " 👋");
        greetLbl.setStyle("-fx-font-size: 26px; -fx-font-weight: 800; -fx-text-fill: #F2A06A;");

        Label subLbl = new Label("Plan your campus event parking seamlessly. Choose an event to get smart proximity recommendations for SJT, CDMM, and Woody's.");
        subLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #CBD5E1;");

        HBox quickActions = new HBox(12);
        quickActions.setStyle("-fx-padding: 8px 0 0 0;");

        Button bookNowBtn = new Button("⚡ Book Event Parking");
        bookNowBtn.getStyleClass().add("button-primary");
        bookNowBtn.setOnAction(e -> screenManager.showEventList());

        Button mapBtn = new Button("🧭 Interactive Campus Map");
        mapBtn.getStyleClass().add("button-secondary");
        mapBtn.setOnAction(e -> screenManager.showParkingMapView());

        Button myPassesBtn = new Button("🎟️ My Passes");
        myPassesBtn.getStyleClass().add("button-secondary");
        myPassesBtn.setOnAction(e -> screenManager.showMyReservations());

        quickActions.getChildren().addAll(bookNowBtn, mapBtn, myPassesBtn);
        heroBanner.getChildren().addAll(greetLbl, subLbl, quickActions);

        // Stats Row
        List<Reservation> userRes = (currentUser != null) ? resDAO.findByUserId(currentUser.getUserId()) : List.of();
        long activeCount = userRes.stream().filter(r -> "CONFIRMED".equalsIgnoreCase(r.getStatus()) || "ACTIVE".equalsIgnoreCase(r.getStatus())).count();
        List<Event> upcomingEvents = eventDAO.findUpcoming();
        List<ParkingArea> areas = areaDAO.findAll();
        int totalAvailableSlots = areas.stream().mapToInt(ParkingArea::getAvailableCapacity).sum();

        HBox statsRow = new HBox(16);
        statsRow.getChildren().addAll(
                createStatCard("🎟️", "Active Reservations", String.valueOf(activeCount), "Ready for check-in"),
                createStatCard("📅", "Upcoming Events", String.valueOf(upcomingEvents.size()), "Campus venues"),
                createStatCard("🅿️", "Available Slots", String.valueOf(totalAvailableSlots), "Across SJT, CDMM & Woody's")
        );

        // Campus Parking Status Section
        VBox parkingSection = new VBox(14);
        Label parkingSecTitle = new Label("Live Campus Parking Availability");
        parkingSecTitle.getStyleClass().add("heading-2");

        HBox parkingCardsBox = new HBox(16);
        for (ParkingArea area : areas) {
            parkingCardsBox.getChildren().add(createAreaCard(area));
        }
        parkingSection.getChildren().addAll(parkingSecTitle, parkingCardsBox);

        // Featured Upcoming Events
        VBox eventsSection = new VBox(14);
        Label eventsSecTitle = new Label("Featured Campus Events");
        eventsSecTitle.getStyleClass().add("heading-2");

        VBox eventCardsList = new VBox(12);
        for (Event event : upcomingEvents) {
            eventCardsList.getChildren().add(createEventStrip(event));
        }
        eventsSection.getChildren().addAll(eventsSecTitle, eventCardsList);

        this.getChildren().addAll(heroBanner, statsRow, parkingSection, eventsSection);
    }

    private VBox createStatCard(String icon, String title, String value, String sub) {
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

        Label valueLbl = new Label(value);
        valueLbl.setStyle("-fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: #F2A06A;");

        Label subLbl = new Label(sub);
        subLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        card.getChildren().addAll(top, valueLbl, subLbl);
        return card;
    }

    private VBox createAreaCard(ParkingArea area) {
        VBox card = new VBox(10);
        card.getStyleClass().add("glass-card-interactive");
        HBox.setHgrow(card, Priority.ALWAYS);

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label nameLbl = new Label(area.getName());
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #fdf8f5;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(area.getStatus());
        statusBadge.getStyleClass().add("badge-available");
        header.getChildren().addAll(nameLbl, spacer, statusBadge);

        Label locLbl = new Label("📍 Near: " + area.getNearestVenue());
        locLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #CBD5E1;");

        double occupancyFraction = area.getTotalCapacity() > 0 ? (double) area.getOccupiedCount() / area.getTotalCapacity() : 0.0;
        ProgressBar pb = new ProgressBar(occupancyFraction);
        pb.setMaxWidth(Double.MAX_VALUE);
        pb.setPrefHeight(6);
        pb.setStyle("-fx-accent: " + (occupancyFraction > 0.85 ? "#ef4444" : "#F16E68") + ";");

        HBox capBox = new HBox();
        Label freeLbl = new Label(area.getAvailableCapacity() + " free");
        freeLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #4ade80; -fx-font-size: 12px;");
        Region capSpacer = new Region();
        HBox.setHgrow(capSpacer, Priority.ALWAYS);
        Label totalLbl = new Label(area.getTotalCapacity() + " total slots");
        totalLbl.setStyle("-fx-text-fill: #94A3B8; -fx-font-size: 12px;");
        capBox.getChildren().addAll(freeLbl, capSpacer, totalLbl);

        Button viewSlotsBtn = new Button("View Area Details →");
        viewSlotsBtn.getStyleClass().add("button-secondary");
        viewSlotsBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6px 12px;");
        viewSlotsBtn.setOnAction(e -> screenManager.showParkingAreaDetails(area));

        card.getChildren().addAll(header, locLbl, pb, capBox, viewSlotsBtn);
        return card;
    }

    private HBox createEventStrip(Event event) {
        HBox strip = new HBox(18);
        strip.getStyleClass().add("glass-card-interactive");
        strip.setAlignment(Pos.CENTER_LEFT);
        strip.setPadding(new Insets(14, 20, 14, 20));

        // Date pill
        VBox datePill = new VBox(2);
        datePill.setAlignment(Pos.CENTER);
        datePill.setStyle("-fx-background-color: rgba(242, 160, 106, 0.16); -fx-background-radius: 12px; -fx-padding: 8px 14px; -fx-border-color: rgba(242, 160, 106, 0.40); -fx-border-radius: 12px;");
        Label dateLbl = new Label(event.getEventDate());
        dateLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #F2A06A;");
        Label timeLbl = new Label(event.getEventTime());
        timeLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #cbd5e1;");
        datePill.getChildren().addAll(dateLbl, timeLbl);

        // Details
        VBox details = new VBox(3);
        Label titleLbl = new Label(event.getTitle());
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #f8fafc;");
        Label venueLbl = new Label("📍 Venue: " + event.getVenue() + "  •  " + event.getCategory() + "  •  Expected Footfall: " + event.getExpectedAttendees());
        venueLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        details.getChildren().addAll(titleLbl, venueLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button bookBtn = new Button("Book Parking");
        bookBtn.getStyleClass().add("button-primary");
        bookBtn.setStyle("-fx-font-size: 12px; -fx-padding: 8px 16px;");
        bookBtn.setOnAction(e -> screenManager.showVehicleSelection(event));

        strip.getChildren().addAll(datePill, details, spacer, bookBtn);
        return strip;
    }
}
