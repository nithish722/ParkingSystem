package com.crisscross.ui.admin;

import com.crisscross.dao.ParkingAreaDAO;
import com.crisscross.models.ParkingArea;
import com.crisscross.ui.ScreenManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class ParkingAreaManagementView extends VBox {
    private final ScreenManager screenManager;
    private final ParkingAreaDAO areaDAO = new ParkingAreaDAO();
    private final TableView<ParkingArea> table = new TableView<>();

    public ParkingAreaManagementView(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.setSpacing(20);
        this.setFillWidth(true);

        HBox topBox = new HBox(16);
        topBox.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(4);
        Label title = new Label("Parking Area Management");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("Configure parking zones (SJT, CDMM, Woody's), capacities, and vehicle eligibility");
        subtitle.getStyleClass().add("text-subtle");
        titleBox.getChildren().addAll(title, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn = new Button("+ Add Parking Area");
        addBtn.getStyleClass().add("button-primary");
        addBtn.setOnAction(e -> showAreaDialog(null));

        topBox.getChildren().addAll(titleBox, spacer, addBtn);

        setupTable();
        loadAreas();

        this.getChildren().addAll(topBox, table);
    }

    private void setupTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(480);

        TableColumn<ParkingArea, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getAreaId()));
        colId.setMaxWidth(60);

        TableColumn<ParkingArea, String> colName = new TableColumn<>("Zone Name");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));

        TableColumn<ParkingArea, String> colCode = new TableColumn<>("Code");
        colCode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCode()));
        colCode.setMaxWidth(90);

        TableColumn<ParkingArea, String> colVeh = new TableColumn<>("Vehicle Type");
        colVeh.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getVehicleType()));

        TableColumn<ParkingArea, Number> colTotal = new TableColumn<>("Total Capacity");
        colTotal.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getTotalCapacity()));

        TableColumn<ParkingArea, Number> colAvail = new TableColumn<>("Available");
        colAvail.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getAvailableCapacity()));

        TableColumn<ParkingArea, String> colVenue = new TableColumn<>("Nearest Venue");
        colVenue.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNearestVenue() + " (" + d.getValue().getWalkingTimeMins() + " min)"));

        TableColumn<ParkingArea, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    ParkingArea a = getTableRow().getItem();
                    Label b = new Label(a.getStatus());
                    if ("OPEN".equalsIgnoreCase(a.getStatus())) b.getStyleClass().add("badge-available");
                    else if ("FULL".equalsIgnoreCase(a.getStatus())) b.getStyleClass().add("badge-occupied");
                    else b.getStyleClass().add("badge-reserved");
                    setGraphic(b);
                }
            }
        });

        TableColumn<ParkingArea, Void> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            {
                editBtn.getStyleClass().add("button-secondary");
                editBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4px 10px;");
                editBtn.setOnAction(e -> {
                    ParkingArea a = getTableRow().getItem();
                    if (a != null) showAreaDialog(a);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : editBtn);
            }
        });

        table.getColumns().addAll(List.of(colId, colName, colCode, colVeh, colTotal, colAvail, colVenue, colStatus, colActions));
    }

    private void loadAreas() {
        table.getItems().clear();
        table.getItems().addAll(areaDAO.findAll());
    }

    private void showAreaDialog(ParkingArea existing) {
        boolean isEdit = (existing != null);

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("modal-overlay");

        VBox dialog = new VBox(16);
        dialog.getStyleClass().add("modal-dialog");
        dialog.setMaxWidth(500);

        Label titleLbl = new Label(isEdit ? "Edit Parking Area" : "Add Parking Area");
        titleLbl.getStyleClass().add("heading-1");

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(10);

        TextField nameField = new TextField(isEdit ? existing.getName() : "");
        TextField codeField = new TextField(isEdit ? existing.getCode() : "");
        if (isEdit) codeField.setDisable(true);

        ComboBox<String> vTypeCombo = new ComboBox<>();
        vTypeCombo.getItems().addAll("BOTH", "CAR", "BIKE");
        vTypeCombo.setValue(isEdit ? existing.getVehicleType() : "BOTH");

        TextField capField = new TextField(isEdit ? String.valueOf(existing.getTotalCapacity()) : "30");
        TextField venueField = new TextField(isEdit ? existing.getNearestVenue() : "SJT Auditorium");
        TextField walkField = new TextField(isEdit ? String.valueOf(existing.getWalkingTimeMins()) : "2");
        TextField descField = new TextField(isEdit ? existing.getLocationDesc() : "Campus Central Perimeter");

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("OPEN", "FULL", "MAINTENANCE");
        statusCombo.setValue(isEdit ? existing.getStatus() : "OPEN");

        grid.add(new Label("Area Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Code:"), 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(new Label("Vehicle Types:"), 0, 2);
        grid.add(vTypeCombo, 1, 2);
        grid.add(new Label("Total Capacity:"), 0, 3);
        grid.add(capField, 1, 3);
        grid.add(new Label("Nearest Venue:"), 0, 4);
        grid.add(venueField, 1, 4);
        grid.add(new Label("Walk Time (Mins):"), 0, 5);
        grid.add(walkField, 1, 5);
        grid.add(new Label("Location Desc:"), 0, 6);
        grid.add(descField, 1, 6);
        grid.add(new Label("Status:"), 0, 7);
        grid.add(statusCombo, 1, 7);

        HBox btnBox = new HBox(12);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("button-secondary");
        cancelBtn.setOnAction(e -> ((StackPane) screenManager.getPrimaryStage().getScene().getRoot()).getChildren().remove(overlay));

        Button saveBtn = new Button(isEdit ? "Update Area" : "Save Area");
        saveBtn.getStyleClass().add("button-primary");
        saveBtn.setOnAction(e -> {
            ParkingArea a = isEdit ? existing : new ParkingArea();
            String enteredName = nameField.getText() != null ? nameField.getText().trim() : "";
            if (enteredName.isEmpty()) enteredName = "NEW CAMPUS PARKING";
            a.setName(enteredName);

            String enteredCode = codeField.getText() != null ? codeField.getText().trim().toUpperCase() : "";
            if (enteredCode.isEmpty()) {
                String firstWord = enteredName.split("\\s+")[0].replaceAll("[^A-Za-z0-9]", "").toUpperCase();
                enteredCode = firstWord.isEmpty() ? "ZONE" : firstWord;
            }
            a.setCode(enteredCode);
            a.setVehicleType(vTypeCombo.getValue() != null ? vTypeCombo.getValue() : "BOTH");

            int cap = isEdit ? existing.getTotalCapacity() : 30;
            try {
                cap = Math.max(1, Integer.parseInt(capField.getText().trim()));
            } catch (Exception ignored) {}
            a.setTotalCapacity(cap);
            if (!isEdit) {
                a.setAvailableCapacity(cap);
            }

            a.setNearestVenue(venueField.getText().trim().isEmpty() ? "Main Campus" : venueField.getText().trim());
            try {
                a.setWalkingTimeMins(Math.max(1, Integer.parseInt(walkField.getText().trim())));
            } catch (Exception ignored) {
                a.setWalkingTimeMins(2);
            }
            a.setLocationDesc(descField.getText().trim().isEmpty() ? "Campus Perimeter" : descField.getText().trim());
            a.setStatus(statusCombo.getValue() != null ? statusCombo.getValue() : "OPEN");

            if (isEdit) {
                areaDAO.update(a);
            } else {
                areaDAO.insert(a);
            }
            screenManager.setSelectedParkingArea(a);

            ((StackPane) screenManager.getPrimaryStage().getScene().getRoot()).getChildren().remove(overlay);
            loadAreas();
        });

        btnBox.getChildren().addAll(cancelBtn, saveBtn);
        dialog.getChildren().addAll(titleLbl, grid, btnBox);
        overlay.getChildren().add(dialog);

        ((StackPane) screenManager.getPrimaryStage().getScene().getRoot()).getChildren().add(overlay);
    }
}
