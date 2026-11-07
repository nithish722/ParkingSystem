package com.crisscross.ui.admin;

import com.crisscross.dao.EventDAO;
import com.crisscross.models.Event;
import com.crisscross.ui.ScreenManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class EventManagementView extends VBox {
    private final ScreenManager screenManager;
    private final EventDAO eventDAO = new EventDAO();
    private final TableView<Event> table = new TableView<>();
    private final TextField searchField = new TextField();

    public EventManagementView(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.setSpacing(20);
        this.setFillWidth(true);

        // Header
        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Event Management");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("View, create and update campus events with designated venues");
        subtitle.getStyleClass().add("text-subtle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField.setPromptText("🔍 Filter events...");
        searchField.setPrefWidth(220);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadEvents());

        Button addBtn = new Button("+ Add New Event");
        addBtn.getStyleClass().add("button-primary");
        addBtn.setOnAction(e -> showEventDialog(null));

        topBox.getChildren().addAll(titleBox, spacer, searchField, addBtn);

        setupTable();
        loadEvents();

        this.getChildren().addAll(topBox, table);
    }

    private void setupTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(500);

        TableColumn<Event, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getEventId()));
        colId.setMaxWidth(60);

        TableColumn<Event, String> colTitle = new TableColumn<>("Event Title");
        colTitle.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTitle()));

        TableColumn<Event, String> colVenue = new TableColumn<>("Venue");
        colVenue.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVenue()));

        TableColumn<Event, String> colDate = new TableColumn<>("Date & Time");
        colDate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEventDate() + " " + d.getValue().getEventTime()));

        TableColumn<Event, String> colOrg = new TableColumn<>("Organizer");
        colOrg.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getOrganizer()));

        TableColumn<Event, String> colCat = new TableColumn<>("Category");
        colCat.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCategory()));

        TableColumn<Event, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Event ev = getTableRow().getItem();
                    Label b = new Label(ev.getStatus());
                    if ("UPCOMING".equalsIgnoreCase(ev.getStatus())) b.getStyleClass().add("badge-available");
                    else if ("ONGOING".equalsIgnoreCase(ev.getStatus())) b.getStyleClass().add("badge-reserved");
                    else b.getStyleClass().add("badge-occupied");
                    setGraphic(b);
                }
            }
        });

        TableColumn<Event, Void> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button delBtn = new Button("Delete");
            private final HBox box = new HBox(6, editBtn, delBtn);

            {
                editBtn.getStyleClass().add("button-secondary");
                editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px;");
                delBtn.getStyleClass().add("button-danger");
                delBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px;");

                editBtn.setOnAction(e -> {
                    Event ev = getTableRow().getItem();
                    if (ev != null) showEventDialog(ev);
                });

                delBtn.setOnAction(e -> {
                    Event ev = getTableRow().getItem();
                    if (ev != null) {
                        screenManager.showConfirmationDialog("Delete Event", "Delete event '" + ev.getTitle() + "'?", () -> {
                            eventDAO.delete(ev.getEventId());
                            loadEvents();
                        });
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(List.of(colId, colTitle, colVenue, colDate, colOrg, colCat, colStatus, colActions));
    }

    private void loadEvents() {
        table.getItems().clear();
        String query = searchField.getText().toLowerCase().trim();
        List<Event> all = eventDAO.findAll();
        for (Event e : all) {
            if (query.isEmpty() || e.getTitle().toLowerCase().contains(query) || e.getVenue().toLowerCase().contains(query)) {
                table.getItems().add(e);
            }
        }
    }

    private void showEventDialog(Event existing) {
        boolean isEdit = (existing != null);

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("modal-overlay");

        VBox dialog = new VBox(16);
        dialog.getStyleClass().add("modal-dialog");
        dialog.setMaxWidth(560);

        Label titleLbl = new Label(isEdit ? "Edit Campus Event" : "Create New Campus Event");
        titleLbl.getStyleClass().add("heading-1");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);

        TextField titleField = new TextField(isEdit ? existing.getTitle() : "");
        titleField.setPromptText("e.g. AI Innovation Conclave");

        ComboBox<String> venueCombo = new ComboBox<>();
        venueCombo.getItems().addAll("SJT Auditorium", "SJT Lab Complex", "CDMM Hall", "CDMM Architecture Block", "Woody's Grounds", "Woody's Sports Arena");
        venueCombo.setValue(isEdit ? existing.getVenue() : "SJT Auditorium");

        TextField dateField = new TextField(isEdit ? existing.getEventDate() : "2026-11-15");
        TextField timeField = new TextField(isEdit ? existing.getEventTime() : "10:00 AM");
        TextField orgField = new TextField(isEdit ? existing.getOrganizer() : "Department of Computer Science");
        TextField attendeesField = new TextField(isEdit ? String.valueOf(existing.getExpectedAttendees()) : "500");

        ComboBox<String> catCombo = new ComboBox<>();
        catCombo.getItems().addAll("TECH", "CULTURAL", "SPORTS", "ACADEMIC");
        catCombo.setValue(isEdit ? existing.getCategory() : "TECH");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("UPCOMING", "ONGOING", "COMPLETED", "CANCELLED");
        statusCombo.setValue(isEdit ? existing.getStatus() : "UPCOMING");

        TextArea descArea = new TextArea(isEdit ? existing.getDescription() : "Keynote addresses and paper presentations.");
        descArea.setPrefRowCount(3);

        grid.add(new Label("Event Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Venue:"), 0, 1);
        grid.add(venueCombo, 1, 1);
        grid.add(new Label("Date (YYYY-MM-DD):"), 0, 2);
        grid.add(dateField, 1, 2);
        grid.add(new Label("Time:"), 0, 3);
        grid.add(timeField, 1, 3);
        grid.add(new Label("Organizer:"), 0, 4);
        grid.add(orgField, 1, 4);
        grid.add(new Label("Category:"), 0, 5);
        grid.add(catCombo, 1, 5);
        grid.add(new Label("Status:"), 0, 6);
        grid.add(statusCombo, 1, 6);
        grid.add(new Label("Attendees:"), 0, 7);
        grid.add(attendeesField, 1, 7);
        grid.add(new Label("Description:"), 0, 8);
        grid.add(descArea, 1, 8);

        HBox btnBox = new HBox(12);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("button-secondary");
        cancelBtn.setOnAction(e -> ((StackPane) screenManager.getPrimaryStage().getScene().getRoot()).getChildren().remove(overlay));

        Button saveBtn = new Button(isEdit ? "Update Event" : "Create Event");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setOnAction(e -> {
            Event ev = isEdit ? existing : new Event();
            ev.setTitle(titleField.getText().trim());
            ev.setVenue(venueCombo.getValue());
            ev.setEventDate(dateField.getText().trim());
            ev.setEventTime(timeField.getText().trim());
            ev.setOrganizer(orgField.getText().trim());
            ev.setCategory(catCombo.getValue());
            ev.setStatus(statusCombo.getValue());
            try {
                ev.setExpectedAttendees(Integer.parseInt(attendeesField.getText().trim()));
            } catch (Exception ignored) {
                ev.setExpectedAttendees(400);
            }
            ev.setDescription(descArea.getText().trim());

            if (isEdit) {
                eventDAO.update(ev);
            } else {
                eventDAO.insert(ev);
            }

            ((StackPane) screenManager.getPrimaryStage().getScene().getRoot()).getChildren().remove(overlay);
            loadEvents();
        });

        btnBox.getChildren().addAll(cancelBtn, saveBtn);
        dialog.getChildren().addAll(titleLbl, grid, btnBox);
        overlay.getChildren().add(dialog);

        ((StackPane) screenManager.getPrimaryStage().getScene().getRoot()).getChildren().add(overlay);
    }
}
