package com.crisscross.ui.customer;

import com.crisscross.dao.ParkingAreaDAO;
import com.crisscross.dao.ParkingSlotDAO;
import com.crisscross.models.Event;
import com.crisscross.models.ParkingArea;
import com.crisscross.models.ParkingSlot;
import com.crisscross.ui.ScreenManager;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import javafx.scene.shape.Polygon;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * 2D Architectural Parking Blueprint Dashboard (Flexopus-inspired Floor Plan).
 * Features 3 designated sectors (SJT, CDMM, Woody's), top-down parking bays
 * arranged with driving aisles, realistic vertical slot cards ("P"),
 * real-time availability/occupancy color coding, and an interactive floating popover tooltip.
 */
public class ParkingMapView extends VBox {
    private final ScreenManager screenManager;
    private final ParkingAreaDAO areaDAO = new ParkingAreaDAO();
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();

    private final List<ParkingArea> allAreas;
    private ParkingArea activeArea;
    private String vehicleType = "CAR";
    private ParkingSlot selectedSlot = null;

    // UI Containers
    private final HBox sectorTabsBox = new HBox(10);
    private final VBox floorplanContainer = new VBox(14);
    private final StackPane boardStack = new StackPane();
    private final VBox popoverCard = new VBox(8);
    private final Polygon popoverCaret = new Polygon();
    private final VBox popoverWrapper = new VBox();

    // Bottom Summary Controls
    private final Label chosenSpotLbl = new Label("None selected (Click any available white bay)");
    private final Label sectorMetaLbl = new Label();
    private final Button proceedBtn = new Button("Proceed to Booking with Selected Spot →");

    public ParkingMapView(ScreenManager screenManager) {
        this.screenManager = screenManager;
        this.allAreas = areaDAO.findAll();
        ParkingArea preArea = screenManager.getSelectedParkingArea();
        this.activeArea = (preArea != null) ? preArea : (!allAreas.isEmpty() ? allAreas.get(0) : null);
        if (screenManager.getSelectedVehicleType() != null && !screenManager.getSelectedVehicleType().isEmpty()) {
            this.vehicleType = screenManager.getSelectedVehicleType();
        }
        this.selectedSlot = screenManager.getSelectedParkingSlot();

        this.setSpacing(18);
        this.setFillWidth(true);

        // 1. Dashboard Header
        VBox header = new VBox(4);
        Label title = new Label("Campus Parking 2D Architectural Blueprint");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("Sector-Wise Parking Floor Plan • Real-time Bay Allocation & Interactive Spot Selector (Flexopus-Inspired Layout)");
        subtitle.getStyleClass().add("text-subtle");
        header.getChildren().addAll(title, subtitle);

        // 2. Sector Navigation Toolbar & Vehicle Category Filter
        HBox toolbar = buildToolbar();

        // 3. Architectural Floor Plan Board
        setupFloorplanBoard();

        // 4. Bottom Selection & Booking Summary Bar
        HBox bottomSummary = buildBottomSummary();

        this.getChildren().addAll(header, toolbar, boardStack, bottomSummary);
        VBox.setVgrow(this, Priority.ALWAYS);

        // Initial Render
        renderFloorplan();
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("glass-card");
        bar.setPadding(new Insets(12, 18, 12, 18));

        // 3 Sector Buttons
        sectorTabsBox.setAlignment(Pos.CENTER_LEFT);
        sectorTabsBox.setSpacing(8);
        updateSectorTabs();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Vehicle Type Toggle
        ToggleGroup tg = new ToggleGroup();
        RadioButton carRadio = new RadioButton("🚗 Car Bays");
        carRadio.setToggleGroup(tg);
        carRadio.setStyle("-fx-text-fill: #CBD5E1; -fx-font-weight: 600; -fx-font-size: 12px;");

        RadioButton bikeRadio = new RadioButton("🏍️ Bike Bays");
        bikeRadio.setToggleGroup(tg);
        bikeRadio.setStyle("-fx-text-fill: #CBD5E1; -fx-font-weight: 600; -fx-font-size: 12px;");

        if ("BIKE".equalsIgnoreCase(this.vehicleType)) {
            bikeRadio.setSelected(true);
        } else {
            carRadio.setSelected(true);
        }

        tg.selectedToggleProperty().addListener((obs, oldV, newV) -> {
            this.vehicleType = carRadio.isSelected() ? "CAR" : "BIKE";
            this.selectedSlot = null;
            hidePopover();
            updateBottomSummary();
            renderFloorplan();
        });

        HBox vehicleBox = new HBox(10, carRadio, bikeRadio);
        vehicleBox.setAlignment(Pos.CENTER);

        // Legend
        HBox legend = new HBox(12);
        legend.setAlignment(Pos.CENTER);
        legend.getChildren().addAll(
                createLegendDot("Available", "#f8fafc", "#94A3B8"),
                createLegendDot("Selected", "#F16E68", "#ffffff"),
                createLegendDot("Occupied", "#dc2626", "#ffffff"),
                createLegendDot("Reserved", "#d97706", "#ffffff")
        );

        bar.getChildren().addAll(sectorTabsBox, spacer, vehicleBox, legend);
        return bar;
    }

    private void updateSectorTabs() {
        sectorTabsBox.getChildren().clear();

        for (int i = 0; i < allAreas.size(); i++) {
            ParkingArea area = allAreas.get(i);
            boolean isSelected = activeArea != null && activeArea.getAreaId() == area.getAreaId();

            String icon = (i == 0) ? "🏢 " : (i == 1) ? "🏛️ " : "🌳 ";
            Button tabBtn = new Button(icon + "Sector " + (i + 1) + ": " + area.getCode() + " Hub");

            if (isSelected) {
                tabBtn.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, #E8B86D, #F16E68); "
                        + "-fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 7px 16px; "
                        + "-fx-background-radius: 14px; -fx-effect: dropshadow(gaussian, rgba(241, 110, 104, 0.45), 12, 0.2, 0, 3);");
            } else {
                tabBtn.setStyle("-fx-background-color: rgba(255, 255, 255, 0.05); -fx-border-color: rgba(255, 255, 255, 0.15); "
                        + "-fx-border-radius: 14px; -fx-background-radius: 14px; -fx-text-fill: #CBD5E1; -fx-font-size: 12px; "
                        + "-fx-font-weight: 600; -fx-padding: 7px 16px; -fx-cursor: hand;");
            }

            tabBtn.setOnAction(e -> {
                this.activeArea = area;
                this.selectedSlot = null;
                hidePopover();
                updateSectorTabs();
                updateBottomSummary();
                renderFloorplan();
            });

            sectorTabsBox.getChildren().add(tabBtn);
        }
    }

    private void setupFloorplanBoard() {
        floorplanContainer.getStyleClass().add("floorplan-board");
        floorplanContainer.setAlignment(Pos.CENTER);
        floorplanContainer.setPadding(new Insets(24));
        floorplanContainer.setSpacing(14);

        // Popover Caret & Card Setup
        popoverCard.getStyleClass().add("floorplan-popover");
        popoverCard.setMaxWidth(340);
        popoverCard.setAlignment(Pos.TOP_LEFT);

        popoverCaret.getPoints().addAll(
                0.0, 0.0,
                16.0, 0.0,
                8.0, 10.0
        );
        popoverCaret.setStyle("-fx-fill: rgba(28, 14, 12, 0.98);");

        popoverWrapper.setAlignment(Pos.CENTER);
        popoverWrapper.getChildren().addAll(popoverCard, popoverCaret);
        popoverWrapper.setVisible(false);
        popoverWrapper.setMouseTransparent(false);
        popoverWrapper.setManaged(false); // floats over the floor plan

        boardStack.setAlignment(Pos.CENTER);
        boardStack.getChildren().addAll(floorplanContainer, popoverWrapper);
        VBox.setVgrow(boardStack, Priority.ALWAYS);
    }

    private void renderFloorplan() {
        floorplanContainer.getChildren().clear();
        hidePopover();

        if (activeArea == null) return;

        List<ParkingSlot> slots = slotDAO.findByAreaAndType(activeArea.getAreaId(), vehicleType);
        if (slots.isEmpty()) {
            slots = slotDAO.findByArea(activeArea.getAreaId());
        }

        // Header strip for this sector inside blueprint
        HBox planHeader = new HBox(12);
        planHeader.setAlignment(Pos.CENTER_LEFT);
        Label sectorTitle = new Label("SECTOR " + activeArea.getCode() + " • " + activeArea.getName() + " BLUEPRINT");
        sectorTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #CBD5E1;");

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Label venueTag = new Label("📍 Nearest Landmark: " + activeArea.getNearestVenue() + " (" + activeArea.getWalkingTimeMins() + " min walk)");
        venueTag.setStyle("-fx-font-size: 11px; -fx-text-fill: #F2A06A; -fx-font-weight: bold;");

        planHeader.getChildren().addAll(sectorTitle, hSpacer, venueTag);
        floorplanContainer.getChildren().add(planHeader);

        // Partition slots into 3 architectural rows (Row A / Top, Row B / Center Dual, Row C / Bottom)
        int total = slots.size();
        int row1Count = Math.min(10, total);
        int row2Count = Math.min(10, Math.max(0, total - row1Count));
        int row3Count = Math.max(0, total - row1Count - row2Count);

        List<ParkingSlot> row1 = new ArrayList<>(slots.subList(0, row1Count));
        List<ParkingSlot> row2 = new ArrayList<>(slots.subList(row1Count, row1Count + row2Count));
        List<ParkingSlot> row3 = new ArrayList<>(slots.subList(row1Count + row2Count, total));

        // Row 1: Top Parking Bays
        HBox row1Box = buildAisleRow("ROW 1 (NORTH WING)", row1);
        floorplanContainer.getChildren().add(row1Box);

        // Driving Aisle 1 (Ingress)
        HBox lane1 = buildDrivingLane("▲ INGRESS ROADWAY (LANE 01) ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ──");
        floorplanContainer.getChildren().add(lane1);

        // Row 2: Center Parking Bays
        if (!row2.isEmpty()) {
            HBox row2Box = buildAisleRow("ROW 2 (CENTER AISLE)", row2);
            floorplanContainer.getChildren().add(row2Box);
        }

        // Driving Aisle 2 (Egress)
        HBox lane2 = buildDrivingLane("▼ EGRESS ROADWAY (LANE 02) ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ──");
        floorplanContainer.getChildren().add(lane2);

        // Row 3: Bottom Parking Bays (includes occupied / reserved bays)
        if (!row3.isEmpty()) {
            HBox row3Box = buildAisleRow("ROW 3 (SOUTH WING)", row3);
            floorplanContainer.getChildren().add(row3Box);
        }

        updateBottomSummary();
    }

    private HBox buildDrivingLane(String text) {
        HBox lane = new HBox();
        lane.setAlignment(Pos.CENTER);
        lane.getStyleClass().add("floorplan-road-lane");

        Label lbl = new Label(text);
        lbl.getStyleClass().add("floorplan-lane-text");
        lane.getChildren().add(lbl);
        return lane;
    }

    private HBox buildAisleRow(String rowLabel, List<ParkingSlot> slotList) {
        HBox rowContainer = new HBox(12);
        rowContainer.setAlignment(Pos.CENTER);

        // Row marker on the left
        VBox tagBox = new VBox(2);
        tagBox.setAlignment(Pos.CENTER);
        tagBox.setPrefWidth(60);
        Label tag = new Label(rowLabel.split(" ")[0] + " " + rowLabel.split(" ")[1]);
        tag.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: #9E5A4B;");
        tagBox.getChildren().add(tag);
        rowContainer.getChildren().add(tagBox);

        // Cluster slots in groups of 2-3 bays separated by architectural pillar markers
        HBox clustersBox = new HBox(8);
        clustersBox.setAlignment(Pos.CENTER);

        for (int i = 0; i < slotList.size(); i++) {
            ParkingSlot slot = slotList.get(i);
            Button spotBtn = createSpotButton(slot);
            clustersBox.getChildren().add(spotBtn);

            // Add architectural divider / pillar every 2-3 slots (like in the image!)
            if ((i + 1) % 3 == 0 && (i + 1) < slotList.size()) {
                VBox pillar = new VBox();
                pillar.setPrefWidth(12);
                pillar.setMinHeight(80);
                pillar.setMaxHeight(80);
                pillar.setAlignment(Pos.CENTER);
                Region pillarCore = new Region();
                pillarCore.setStyle("-fx-background-color: rgba(255, 255, 255, 0.12); -fx-background-radius: 3px; -fx-min-width: 6px; -fx-max-width: 6px; -fx-min-height: 48px; -fx-max-height: 48px;");
                pillar.getChildren().add(pillarCore);
                clustersBox.getChildren().add(pillar);
            }
        }

        rowContainer.getChildren().add(clustersBox);
        return rowContainer;
    }

    private Button createSpotButton(ParkingSlot slot) {
        Button btn = new Button();
        btn.getStyleClass().add("floorplan-spot");

        // Format slot number (e.g. C02 or 02)
        String rawNum = slot.getSlotNumber();
        String displayNum = rawNum;
        if (rawNum.contains("-")) {
            displayNum = rawNum.substring(rawNum.lastIndexOf('-') + 1);
        }

        // Layout inside bay: Bold 'P' on top, Slot Number on bottom
        VBox bayContent = new VBox(2);
        bayContent.setAlignment(Pos.CENTER);

        Label pLbl = new Label("P");
        pLbl.setStyle("-fx-font-size: 19px; -fx-font-weight: 900; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

        Label numLbl = new Label(displayNum);
        numLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

        bayContent.getChildren().addAll(pLbl, numLbl);
        btn.setGraphic(bayContent);

        // State classes
        if (slot.isAvailable()) {
            boolean isCurrentlySelected = selectedSlot != null && selectedSlot.getSlotId() == slot.getSlotId();
            if (isCurrentlySelected) {
                btn.getStyleClass().add("floorplan-spot-selected");
                pLbl.setStyle(pLbl.getStyle() + "-fx-text-fill: #ffffff;");
                numLbl.setStyle(numLbl.getStyle() + "-fx-text-fill: #ffffff;");
            } else {
                btn.getStyleClass().add("floorplan-spot-available");
                pLbl.setStyle(pLbl.getStyle() + "-fx-text-fill: #0F172A;");
                numLbl.setStyle(numLbl.getStyle() + "-fx-text-fill: #1E293B;");
            }

            btn.setOnAction(e -> {
                handleSpotSelection(slot, btn);
            });
        } else if ("OCCUPIED".equalsIgnoreCase(slot.getStatus())) {
            btn.getStyleClass().add("floorplan-spot-occupied");
            pLbl.setStyle(pLbl.getStyle() + "-fx-text-fill: #ffffff;");
            numLbl.setStyle(numLbl.getStyle() + "-fx-text-fill: #fecaca;");

            btn.setOnAction(e -> {
                showOccupiedTooltip(slot, btn);
            });
        } else if ("RESERVED".equalsIgnoreCase(slot.getStatus())) {
            btn.getStyleClass().add("floorplan-spot-reserved");
            pLbl.setStyle(pLbl.getStyle() + "-fx-text-fill: #ffffff;");
            numLbl.setStyle(numLbl.getStyle() + "-fx-text-fill: #fef3c7;");

            btn.setOnAction(e -> {
                showReservedTooltip(slot, btn);
            });
        } else {
            btn.getStyleClass().add("floorplan-spot-blocked");
            pLbl.setStyle(pLbl.getStyle() + "-fx-text-fill: #94a3b8;");
            numLbl.setStyle(numLbl.getStyle() + "-fx-text-fill: #64748b;");
        }

        return btn;
    }

    private void handleSpotSelection(ParkingSlot slot, Button btn) {
        this.selectedSlot = slot;

        // Reset all other available bays to normal state
        resetAvailableBayStyles();

        // Highlight selected bay
        btn.getStyleClass().remove("floorplan-spot-available");
        if (!btn.getStyleClass().contains("floorplan-spot-selected")) {
            btn.getStyleClass().add("floorplan-spot-selected");
        }
        if (btn.getGraphic() instanceof VBox vb) {
            for (var node : vb.getChildren()) {
                if (node instanceof Label l) l.setStyle(l.getStyle() + "-fx-text-fill: #ffffff;");
            }
        }

        // Tactile bounce
        ScaleTransition st = new ScaleTransition(Duration.millis(90), btn);
        st.setToX(1.08);
        st.setToY(1.08);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();

        // Display the interactive floating popover over this bay!
        displayFloatingPopover(slot, btn);
        updateBottomSummary();
    }

    private void resetAvailableBayStyles() {
        for (var node : floorplanContainer.getChildren()) {
            if (node instanceof HBox rowBox) {
                for (var child : rowBox.getChildren()) {
                    if (child instanceof HBox clusterBox) {
                        for (var item : clusterBox.getChildren()) {
                            if (item instanceof Button b && b.getStyleClass().contains("floorplan-spot-selected")) {
                                b.getStyleClass().remove("floorplan-spot-selected");
                                b.getStyleClass().add("floorplan-spot-available");
                                if (b.getGraphic() instanceof VBox vb) {
                                    for (var n : vb.getChildren()) {
                                        if (n instanceof Label l) {
                                            if (l.getText().equals("P")) l.setStyle("-fx-font-size: 19px; -fx-font-weight: 900; -fx-text-fill: #0F172A;");
                                            else l.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #1E293B;");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void displayFloatingPopover(ParkingSlot slot, Button targetBtn) {
        popoverCard.getChildren().clear();

        // Top Row: Title + Status Pill (Matching user template image)
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label("Parking Spot " + slot.getSlotNumber());
        titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label("available");
        statusBadge.setStyle("-fx-background-color: rgba(74, 222, 128, 0.22); -fx-text-fill: #4ade80; "
                + "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3px 10px; -fx-background-radius: 10px; "
                + "-fx-border-color: rgba(74, 222, 128, 0.50); -fx-border-radius: 10px;");

        topRow.getChildren().addAll(titleLbl, spacer, statusBadge);

        // Feature / Attribute Pills (e.g. electric charger from image)
        HBox pillsBox = new HBox(8);
        pillsBox.setAlignment(Pos.CENTER_LEFT);

        int slotNumDigits = 0;
        try {
            String digits = slot.getSlotNumber().replaceAll("\\D+", "");
            if (!digits.isEmpty()) slotNumDigits = Integer.parseInt(digits);
        } catch (Exception ignored) {}

        boolean hasCharger = (slotNumDigits % 2 == 0); // Alternate bays feature EV charger
        if (hasCharger) {
            Label chargerPill = new Label("⚡ electric charger");
            chargerPill.setStyle("-fx-background-color: rgba(242, 160, 106, 0.16); -fx-text-fill: #F2A06A; "
                    + "-fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 3px 8px; -fx-background-radius: 8px; "
                    + "-fx-border-color: rgba(242, 160, 106, 0.40); -fx-border-radius: 8px;");
            pillsBox.getChildren().add(chargerPill);
        }

        Label levelPill = new Label("🏢 " + slot.getFloorLevel());
        levelPill.setStyle("-fx-background-color: rgba(255, 255, 255, 0.08); -fx-text-fill: #CBD5E1; "
                + "-fx-font-size: 10px; -fx-font-weight: 600; -fx-padding: 3px 8px; -fx-background-radius: 8px;");
        pillsBox.getChildren().add(levelPill);

        Label walkPill = new Label("🚶 " + activeArea.getWalkingTimeMins() + " min to " + activeArea.getNearestVenue());
        walkPill.setStyle("-fx-font-size: 11px; -fx-text-fill: #CBB5AA;");

        // Action Button
        Button bookBtn = new Button("Confirm & Reserve Bay " + slot.getSlotNumber() + " →");
        bookBtn.getStyleClass().add("button-primary");
        bookBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6px 14px;");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setOnAction(e -> triggerBookingWithSlot(slot));

        popoverCard.getChildren().addAll(topRow, pillsBox, walkPill, bookBtn);

        // Position Popover over the board
        positionPopoverAbove(targetBtn);
    }

    private void showOccupiedTooltip(ParkingSlot slot, Button targetBtn) {
        popoverCard.getChildren().clear();

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label("Parking Spot " + slot.getSlotNumber());
        titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label("occupied");
        statusBadge.setStyle("-fx-background-color: rgba(239, 68, 68, 0.25); -fx-text-fill: #f87171; "
                + "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3px 10px; -fx-background-radius: 10px; "
                + "-fx-border-color: rgba(239, 68, 68, 0.50); -fx-border-radius: 10px;");
        topRow.getChildren().addAll(titleLbl, spacer, statusBadge);

        Label desc = new Label("This parking spot is currently booked by another vehicle for the ongoing campus event.");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #fca5a5;");

        popoverCard.getChildren().addAll(topRow, desc);
        positionPopoverAbove(targetBtn);
    }

    private void showReservedTooltip(ParkingSlot slot, Button targetBtn) {
        popoverCard.getChildren().clear();

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label("Parking Spot " + slot.getSlotNumber());
        titleLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label("reserved");
        statusBadge.setStyle("-fx-background-color: rgba(245, 158, 11, 0.25); -fx-text-fill: #fbbf24; "
                + "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3px 10px; -fx-background-radius: 10px; "
                + "-fx-border-color: rgba(245, 158, 11, 0.50); -fx-border-radius: 10px;");
        topRow.getChildren().addAll(titleLbl, spacer, statusBadge);

        Label desc = new Label("This spot is reserved for VIP university faculty / event judges.");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 11px; -fx-text-fill: #fde68a;");

        popoverCard.getChildren().addAll(topRow, desc);
        positionPopoverAbove(targetBtn);
    }

    private void positionPopoverAbove(Button targetBtn) {
        popoverWrapper.setVisible(true);

        // Compute local coordinates relative to boardStack
        try {
            var btnBounds = targetBtn.localToScene(targetBtn.getBoundsInLocal());
            var stackBounds = boardStack.localToScene(boardStack.getBoundsInLocal());

            double x = btnBounds.getMinX() - stackBounds.getMinX() - (popoverCard.getMaxWidth() / 2.0) + (targetBtn.getWidth() / 2.0);
            double y = btnBounds.getMinY() - stackBounds.getMinY() - 145.0;

            // Keep within board limits
            x = Math.max(20, Math.min(boardStack.getWidth() - popoverCard.getMaxWidth() - 20, x));
            y = Math.max(10, y);

            popoverWrapper.setLayoutX(x);
            popoverWrapper.setLayoutY(y);

            FadeTransition ft = new FadeTransition(Duration.millis(160), popoverWrapper);
            ft.setFromValue(0.0);
            ft.setToValue(1.0);
            ft.play();
        } catch (Exception ex) {
            popoverWrapper.setLayoutX(40);
            popoverWrapper.setLayoutY(30);
        }
    }

    private void hidePopover() {
        popoverWrapper.setVisible(false);
    }

    private HBox buildBottomSummary() {
        HBox bottom = new HBox(20);
        bottom.getStyleClass().add("glass-card-highlight");
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(16, 24, 16, 24));

        VBox leftInfo = new VBox(3);
        Label tag = new Label("SELECTED ARCHITECTURAL PARKING BAY:");
        tag.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #94A3B8;");
        chosenSpotLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #F2A06A;");
        sectorMetaLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #CBD5E1;");
        leftInfo.getChildren().addAll(tag, chosenSpotLbl, sectorMetaLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        proceedBtn.getStyleClass().add("button-primary");
        proceedBtn.setDisable(true);
        proceedBtn.setOnAction(e -> {
            if (selectedSlot != null) {
                triggerBookingWithSlot(selectedSlot);
            }
        });

        bottom.getChildren().addAll(leftInfo, spacer, proceedBtn);
        return bottom;
    }

    private void updateBottomSummary() {
        if (activeArea == null) return;

        sectorMetaLbl.setText("Sector: " + activeArea.getName() + " (" + activeArea.getCode() + ") • Landmark: "
                + activeArea.getNearestVenue() + " (" + activeArea.getWalkingTimeMins() + " min walk) • Rate: "
                + (activeArea.getHourlyRate() > 0 ? "₹" + activeArea.getHourlyRate() + "/hr" : "Free Campus Bay"));

        if (selectedSlot != null) {
            chosenSpotLbl.setText("Bay: " + selectedSlot.getSlotNumber() + "  (" + selectedSlot.getFloorLevel() + " • " + selectedSlot.getSlotType() + ")");
            proceedBtn.setDisable(false);
            proceedBtn.setText("Proceed with Bay " + selectedSlot.getSlotNumber() + " →");
        } else {
            chosenSpotLbl.setText("None selected (Click any available white parking bay above)");
            proceedBtn.setDisable(true);
            proceedBtn.setText("Select an Available Bay to Proceed →");
        }
    }

    private void triggerBookingWithSlot(ParkingSlot slot) {
        Event currentEvent = screenManager.getSelectedEventForBooking();
        String vehicleNumber = screenManager.getUserVehicleNumber();
        if (vehicleNumber == null || vehicleNumber.isEmpty()) vehicleNumber = "TN-22-CR-1024";

        screenManager.showBookingSummary(
                currentEvent,
                activeArea,
                slot,
                this.vehicleType,
                vehicleNumber
        );
    }

    private HBox createLegendDot(String text, String fillColor, String textColor) {
        HBox dotBox = new HBox(6);
        dotBox.setAlignment(Pos.CENTER);

        Region dot = new Region();
        dot.setStyle("-fx-background-color: " + fillColor + "; -fx-background-radius: 4px; -fx-min-width: 12px; -fx-max-width: 12px; -fx-min-height: 12px; -fx-max-height: 12px; -fx-border-color: rgba(255,255,255,0.4); -fx-border-radius: 4px;");

        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #E8C5B0; -fx-font-weight: 600;");

        dotBox.getChildren().addAll(dot, lbl);
        return dotBox;
    }
}
