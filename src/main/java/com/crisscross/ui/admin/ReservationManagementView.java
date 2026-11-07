package com.crisscross.ui.admin;

import com.crisscross.dao.ReservationDAO;
import com.crisscross.models.Reservation;
import com.crisscross.services.BookingService;
import com.crisscross.ui.ScreenManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;
import java.util.stream.Collectors;

public class ReservationManagementView extends VBox {
    private final ScreenManager screenManager;
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final TableView<Reservation> table = new TableView<>();
    private final TextField searchField = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();

    public ReservationManagementView(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.setSpacing(20);
        this.setFillWidth(true);

        // Header
        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Reservation Management");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("Comprehensive audit log and check-in control for all customer event parking reservations");
        subtitle.getStyleClass().add("text-subtle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        searchField.setPromptText("🔍 Search user, code or plate...");
        searchField.setPrefWidth(220);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> loadReservations());

        statusFilter.getItems().addAll("ALL STATUSES", "CONFIRMED", "COMPLETED", "CANCELLED");
        statusFilter.setValue("ALL STATUSES");
        statusFilter.setOnAction(e -> loadReservations());

        topBox.getChildren().addAll(titleBox, spacer, searchField, statusFilter);

        setupTable();
        loadReservations();

        this.getChildren().addAll(topBox, table);
    }

    private void setupTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(500);

        TableColumn<Reservation, String> colCode = new TableColumn<>("Booking Code");
        colCode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBookingCode()));

        TableColumn<Reservation, String> colUser = new TableColumn<>("Customer Name");
        colUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUserName()));

        TableColumn<Reservation, String> colEvent = new TableColumn<>("Campus Event");
        colEvent.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEventTitle()));

        TableColumn<Reservation, String> colArea = new TableColumn<>("Parking Zone");
        colArea.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAreaName()));

        TableColumn<Reservation, String> colSlot = new TableColumn<>("Slot");
        colSlot.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSlotNumber()));

        TableColumn<Reservation, String> colPlate = new TableColumn<>("Vehicle Plate");
        colPlate.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVehicleType() + " (" + d.getValue().getVehicleNumber() + ")"));

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

        TableColumn<Reservation, Void> colActions = new TableColumn<>("Inspection & Actions");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button inspectBtn = new Button("Inspect");
            private final Button checkInBtn = new Button("Check-in");
            private final HBox box = new HBox(6, inspectBtn, checkInBtn);

            {
                inspectBtn.getStyleClass().add("button-secondary");
                inspectBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px;");
                checkInBtn.getStyleClass().add("button-primary");
                checkInBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px;");

                inspectBtn.setOnAction(e -> {
                    Reservation r = getTableRow().getItem();
                    if (r != null) showInspectionDialog(r);
                });

                checkInBtn.setOnAction(e -> {
                    Reservation r = getTableRow().getItem();
                    if (r != null) {
                        reservationDAO.updateStatus(r.getReservationId(), "COMPLETED");
                        loadReservations();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Reservation r = getTableRow().getItem();
                    checkInBtn.setDisable("COMPLETED".equalsIgnoreCase(r.getStatus()) || "CANCELLED".equalsIgnoreCase(r.getStatus()));
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(List.of(colCode, colUser, colEvent, colArea, colSlot, colPlate, colStatus, colActions));
    }

    private void loadReservations() {
        table.getItems().clear();
        String q = searchField.getText().toLowerCase().trim();
        String st = statusFilter.getValue();

        List<Reservation> all = reservationDAO.findAll();
        List<Reservation> filtered = all.stream().filter(r -> {
            boolean matchesQuery = q.isEmpty() ||
                    r.getBookingCode().toLowerCase().contains(q) ||
                    (r.getUserName() != null && r.getUserName().toLowerCase().contains(q)) ||
                    r.getVehicleNumber().toLowerCase().contains(q) ||
                    r.getSlotNumber().toLowerCase().contains(q);
            boolean matchesStatus = "ALL STATUSES".equals(st) || r.getStatus().equalsIgnoreCase(st);
            return matchesQuery && matchesStatus;
        }).collect(Collectors.toList());

        table.getItems().addAll(filtered);
    }

    private void showInspectionDialog(Reservation r) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("modal-overlay");

        VBox dialog = new VBox(16);
        dialog.getStyleClass().add("modal-dialog");
        dialog.setMaxWidth(520);

        Label title = new Label("Reservation Inspection: " + r.getBookingCode());
        title.getStyleClass().add("heading-1");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);

        grid.add(createRow("Customer ID & Name:", r.getUserName() + " (User #" + r.getUserId() + ")"), 0, 0);
        grid.add(createRow("Campus Event:", r.getEventTitle()), 0, 1);
        grid.add(createRow("Event Venue:", r.getEventVenue()), 0, 2);
        grid.add(createRow("Allocated Zone:", r.getAreaName()), 0, 3);
        grid.add(createRow("Assigned Bay:", r.getSlotNumber()), 0, 4);
        grid.add(createRow("Vehicle Info:", r.getVehicleType() + " (" + r.getVehicleNumber() + ")"), 0, 5);
        grid.add(createRow("Current Status:", r.getStatus()), 0, 6);
        grid.add(createRow("Booking Timestamp:", r.getBookingTime() != null ? r.getBookingTime().toString() : "N/A"), 0, 7);

        HBox btnBox = new HBox(12);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelResBtn = new Button("Cancel Booking");
        cancelResBtn.getStyleClass().add("button-danger");
        cancelResBtn.setDisable("CANCELLED".equalsIgnoreCase(r.getStatus()));
        cancelResBtn.setOnAction(e -> {
            try {
                BookingService.getInstance().cancelBooking(r.getReservationId());
                ((StackPane) screenManager.getPrimaryStage().getScene().getRoot()).getChildren().remove(overlay);
                loadReservations();
            } catch (Exception ex) {
                System.err.println(ex.getMessage());
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("button-secondary");
        closeBtn.setOnAction(e -> ((StackPane) screenManager.getPrimaryStage().getScene().getRoot()).getChildren().remove(overlay));

        btnBox.getChildren().addAll(cancelResBtn, closeBtn);
        dialog.getChildren().addAll(title, grid, btnBox);
        overlay.getChildren().add(dialog);

        ((StackPane) screenManager.getPrimaryStage().getScene().getRoot()).getChildren().add(overlay);
    }

    private VBox createRow(String label, String val) {
        VBox b = new VBox(2);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");
        Label v = new Label(val);
        v.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        b.getChildren().addAll(l, v);
        return b;
    }
}
