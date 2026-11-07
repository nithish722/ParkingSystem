package com.crisscross.ui.customer;

import com.crisscross.dao.EventDAO;
import com.crisscross.models.Event;
import com.crisscross.ui.ScreenManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.util.List;
import java.util.stream.Collectors;

public class EventListView extends VBox {
    private final ScreenManager screenManager;
    private final EventDAO eventDAO = new EventDAO();
    private final VBox eventsContainer = new VBox(14);
    private List<Event> allEvents;

    public EventListView(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.setSpacing(20);
        this.setFillWidth(true);

        // Header Title & Search Controls
        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Campus Events Catalog");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("Browse upcoming university symposiums, fests and sports meets to book parking");
        subtitle.getStyleClass().add("text-subtle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TextField searchField = new TextField();
        searchField.setPromptText("🔍 Search event or venue...");
        searchField.setPrefWidth(240);

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("ALL CATEGORIES", "TECH", "CULTURAL", "SPORTS");
        categoryCombo.setValue("ALL CATEGORIES");

        topBox.getChildren().addAll(titleBox, spacer, searchField, categoryCombo);

        // Load Events
        allEvents = eventDAO.findAll();

        Runnable filterAction = () -> {
            String query = searchField.getText().toLowerCase().trim();
            String cat = categoryCombo.getValue();

            List<Event> filtered = allEvents.stream().filter(e -> {
                boolean matchesQuery = query.isEmpty() ||
                        e.getTitle().toLowerCase().contains(query) ||
                        e.getVenue().toLowerCase().contains(query) ||
                        e.getOrganizer().toLowerCase().contains(query);
                boolean matchesCat = "ALL CATEGORIES".equals(cat) || e.getCategory().equalsIgnoreCase(cat);
                return matchesQuery && matchesCat;
            }).collect(Collectors.toList());

            renderEvents(filtered);
        };

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterAction.run());
        categoryCombo.setOnAction(e -> filterAction.run());

        this.getChildren().addAll(topBox, eventsContainer);
        renderEvents(allEvents);
    }

    private void renderEvents(List<Event> events) {
        eventsContainer.getChildren().clear();

        if (events.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.getStyleClass().add("glass-card");
            emptyBox.setPadding(new Insets(40));
            Label noEventLbl = new Label("No matching events found.");
            noEventLbl.setStyle("-fx-font-size: 16px; -fx-text-fill: #94a3b8;");
            emptyBox.getChildren().add(noEventLbl);
            eventsContainer.getChildren().add(emptyBox);
            return;
        }

        for (Event event : events) {
            eventsContainer.getChildren().add(createEventCard(event));
        }
    }

    private VBox createEventCard(Event event) {
        VBox card = new VBox(12);
        card.getStyleClass().add("glass-card-interactive");
        card.setPadding(new Insets(18, 24, 18, 24));

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label catBadge = new Label(event.getCategory());
        catBadge.setStyle("-fx-background-color: rgba(242, 160, 106, 0.16); -fx-text-fill: #F2A06A; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4px 10px; -fx-background-radius: 10px;");

        Label statusBadge = new Label(event.getStatus());
        statusBadge.getStyleClass().add("badge-available");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateLbl = new Label("📅 " + event.getEventDate() + "  ⏰ " + event.getEventTime());
        dateLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #CBD5E1; -fx-font-weight: 600;");

        topRow.getChildren().addAll(catBadge, statusBadge, spacer, dateLbl);

        Label titleLbl = new Label(event.getTitle());
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

        Label descLbl = new Label(event.getDescription());
        descLbl.setWrapText(true);
        descLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #cbd5e1;");

        HBox bottomRow = new HBox(16);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Label venueLbl = new Label("📍 Venue: " + event.getVenue());
        venueLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #6ee7b7; -fx-font-size: 13px;");

        Label orgLbl = new Label("🏛️ Organized by: " + event.getOrganizer());
        orgLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        Button detailsBtn = new Button("View Details");
        detailsBtn.getStyleClass().add("button-secondary");
        detailsBtn.setOnAction(e -> screenManager.showEventDetails(event));

        Button bookBtn = new Button("Select Parking →");
        bookBtn.getStyleClass().add("button-primary");
        bookBtn.setOnAction(e -> screenManager.showVehicleSelection(event));

        bottomRow.getChildren().addAll(venueLbl, orgLbl, bottomSpacer, detailsBtn, bookBtn);

        card.getChildren().addAll(topRow, titleLbl, descLbl, bottomRow);
        return card;
    }
}
