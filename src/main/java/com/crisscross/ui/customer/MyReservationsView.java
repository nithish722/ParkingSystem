package com.crisscross.ui.customer;

import com.crisscross.dao.ReservationDAO;
import com.crisscross.models.Reservation;
import com.crisscross.models.User;
import com.crisscross.services.AuthService;
import com.crisscross.services.BookingService;
import com.crisscross.ui.ScreenManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class MyReservationsView extends VBox {
    private final ScreenManager screenManager;
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final TableView<Reservation> table = new TableView<>();

    public MyReservationsView(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.setSpacing(20);
        this.setFillWidth(true);

        // Header
        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("My Parking Passes & Reservations");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("Manage your active event parking permits, view QR gate passes, or cancel bookings");
        subtitle.getStyleClass().add("text-subtle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button bookNewBtn = new Button("+ Book New Event Parking");
        bookNewBtn.getStyleClass().add("button-primary");
        bookNewBtn.setOnAction(e -> screenManager.showEventList());

        topBox.getChildren().addAll(titleBox, spacer, bookNewBtn);

        // Table Setup
        setupTable();
        loadReservations();

        this.getChildren().addAll(topBox, table);
    }

    private void setupTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(480);

        TableColumn<Reservation, String> colCode = new TableColumn<>("Booking Code");
        colCode.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBookingCode()));

        TableColumn<Reservation, String> colEvent = new TableColumn<>("Campus Event");
        colEvent.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEventTitle()));

        TableColumn<Reservation, String> colVenue = new TableColumn<>("Venue");
        colVenue.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEventVenue()));

        TableColumn<Reservation, String> colArea = new TableColumn<>("Parking Zone");
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
                    setText(null);
                } else {
                    Reservation r = getTableRow().getItem();
                    Label badge = new Label(r.getStatus());
                    if ("CONFIRMED".equalsIgnoreCase(r.getStatus()) || "ACTIVE".equalsIgnoreCase(r.getStatus())) {
                        badge.getStyleClass().add("badge-available");
                    } else if ("COMPLETED".equalsIgnoreCase(r.getStatus())) {
                        badge.getStyleClass().add("badge-reserved");
                    } else {
                        badge.getStyleClass().add("badge-occupied");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        TableColumn<Reservation, Void> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View Pass");
            private final Button downloadBtn = new Button("Download 📥");
            private final Button cancelBtn = new Button("Cancel");
            private final HBox btnBox = new HBox(6, viewBtn, downloadBtn, cancelBtn);

            {
                viewBtn.getStyleClass().add("button-secondary");
                viewBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px;");

                downloadBtn.getStyleClass().add("button-secondary");
                downloadBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px; -fx-text-fill: #E8B86D; -fx-border-color: rgba(232, 184, 109, 0.4);");

                cancelBtn.getStyleClass().add("button-danger");
                cancelBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 8px;");

                viewBtn.setOnAction(e -> {
                    Reservation r = getTableRow().getItem();
                    if (r != null) screenManager.showReservationDetails(r);
                });

                downloadBtn.setOnAction(e -> {
                    Reservation r = getTableRow().getItem();
                    if (r != null) {
                        try {
                            User curUser = AuthService.getInstance().getCurrentUser();
                            java.io.File saved = com.crisscross.util.PassReceiptGenerator.downloadAndOpenReceipt(getScene().getWindow(), r, curUser);
                            if (saved != null) {
                                screenManager.showConfirmationDialog(
                                        "Receipt Downloaded Successfully! 📥",
                                        "Your digital pass receipt has been saved to:\n\n"
                                                + saved.getAbsolutePath()
                                                + "\n\nFile Explorer has opened with your receipt selected for printing.",
                                        () -> {}
                                );
                            }
                        } catch (Exception ex) {
                            screenManager.showConfirmationDialog("Download Notice", "Could not complete download: " + ex.getMessage(), () -> {});
                        }
                    }
                });

                cancelBtn.setOnAction(e -> {
                    Reservation r = getTableRow().getItem();
                    if (r != null) {
                        screenManager.showConfirmationDialog("Cancel Reservation",
                                "Are you sure you want to cancel booking " + r.getBookingCode() + " for slot " + r.getSlotNumber() + "?",
                                () -> {
                                    try {
                                        BookingService.getInstance().cancelBooking(r.getReservationId());
                                        loadReservations();
                                    } catch (Exception ex) {
                                        System.err.println(ex.getMessage());
                                    }
                                });
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
                    cancelBtn.setDisable("CANCELLED".equalsIgnoreCase(r.getStatus()) || "COMPLETED".equalsIgnoreCase(r.getStatus()));
                    setGraphic(btnBox);
                }
            }
        });

        table.getColumns().addAll(List.of(colCode, colEvent, colVenue, colArea, colSlot, colVehicle, colStatus, colActions));
    }

    private void loadReservations() {
        table.getItems().clear();
        User u = AuthService.getInstance().getCurrentUser();
        if (u != null) {
            List<Reservation> list = reservationDAO.findByUserId(u.getUserId());
            table.getItems().addAll(list);
        }
    }
}
