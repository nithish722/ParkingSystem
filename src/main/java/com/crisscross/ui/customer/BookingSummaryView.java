package com.crisscross.ui.customer;

import com.crisscross.dao.ParkingAreaDAO;
import com.crisscross.dao.ParkingSlotDAO;
import com.crisscross.models.*;
import com.crisscross.services.AuthService;
import com.crisscross.services.BookingService;
import com.crisscross.services.ParkingPricingService;
import com.crisscross.ui.ScreenManager;
import com.crisscross.util.QRCodeUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class BookingSummaryView extends VBox {

    private final ScreenManager screenManager;
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final ParkingAreaDAO areaDAO = new ParkingAreaDAO();

    private final Event activeEvent;
    private final ParkingArea activeArea;
    private final String activeVehicleType;
    private final String activeVehicleNumber;
    private ParkingSlot currentSlot;

    // Dynamic UI labels updated when user clicks another bay on the map
    private Label reservedSlotValueLbl;
    private Label vehicleClassValueLbl;
    private final Label mapSubtitleLbl = new Label();
    private final Label locatorTargetLbl = new Label();
    private final Label locatorSubLbl = new Label();
    private final VBox floorplanContainer = new VBox(12);

    // Selected payment mode state ("UPI", "CARD", "NET")
    private String selectedModeKey = "UPI";
    private String selectedPaymentMethodTitle = "UPI QR Scanner & Instant Pay (GPay / PhonePe)";

    public BookingSummaryView(ScreenManager screenManager, Event event, ParkingArea area,
                              ParkingSlot slot, String vehicleType, String vehicleNumber) {
        this.screenManager = screenManager;
        this.activeEvent = event;
        List<ParkingArea> allAreas = areaDAO.findAll();
        this.activeArea = (area != null) ? area : (!allAreas.isEmpty() ? allAreas.get(0) : null);
        this.activeVehicleType = (vehicleType != null && !vehicleType.isEmpty()) ? vehicleType : "CAR";
        this.activeVehicleNumber = (vehicleNumber != null && !vehicleNumber.isEmpty()) ? vehicleNumber : "TN-22-CR-1024";
        this.currentSlot = slot;

        this.setSpacing(20);
        this.setFillWidth(true);

        Button backBtn = new Button("← Change Selected Slot");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setOnAction(e -> screenManager.showSlotSelection(activeEvent, activeArea, activeVehicleType));

        // Header
        VBox header = new VBox(6);
        Label title = new Label("Review & Confirm Parking Reservation");
        title.getStyleClass().add("heading-1");
        Label subtitle = new Label("Please verify your event entry and locate your designated parking bay on the blueprint prior to final pass generation.");
        subtitle.getStyleClass().add("text-subtle");
        header.getChildren().addAll(title, subtitle);

        // =========================================================================
        // LEFT COLUMN: Summary Review Card
        // =========================================================================
        VBox summaryCard = new VBox(20);
        summaryCard.getStyleClass().add("glass-card");
        summaryCard.setPrefWidth(510);
        summaryCard.setMinWidth(450);
        summaryCard.setMaxWidth(550);
        summaryCard.setPadding(new Insets(28));

        Label reviewBadge = new Label("SUMMARY REVIEW");
        reviewBadge.setStyle("-fx-background-color: rgba(232, 184, 109, 0.16); -fx-text-fill: #E8B86D; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5px 14px; -fx-background-radius: 12px; -fx-border-color: rgba(232, 184, 109, 0.35); -fx-border-radius: 12px;");

        // Event info block
        VBox eventBox = new VBox(6);
        eventBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.035); -fx-background-radius: 16px; -fx-padding: 16px 20px; -fx-border-color: rgba(255, 255, 255, 0.10); -fx-border-radius: 16px;");

        Label eTitle = new Label(activeEvent != null ? activeEvent.getTitle() : "Campus Event Parking");
        eTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

        Label eVenue = new Label("📍 Venue: " + (activeEvent != null ? activeEvent.getVenue() : "Main Quad") + "  •  Date: " + (activeEvent != null ? activeEvent.getEventDate() : "Upcoming"));
        eVenue.setStyle("-fx-font-size: 13px; -fx-text-fill: #F2A06A;");

        eventBox.getChildren().addAll(eTitle, eVenue);

        // Determine user pricing category
        User u = AuthService.getInstance().getCurrentUser();
        String userEmail = u != null && u.getEmail() != null ? u.getEmail() : "";
        boolean isVit = ParkingPricingService.isVitEmail(userEmail);
        double baseCharge = ParkingPricingService.getBaseCharge(userEmail, activeVehicleType);
        double overtimeRate = ParkingPricingService.getOvertimeHourlyRate(userEmail, activeVehicleType);
        String durationText = "4-Hour Event Access Pass";
        String amountText = isVit ? "₹ 0.00 (100% Free VIT Waiver)" : ("₹ " + String.format(Locale.US, "%.2f", baseCharge) + " (Visitor Standard Tariff)");
        String generatedTxnId = isVit
                ? ("VIT-WAIVER-" + (10000 + new Random().nextInt(90000)))
                : ("TXN-CRISS-2026-" + (10000 + new Random().nextInt(90000)));

        // Grid Details
        GridPane grid = new GridPane();
        grid.setHgap(26);
        grid.setVgap(14);
        grid.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-background-radius: 14px; -fx-padding: 18px 20px;");

        grid.add(createSummaryRow("Booked For (Attendee)", u != null ? u.getFullName() : "Alex Reynolds"), 0, 0);
        grid.add(createSummaryRow("Account Email", userEmail.isEmpty() ? "alex@vit.ac.in" : userEmail), 1, 0);

        grid.add(createSummaryRow("Allocated Parking Area", activeArea != null ? activeArea.getName() : "SJT PARKING"), 0, 1);

        VBox slotRowBox = createSummaryRow("Reserved Slot Identifier", currentSlot != null ? currentSlot.getSlotNumber() : "SJT-C01");
        reservedSlotValueLbl = (Label) slotRowBox.getChildren().get(1);
        reservedSlotValueLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #F2A06A;");
        grid.add(slotRowBox, 1, 1);

        VBox vehicleClassBox = createSummaryRow("Vehicle Classification", activeVehicleType + " (" + (currentSlot != null ? currentSlot.getFloorLevel() : "Ground") + ")");
        vehicleClassValueLbl = (Label) vehicleClassBox.getChildren().get(1);
        grid.add(vehicleClassBox, 0, 2);

        grid.add(createSummaryRow("Vehicle Plate Number", activeVehicleNumber), 1, 2);

        grid.add(createSummaryRow("Parking Pass Validity", durationText), 0, 3);
        grid.add(createSummaryRow("Total Amount Due", amountText), 1, 3);

        // =========================================================================
        // PAYMENT SECTION (Shown ONLY for Non-VIT Customer Emails!)
        // Uses Interactive SELECTING PANELS (No RadioButtons!)
        // =========================================================================
        VBox paymentSection = new VBox(12);
        TextField paymentAccountField = new TextField();
        Label accountInputLbl = new Label("UPI ID / Payment Credential:");
        accountInputLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");

        String emailPrefix = (!userEmail.isEmpty() && userEmail.contains("@"))
                ? userEmail.substring(0, userEmail.indexOf('@'))
                : "visitor";
        paymentAccountField.setText(emailPrefix + "@okaxis");
        paymentAccountField.setPromptText("Enter UPI ID (e.g., name@okaxis)");

        // Interactive Selecting Panel Cards
        HBox[] methodPanels = new HBox[3];
        Label[] methodBadges = new Label[3];

        Runnable refreshSelectingPanels = () -> {
            String[] keys = {"UPI", "CARD", "NET"};
            for (int i = 0; i < 3; i++) {
                boolean active = keys[i].equals(selectedModeKey);
                if (methodPanels[i] != null && methodBadges[i] != null) {
                    applySelectPanelStyle(methodPanels[i], methodBadges[i], active);
                }
            }
            if ("UPI".equals(selectedModeKey)) {
                selectedPaymentMethodTitle = "UPI QR Scanner & Instant Pay (GPay / PhonePe)";
                accountInputLbl.setText("UPI ID / VPA Address (or Scan QR on Next Screen):");
                paymentAccountField.setText(emailPrefix + "@okaxis");
                paymentAccountField.setPromptText("Enter UPI ID (e.g., name@okaxis)");
            } else if ("CARD".equals(selectedModeKey)) {
                selectedPaymentMethodTitle = "Credit / Debit Card (Visa / RuPay)";
                accountInputLbl.setText("Card Number (Masked for Security):");
                paymentAccountField.setText("4532 •••• •••• 8891  (Exp: 08/29)");
                paymentAccountField.setPromptText("Enter 16-digit Card Number");
            } else {
                selectedPaymentMethodTitle = "Net Banking / Campus FastTag";
                accountInputLbl.setText("Bank / FastTag Wallet ID:");
                paymentAccountField.setText("FASTTAG-" + activeVehicleNumber.replaceAll("[^A-Z0-9]", ""));
                paymentAccountField.setPromptText("Enter FastTag or NetBanking ID");
            }
        };

        if (!isVit) {
            paymentSection.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, rgba(241, 110, 104, 0.14) 0%, rgba(18, 22, 54, 0.55) 100%); "
                    + "-fx-background-radius: 16px; -fx-padding: 18px 20px; "
                    + "-fx-border-color: rgba(241, 110, 104, 0.50); -fx-border-radius: 16px; -fx-border-width: 1.2px;");

            HBox payHeaderRow = new HBox(10);
            payHeaderRow.setAlignment(Pos.CENTER_LEFT);
            Label payTitle = new Label("💳 Visitor Payment Details & Checkout");
            payTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");
            Region paySpacer = new Region();
            HBox.setHgrow(paySpacer, Priority.ALWAYS);
            Label payBadge = new Label("PAYMENT REQUIRED");
            payBadge.setStyle("-fx-background-color: rgba(241, 110, 104, 0.25); -fx-text-fill: #F2A06A; -fx-font-size: 10px; -fx-font-weight: 800; -fx-padding: 3px 10px; -fx-background-radius: 10px; -fx-border-color: rgba(241, 110, 104, 0.45); -fx-border-radius: 10px;");
            payHeaderRow.getChildren().addAll(payTitle, paySpacer, payBadge);

            Label paySub = new Label("Non-VIT email account detected (" + (userEmail.isEmpty() ? "external visitor" : userEmail) + "). Select your preferred payment method from the panel below:");
            paySub.setStyle("-fx-font-size: 11px; -fx-text-fill: #CBD5E1;");
            paySub.setWrapText(true);

            // Itemized Payment Breakdown Grid
            GridPane feeGrid = new GridPane();
            feeGrid.setHgap(20);
            feeGrid.setVgap(8);
            feeGrid.setStyle("-fx-background-color: rgba(0, 0, 0, 0.25); -fx-background-radius: 12px; -fx-padding: 12px 14px;");

            feeGrid.add(createSummaryRow("Base 4-Hr Slot Charge (" + activeVehicleType + ")", "₹ " + String.format(Locale.US, "%.2f", baseCharge)), 0, 0);
            feeGrid.add(createSummaryRow("Digital Gate QR & Pass Fee", "₹ 0.00 (Included)"), 1, 0);
            feeGrid.add(createSummaryRow("Overtime Rate (After 4 Hrs)", "+₹ " + String.format(Locale.US, "%.2f", overtimeRate) + "/hr (12%)"), 0, 1);
            feeGrid.add(createSummaryRow("Transaction Reference ID", generatedTxnId), 1, 1);

            // Selecting Panel Header
            Label methodPrompt = new Label("Select Payment Method (Click Panel to Select):");
            methodPrompt.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #E8B86D;");

            // Interactive Selecting Panel (3 full-width selectable cards — NO RadioButtons)
            VBox selectingPanelContainer = new VBox(8);

            methodBadges[0] = new Label();
            methodPanels[0] = createPaymentSelectionCard(
                    "📱",
                    "UPI QR Scanner & Instant Pay",
                    "Scan UPI QR code on next screen via GPay, PhonePe, Paytm or BHIM",
                    methodBadges[0],
                    () -> {
                        selectedModeKey = "UPI";
                        refreshSelectingPanels.run();
                    }
            );

            methodBadges[1] = new Label();
            methodPanels[1] = createPaymentSelectionCard(
                    "💳",
                    "Credit / Debit Card",
                    "Instant checkout with Visa, MasterCard, RuPay or Campus Card",
                    methodBadges[1],
                    () -> {
                        selectedModeKey = "CARD";
                        refreshSelectingPanels.run();
                    }
            );

            methodBadges[2] = new Label();
            methodPanels[2] = createPaymentSelectionCard(
                    "🏦",
                    "Net Banking / Campus FastTag",
                    "Direct institutional bank transfer or automated RFID gate FastTag",
                    methodBadges[2],
                    () -> {
                        selectedModeKey = "NET";
                        refreshSelectingPanels.run();
                    }
            );

            refreshSelectingPanels.run();
            selectingPanelContainer.getChildren().addAll(methodPanels[0], methodPanels[1], methodPanels[2]);

            VBox credentialBox = new VBox(4, accountInputLbl, paymentAccountField);

            HBox totalPayStrip = new HBox(10);
            totalPayStrip.setAlignment(Pos.CENTER_LEFT);
            totalPayStrip.setStyle("-fx-background-color: rgba(34, 197, 94, 0.14); -fx-background-radius: 10px; -fx-padding: 10px 14px; -fx-border-color: rgba(74, 222, 128, 0.40); -fx-border-radius: 10px;");
            Label totalPayLbl = new Label("Total Payable Now: ₹ " + String.format(Locale.US, "%.2f", baseCharge));
            totalPayLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #4ade80;");
            Region totSp = new Region();
            HBox.setHgrow(totSp, Priority.ALWAYS);
            Label secureTag = new Label("📷 Opens UPI Scanner on Confirm");
            secureTag.setStyle("-fx-font-size: 10px; -fx-text-fill: #E8B86D; -fx-font-weight: 700;");
            totalPayStrip.getChildren().addAll(totalPayLbl, totSp, secureTag);

            paymentSection.getChildren().addAll(payHeaderRow, paySub, feeGrid, methodPrompt, selectingPanelContainer, credentialBox, totalPayStrip);
        } else {
            selectedPaymentMethodTitle = "VIT Institutional Waiver";
            paymentSection.setStyle("-fx-background-color: rgba(34, 197, 94, 0.10); -fx-background-radius: 14px; -fx-padding: 14px 18px; -fx-border-color: rgba(74, 222, 128, 0.35); -fx-border-radius: 14px;");
            Label vitHeader = new Label("🎓 VIT Verified Community Account — 100% Free Parking Pass");
            vitHeader.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #4ade80;");
            Label vitDesc = new Label("Your VIT email (" + userEmail + ") qualifies for a 100% institutional parking fee waiver (₹ 0.00). No payment is required.");
            vitDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #CBD5E1;");
            vitDesc.setWrapText(true);
            paymentSection.getChildren().addAll(vitHeader, vitDesc);
        }

        // Policy Notice Box
        VBox policyBox = new VBox(6);
        policyBox.setStyle("-fx-background-color: rgba(232, 184, 109, 0.08); -fx-background-radius: 12px; -fx-padding: 14px 18px; -fx-border-color: rgba(232, 184, 109, 0.25); -fx-border-radius: 12px;");
        Label policyHeader = new Label("⏱️ Duration & Overtime Surcharge Policy:");
        policyHeader.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #E8B86D;");
        Label policyBody = new Label(ParkingPricingService.getOvertimePolicyDescription(userEmail, activeVehicleType));
        policyBody.setStyle("-fx-font-size: 11px; -fx-text-fill: #CBD5E1;");
        policyBody.setWrapText(true);
        policyBox.getChildren().addAll(policyHeader, policyBody);

        Label errorLbl = new Label();
        errorLbl.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-font-size: 13px;");
        errorLbl.setVisible(false);

        // Action Buttons
        HBox actionBox = new HBox(16);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("button-secondary");
        cancelBtn.setOnAction(e -> screenManager.showEventList());

        String confirmBtnText = isVit
                ? "Confirm & Generate Free Pass 🎟️"
                : ("Confirm & Open UPI Scanner (₹ " + String.format(Locale.US, "%.2f", baseCharge) + ") 📷");
        Button confirmBtn = new Button(confirmBtnText);
        confirmBtn.getStyleClass().add("button-primary");
        confirmBtn.setOnAction(e -> {
            errorLbl.setVisible(false);
            if (isVit) {
                // VIT Community Member -> Direct Free Pass Generation
                completeReservationAndShowConfirmation(u, userEmail, true, baseCharge, generatedTxnId, userEmail, errorLbl);
            } else {
                // General Customer -> Pop up the UPI Scanner Payment Screen first!
                openUpiScannerPaymentModal(
                        u,
                        userEmail,
                        baseCharge,
                        overtimeRate,
                        generatedTxnId,
                        paymentAccountField,
                        refreshSelectingPanels,
                        errorLbl
                );
            }
        });

        actionBox.getChildren().addAll(cancelBtn, confirmBtn);
        summaryCard.getChildren().addAll(reviewBadge, eventBox, grid, paymentSection, policyBox, errorLbl, actionBox);

        // =========================================================================
        // RIGHT COLUMN: Parking Map View (2D Architectural Slot Locator)
        // =========================================================================
        VBox mapCard = buildParkingMapLocatorCard();
        HBox.setHgrow(mapCard, Priority.ALWAYS);

        // Side-by-side Split Row
        HBox mainContentRow = new HBox(24);
        mainContentRow.setAlignment(Pos.TOP_LEFT);
        mainContentRow.getChildren().addAll(summaryCard, mapCard);
        VBox.setVgrow(mainContentRow, Priority.ALWAYS);

        this.getChildren().addAll(backBtn, header, mainContentRow);
    }

    /**
     * Builds an interactive Selectable Card for the Payment Method Selecting Panel (no RadioButtons).
     */
    private HBox createPaymentSelectionCard(String icon, String title, String subtitle, Label statusBadge, Runnable onSelect) {
        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setCursor(Cursor.HAND);

        StackPane iconBadge = new StackPane();
        iconBadge.setPrefSize(38, 38);
        iconBadge.setMinSize(38, 38);
        iconBadge.setMaxSize(38, 38);
        iconBadge.setStyle("-fx-background-color: rgba(255, 255, 255, 0.08); -fx-background-radius: 10px;");
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 18px;");
        iconBadge.getChildren().add(iconLbl);

        VBox textCol = new VBox(2);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");
        Label subLbl = new Label(subtitle);
        subLbl.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #cbd5e1;");
        subLbl.setWrapText(true);
        textCol.getChildren().addAll(titleLbl, subLbl);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        card.getChildren().addAll(iconBadge, textCol, statusBadge);
        card.setOnMouseClicked(e -> onSelect.run());
        return card;
    }

    private void applySelectPanelStyle(HBox panel, Label badge, boolean isSelected) {
        if (isSelected) {
            panel.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, rgba(52, 211, 153, 0.22) 0%, rgba(232, 184, 109, 0.18) 100%); "
                    + "-fx-background-radius: 12px; -fx-padding: 10px 14px; "
                    + "-fx-border-color: #4ade80; -fx-border-width: 1.8px; -fx-border-radius: 12px; "
                    + "-fx-effect: dropshadow(gaussian, rgba(74, 222, 128, 0.25), 12, 0.1, 0, 2);");
            badge.setText("✓ SELECTED");
            badge.setStyle("-fx-background-color: rgba(34, 197, 94, 0.28); -fx-text-fill: #4ade80; -fx-font-size: 10px; -fx-font-weight: 900; -fx-padding: 4px 10px; -fx-background-radius: 8px; -fx-border-color: rgba(74, 222, 128, 0.60); -fx-border-radius: 8px;");
        } else {
            panel.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); "
                    + "-fx-background-radius: 12px; -fx-padding: 10px 14px; "
                    + "-fx-border-color: rgba(255, 255, 255, 0.14); -fx-border-width: 1px; -fx-border-radius: 12px;");
            badge.setText("SELECT");
            badge.setStyle("-fx-background-color: rgba(255, 255, 255, 0.08); -fx-text-fill: #94a3b8; -fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 4px 10px; -fx-background-radius: 8px;");
        }
    }

    /**
     * Pops up the Interactive UPI QR Scanner & Payment Checkout Screen for General Customers.
     * After the customer scans/pays on this popup, it creates the booking and navigates to BookingConfirmationView.
     */
    private void openUpiScannerPaymentModal(User user, String userEmail, double baseCharge, double overtimeRate,
                                            String generatedTxnId, TextField mainAccountField,
                                            Runnable syncMainPanels, Label mainErrorLbl) {
        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("modal-overlay");
        overlay.setPadding(new Insets(24));

        VBox modalCard = new VBox(18);
        modalCard.getStyleClass().add("glass-card-highlight");
        modalCard.setMaxWidth(820);
        modalCard.setMaxHeight(Region.USE_PREF_SIZE);
        modalCard.setPadding(new Insets(26, 30, 26, 30));
        modalCard.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, rgba(16, 20, 46, 0.97) 0%, rgba(28, 18, 28, 0.98) 100%); "
                + "-fx-background-radius: 24px; -fx-border-color: rgba(232, 184, 109, 0.60); -fx-border-width: 1.5px; -fx-border-radius: 24px; "
                + "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.75), 40, 0.25, 0, 12);");

        // 1. Top Modal Header with Live Timer
        HBox topHeader = new HBox(12);
        topHeader.setAlignment(Pos.CENTER_LEFT);

        VBox titleCol = new VBox(3);
        Label gatewayBadge = new Label("📷 CRISSCROSS UPI QR SCANNER & PAYMENT GATEWAY");
        gatewayBadge.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #E8B86D;");
        Label modalTitle = new Label("Scan UPI QR Code to Pay Parking Bill");
        modalTitle.setStyle("-fx-font-size: 21px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");
        titleCol.getChildren().addAll(gatewayBadge, modalTitle);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Label timerBadge = new Label("⏱️ Session: 05:00");
        timerBadge.setStyle("-fx-background-color: rgba(241, 110, 104, 0.20); -fx-text-fill: #F2A06A; -fx-font-size: 12px; -fx-font-weight: 800; -fx-padding: 6px 14px; -fx-background-radius: 12px; -fx-border-color: rgba(241, 110, 104, 0.45); -fx-border-radius: 12px;");

        final int[] remainingSeconds = {300};
        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), ev -> {
            if (remainingSeconds[0] > 0) {
                remainingSeconds[0]--;
                int mins = remainingSeconds[0] / 60;
                int secs = remainingSeconds[0] % 60;
                timerBadge.setText(String.format(Locale.US, "⏱️ Session: %02d:%02d", mins, secs));
            }
        }));
        countdown.setCycleCount(Animation.INDEFINITE);
        countdown.play();

        topHeader.getChildren().addAll(titleCol, headerSpacer, timerBadge);

        // 2. Main Split Body: Left = UPI QR Scanner Terminal, Right = Selecting Panel & Bill Summary
        HBox splitBody = new HBox(24);
        splitBody.setAlignment(Pos.TOP_CENTER);

        // -------------------------------------------------------------------------
        // LEFT PANEL: Authentic UPI QR Scanner Terminal
        // -------------------------------------------------------------------------
        VBox scannerTerminal = new VBox(12);
        scannerTerminal.setAlignment(Pos.CENTER);
        scannerTerminal.setPrefWidth(330);
        scannerTerminal.setMinWidth(310);
        scannerTerminal.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-background-radius: 18px; -fx-padding: 18px; -fx-border-color: rgba(232, 184, 109, 0.35); -fx-border-radius: 18px;");

        Label merchantHeader = new Label("MERCHANT: CRISSCROSS VIT PARKING");
        merchantHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: #E8B86D;");

        Label vpaTag = new Label("VPA: crisscross.vit@okaxis  ✓ Verified");
        vpaTag.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #4ade80;");

        // Load the Official Payment UPI QR Code Image for Payment Details
        String slotStr = currentSlot != null ? currentSlot.getSlotNumber() : "SJT-C01";
        String areaStr = activeArea != null ? activeArea.getName() : "SJT PARKING";
        String upiPayload = String.format(Locale.US,
                "upi://pay?pa=crisscross.vit@okaxis&pn=CrissCross%%20Smart%%20Parking&tr=%s&am=%.2f&cu=INR&tn=Slot%%20%s%%20(%s)",
                generatedTxnId, baseCharge, slotStr, activeVehicleNumber);

        Image upiQrImg = null;
        try {
            var qrStream = getClass().getResourceAsStream("/images/payment_upi_qr.jpg");
            if (qrStream != null) {
                upiQrImg = new Image(qrStream);
            }
        } catch (Exception ignored) {}
        if (upiQrImg == null) {
            upiQrImg = QRCodeUtil.generateImage(upiPayload, 200);
        }

        ImageView qrImageView = new ImageView();
        if (upiQrImg != null) {
            qrImageView.setImage(upiQrImg);
            qrImageView.setFitWidth(190);
            qrImageView.setFitHeight(190);
            qrImageView.setPreserveRatio(true);
            qrImageView.setSmooth(true);
        }

        // White QR Card + Animated Laser Line inside Scanner Frame
        StackPane qrScannerFrame = new StackPane();
        qrScannerFrame.setPrefSize(214, 214);
        qrScannerFrame.setMaxSize(214, 214);
        qrScannerFrame.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 16px; -fx-padding: 12px; "
                + "-fx-border-color: #4ade80; -fx-border-width: 2.5px; -fx-border-radius: 16px; "
                + "-fx-effect: dropshadow(gaussian, rgba(74, 222, 128, 0.35), 18, 0.2, 0, 4);");

        Line scanLaser = new Line(0, 0, 182, 0);
        scanLaser.setStyle("-fx-stroke: #22c55e; -fx-stroke-width: 2.5px; -fx-effect: dropshadow(gaussian, #4ade80, 10, 0.8, 0, 0);");
        StackPane.setAlignment(scanLaser, Pos.TOP_CENTER);

        TranslateTransition laserAnim = new TranslateTransition(Duration.millis(1800), scanLaser);
        laserAnim.setFromY(8);
        laserAnim.setToY(184);
        laserAnim.setAutoReverse(true);
        laserAnim.setCycleCount(Animation.INDEFINITE);
        laserAnim.play();

        qrScannerFrame.getChildren().addAll(qrImageView, scanLaser);

        Label amountBigPill = new Label("SCAN & PAY: ₹ " + String.format(Locale.US, "%.2f", baseCharge));
        amountBigPill.setStyle("-fx-background-color: rgba(34, 197, 94, 0.22); -fx-text-fill: #4ade80; -fx-font-size: 14px; -fx-font-weight: 900; -fx-padding: 6px 18px; -fx-background-radius: 12px; -fx-border-color: rgba(74, 222, 128, 0.50); -fx-border-radius: 12px;");

        Label scanHint = new Label("Open any UPI app on your phone & scan above QR");
        scanHint.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #cbd5e1;");

        // Supported UPI App Pills
        HBox appsRow = new HBox(6);
        appsRow.setAlignment(Pos.CENTER);
        appsRow.getChildren().addAll(
                createUpiAppPill("GPay", "#3b82f6"),
                createUpiAppPill("PhonePe", "#a855f7"),
                createUpiAppPill("Paytm", "#06b6d4"),
                createUpiAppPill("BHIM UPI", "#f97316")
        );

        scannerTerminal.getChildren().addAll(merchantHeader, vpaTag, qrScannerFrame, amountBigPill, scanHint, appsRow);

        // -------------------------------------------------------------------------
        // RIGHT PANEL: Bill Summary + Payment Method Selecting Panel
        // -------------------------------------------------------------------------
        VBox rightCol = new VBox(12);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        // Bill Summary Box
        VBox billBox = new VBox(8);
        billBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.28); -fx-background-radius: 14px; -fx-padding: 12px 16px; -fx-border-color: rgba(255, 255, 255, 0.10); -fx-border-radius: 14px;");

        Label billHeader = new Label("🧾 RESERVATION BILL SUMMARY");
        billHeader.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #E8B86D;");

        GridPane billGrid = new GridPane();
        billGrid.setHgap(18);
        billGrid.setVgap(6);
        billGrid.add(createSummaryRow("Allocated Area & Bay", areaStr + " • " + slotStr), 0, 0);
        billGrid.add(createSummaryRow("Vehicle & Plate", activeVehicleType + " (" + activeVehicleNumber + ")"), 1, 0);
        billGrid.add(createSummaryRow("Base 4-Hr Tariff", "₹ " + String.format(Locale.US, "%.2f", baseCharge)), 0, 1);
        billGrid.add(createSummaryRow("Overtime (+12%/hr)", "+₹ " + String.format(Locale.US, "%.2f", overtimeRate) + "/hr"), 1, 1);
        billBox.getChildren().addAll(billHeader, billGrid);

        // Selecting Panel inside the Modal (Synced with Main Screen, NO RadioButtons!)
        Label modalSelectTitle = new Label("Payment Method Selecting Panel:");
        modalSelectTitle.setStyle("-fx-font-size: 11.5px; -fx-font-weight: 800; -fx-text-fill: #E8B86D;");

        VBox modalSelectPanel = new VBox(7);
        HBox[] modalCards = new HBox[3];
        Label[] modalBadges = new Label[3];

        Label modalInputLbl = new Label("Payer UPI ID / Payment Credential:");
        modalInputLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");

        TextField modalAccountField = new TextField(mainAccountField.getText());
        modalAccountField.setPromptText("Enter UPI ID or Payment Credential");

        Runnable syncModalSelection = () -> {
            String[] keys = {"UPI", "CARD", "NET"};
            for (int i = 0; i < 3; i++) {
                boolean active = keys[i].equals(selectedModeKey);
                if (modalCards[i] != null && modalBadges[i] != null) {
                    applySelectPanelStyle(modalCards[i], modalBadges[i], active);
                }
            }
            syncMainPanels.run();
            modalAccountField.setText(mainAccountField.getText());
            if ("UPI".equals(selectedModeKey)) {
                modalInputLbl.setText("Payer UPI ID / VPA (or Scan QR on Left):");
            } else if ("CARD".equals(selectedModeKey)) {
                modalInputLbl.setText("Card Number (Visa / MasterCard / RuPay):");
            } else {
                modalInputLbl.setText("Net Banking / Campus FastTag ID:");
            }
        };

        modalBadges[0] = new Label();
        modalCards[0] = createPaymentSelectionCard(
                "📱",
                "Scan UPI QR Code / UPI Instant Pay",
                "Scan the QR code on the left with GPay, PhonePe, Paytm or BHIM",
                modalBadges[0],
                () -> {
                    selectedModeKey = "UPI";
                    syncModalSelection.run();
                }
        );

        modalBadges[1] = new Label();
        modalCards[1] = createPaymentSelectionCard(
                "💳",
                "Credit / Debit Card",
                "Pay via Visa, MasterCard, RuPay or Campus Smart Card",
                modalBadges[1],
                () -> {
                    selectedModeKey = "CARD";
                    syncModalSelection.run();
                }
        );

        modalBadges[2] = new Label();
        modalCards[2] = createPaymentSelectionCard(
                "🏦",
                "Net Banking / Campus FastTag",
                "Pay via Institutional Net Banking or Vehicle RFID FastTag",
                modalBadges[2],
                () -> {
                    selectedModeKey = "NET";
                    syncModalSelection.run();
                }
        );

        syncModalSelection.run();
        modalSelectPanel.getChildren().addAll(modalCards[0], modalCards[1], modalCards[2]);

        VBox modalCredBox = new VBox(4, modalInputLbl, modalAccountField);

        Label modalErrorLbl = new Label();
        modalErrorLbl.setStyle("-fx-text-fill: #f87171; -fx-font-size: 12px; -fx-font-weight: bold;");
        modalErrorLbl.setVisible(false);

        rightCol.getChildren().addAll(billBox, modalSelectTitle, modalSelectPanel, modalCredBox, modalErrorLbl);

        splitBody.getChildren().addAll(scannerTerminal, rightCol);

        // 3. Bottom Action Buttons of the UPI Scanner Modal
        HBox modalActions = new HBox(14);
        modalActions.setAlignment(Pos.CENTER_RIGHT);

        Button backToReviewBtn = new Button("← Back to Review");
        backToReviewBtn.getStyleClass().add("button-secondary");
        backToReviewBtn.setOnAction(ev -> {
            countdown.stop();
            laserAnim.stop();
            mainAccountField.setText(modalAccountField.getText());
            screenManager.closeCustomModal(overlay);
        });

        Button completePayBtn = new Button("✅ Scan Complete — Pay ₹ " + String.format(Locale.US, "%.2f", baseCharge) + " & Confirm Reservation");
        completePayBtn.getStyleClass().add("button-primary");
        completePayBtn.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 800; -fx-padding: 12px 24px;");
        completePayBtn.setOnAction(ev -> {
            String enteredAcct = modalAccountField.getText() != null ? modalAccountField.getText().trim() : "";
            if (enteredAcct.isEmpty()) {
                enteredAcct = "UPI-QR-SCANNER-VERIFIED";
            }
            mainAccountField.setText(enteredAcct);
            countdown.stop();
            laserAnim.stop();
            screenManager.closeCustomModal(overlay);
            completeReservationAndShowConfirmation(user, userEmail, false, baseCharge, generatedTxnId, enteredAcct, mainErrorLbl);
        });

        modalActions.getChildren().addAll(backToReviewBtn, completePayBtn);

        modalCard.getChildren().addAll(topHeader, splitBody, modalActions);
        overlay.getChildren().add(modalCard);

        screenManager.showCustomModal(overlay);
    }

    private Label createUpiAppPill(String name, String borderColor) {
        Label pill = new Label(name);
        pill.setStyle("-fx-background-color: rgba(255, 255, 255, 0.08); -fx-text-fill: #f8fafc; -fx-font-size: 10px; -fx-font-weight: 800; -fx-padding: 3px 9px; -fx-background-radius: 8px; -fx-border-color: " + borderColor + "; -fx-border-radius: 8px;");
        return pill;
    }

    private void completeReservationAndShowConfirmation(User user, String userEmail, boolean isVit,
                                                        double baseCharge, String generatedTxnId,
                                                        String payerAccount, Label errorLbl) {
        try {
            int uid = user != null ? user.getUserId() : 2;
            String uname = user != null ? user.getFullName() : "Alex Reynolds";

            String paymentNotes;
            if (isVit) {
                paymentNotes = "FREE VIT WAIVER | Method: VIT Institutional Waiver | TxnID: " + generatedTxnId
                        + " | Account: " + userEmail + " | Plan: VIT Free Event Pass (₹0.00)";
            } else {
                paymentNotes = "PAID | Method: " + selectedPaymentMethodTitle
                        + " | TxnID: " + generatedTxnId
                        + " | Account: " + payerAccount
                        + " | Plan: Visitor 4-Hour Standard Pass (₹" + String.format(Locale.US, "%.2f", baseCharge) + ")";
            }

            Reservation res = BookingService.getInstance().createBooking(
                    uid,
                    uname,
                    activeEvent != null ? activeEvent.getEventId() : 1,
                    activeEvent != null ? activeEvent.getTitle() : "Campus Event",
                    activeEvent != null ? activeEvent.getVenue() : "SJT Auditorium",
                    activeArea != null ? activeArea.getAreaId() : 1,
                    activeArea != null ? activeArea.getName() : "SJT PARKING",
                    currentSlot != null ? currentSlot.getSlotId() : 1,
                    currentSlot != null ? currentSlot.getSlotNumber() : "SJT-C01",
                    activeVehicleType,
                    activeVehicleNumber,
                    4,
                    baseCharge,
                    paymentNotes
            );

            screenManager.showBookingConfirmation(res);
        } catch (Exception ex) {
            errorLbl.setText(ex.getMessage());
            errorLbl.setVisible(true);
        }
    }

    private VBox buildParkingMapLocatorCard() {
        VBox mapCard = new VBox(16);
        mapCard.getStyleClass().add("glass-card");
        mapCard.setPadding(new Insets(24, 26, 24, 26));
        mapCard.setMaxWidth(Double.MAX_VALUE);

        // 1. Top Badge & Legend Row
        HBox topBar = new HBox(12);
        topBar.setAlignment(Pos.CENTER_LEFT);

        Label mapBadge = new Label("📍 PARKING MAP • SLOT LOCATOR");
        mapBadge.setStyle("-fx-background-color: rgba(241, 110, 104, 0.18); -fx-text-fill: #F2A06A; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5px 14px; -fx-background-radius: 12px; -fx-border-color: rgba(241, 110, 104, 0.40); -fx-border-radius: 12px;");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox legend = new HBox(12);
        legend.setAlignment(Pos.CENTER_RIGHT);
        legend.getChildren().addAll(
                createLegendDot("Your Slot", "#F16E68"),
                createLegendDot("Available", "#f8fafc"),
                createLegendDot("Occupied", "#dc2626"),
                createLegendDot("Reserved", "#d97706")
        );

        topBar.getChildren().addAll(mapBadge, topSpacer, legend);

        // 2. Title & Subtitle
        VBox titleBox = new VBox(4);
        String areaTitle = activeArea != null ? activeArea.getName() : "SJT PARKING";
        Label mapTitle = new Label("2D Architectural Blueprint — " + areaTitle);
        mapTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

        mapSubtitleLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        titleBox.getChildren().addAll(mapTitle, mapSubtitleLbl);

        // 3. Highlighted Slot Location Callout Banner
        HBox locatorBanner = new HBox(14);
        locatorBanner.setAlignment(Pos.CENTER_LEFT);
        locatorBanner.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, rgba(232, 184, 109, 0.14) 0%, rgba(241, 110, 104, 0.16) 100%); "
                + "-fx-background-radius: 14px; -fx-padding: 12px 16px; "
                + "-fx-border-color: rgba(232, 184, 109, 0.45); -fx-border-radius: 14px;");

        Label pinIcon = new Label("🎯");
        pinIcon.setStyle("-fx-font-size: 22px;");

        VBox locatorTextCol = new VBox(3);
        locatorTargetLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #E8B86D;");
        locatorSubLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #E2E8F0;");
        locatorSubLbl.setWrapText(true);
        locatorTextCol.getChildren().addAll(locatorTargetLbl, locatorSubLbl);
        HBox.setHgrow(locatorTextCol, Priority.ALWAYS);

        locatorBanner.getChildren().addAll(pinIcon, locatorTextCol);

        // 4. 2D Architectural Floor Plan Board
        floorplanContainer.getStyleClass().add("floorplan-board");
        floorplanContainer.setAlignment(Pos.CENTER);
        floorplanContainer.setPadding(new Insets(18, 20, 18, 20));
        VBox.setVgrow(floorplanContainer, Priority.ALWAYS);

        renderBlueprint();

        // 5. Bottom Navigation Hint & Full Map Button
        HBox footerRow = new HBox(12);
        footerRow.setAlignment(Pos.CENTER_LEFT);

        String code = activeArea != null ? activeArea.getCode() : "SJT";
        Label navHint = new Label("🧭 Gate Entry: Follow Sector " + code + " digital signage • Click any white bay above to change your slot");
        navHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        Button fullMapBtn = new Button("Open Full Parking Map ↗");
        fullMapBtn.getStyleClass().add("button-secondary");
        fullMapBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6px 14px;");
        fullMapBtn.setOnAction(e -> screenManager.showParkingMapView());

        footerRow.getChildren().addAll(navHint, footerSpacer, fullMapBtn);

        mapCard.getChildren().addAll(topBar, titleBox, locatorBanner, floorplanContainer, footerRow);
        return mapCard;
    }

    private void renderBlueprint() {
        floorplanContainer.getChildren().clear();

        if (activeArea == null) return;

        List<ParkingSlot> slots = slotDAO.findByAreaAndType(activeArea.getAreaId(), activeVehicleType);
        if (slots.isEmpty()) {
            slots = slotDAO.findByArea(activeArea.getAreaId());
        }

        // Blueprint internal header strip
        HBox planHeader = new HBox(10);
        planHeader.setAlignment(Pos.CENTER_LEFT);

        String icon = "BIKE".equalsIgnoreCase(activeVehicleType) ? "🏍️ " : "🚗 ";
        Label sectorTitle = new Label(icon + "SECTOR " + activeArea.getCode() + " • " + activeVehicleType + " BAYS FLOOR PLAN");
        sectorTitle.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #CBD5E1;");

        Region hSpacer = new Region();
        HBox.setHgrow(hSpacer, Priority.ALWAYS);

        Label venueTag = new Label("📍 Nearest Venue: " + activeArea.getNearestVenue() + " (" + activeArea.getWalkingTimeMins() + " min walk)");
        venueTag.setStyle("-fx-font-size: 11px; -fx-text-fill: #F2A06A; -fx-font-weight: bold;");

        planHeader.getChildren().addAll(sectorTitle, hSpacer, venueTag);
        floorplanContainer.getChildren().add(planHeader);

        // Partition slots into 3 balanced architectural rows (e.g. 5 per row for 15 slots)
        int total = slots.size();
        int perRow = Math.max(1, (int) Math.ceil(total / 3.0));
        int row1Count = Math.min(perRow, total);
        int row2Count = Math.min(perRow, Math.max(0, total - row1Count));

        List<ParkingSlot> row1 = new ArrayList<>(slots.subList(0, row1Count));
        List<ParkingSlot> row2 = new ArrayList<>(slots.subList(row1Count, row1Count + row2Count));
        List<ParkingSlot> row3 = new ArrayList<>(slots.subList(row1Count + row2Count, total));

        // Determine which row & position the current selected slot is in
        String selectedRowName = "ROW 1 (NORTH WING)";
        String selectedLaneName = "Ingress Roadway (Lane 01)";
        int bayPositionInRow = 1;

        for (int i = 0; i < row1.size(); i++) {
            if (isSameSlot(row1.get(i), currentSlot)) {
                selectedRowName = "ROW 1 (NORTH WING)";
                selectedLaneName = "Ingress Roadway (Lane 01)";
                bayPositionInRow = i + 1;
            }
        }
        for (int i = 0; i < row2.size(); i++) {
            if (isSameSlot(row2.get(i), currentSlot)) {
                selectedRowName = "ROW 2 (CENTER AISLE)";
                selectedLaneName = "Ingress Lane 01 / Egress Lane 02";
                bayPositionInRow = i + 1;
            }
        }
        for (int i = 0; i < row3.size(); i++) {
            if (isSameSlot(row3.get(i), currentSlot)) {
                selectedRowName = "ROW 3 (SOUTH WING)";
                selectedLaneName = "Egress Roadway (Lane 02)";
                bayPositionInRow = i + 1;
            }
        }

        // Update callout banner & subtitle
        String slotNumStr = currentSlot != null ? currentSlot.getSlotNumber() : "SJT-C01";
        String floorStr = currentSlot != null ? currentSlot.getFloorLevel() : "Ground Level";
        mapSubtitleLbl.setText("Highlighting your designated slot (" + slotNumStr + ") in " + activeArea.getName() + " • Click any available white bay to switch");
        locatorTargetLbl.setText("YOUR PARKING SLOT: " + slotNumStr + "  •  " + selectedRowName + " (Bay #" + bayPositionInRow + ")");
        locatorSubLbl.setText("Zone Level: " + floorStr + "  •  Approach via " + selectedLaneName + "  •  "
                + activeArea.getWalkingTimeMins() + " min walk to " + activeArea.getNearestVenue());

        // Row 1: Top Parking Bays
        floorplanContainer.getChildren().add(buildAisleRow("ROW 1", "NORTH", row1));

        // Driving Aisle 1 (Ingress)
        floorplanContainer.getChildren().add(buildDrivingLane("▲ INGRESS ROADWAY (LANE 01) ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ──"));

        // Row 2: Center Parking Bays
        if (!row2.isEmpty()) {
            floorplanContainer.getChildren().add(buildAisleRow("ROW 2", "CENTER", row2));
        }

        // Driving Aisle 2 (Egress)
        floorplanContainer.getChildren().add(buildDrivingLane("▼ EGRESS ROADWAY (LANE 02) ── ── ── ── ── ── ── ── ── ── ── ── ── ── ── ──"));

        // Row 3: Bottom Parking Bays
        if (!row3.isEmpty()) {
            floorplanContainer.getChildren().add(buildAisleRow("ROW 3", "SOUTH", row3));
        }
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

    private HBox buildAisleRow(String rowTitle, String wingSub, List<ParkingSlot> slotList) {
        HBox rowContainer = new HBox(14);
        rowContainer.setAlignment(Pos.CENTER);

        // Row marker on the left
        VBox tagBox = new VBox(1);
        tagBox.setAlignment(Pos.CENTER);
        tagBox.setPrefWidth(56);
        Label tag = new Label(rowTitle);
        tag.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #E8B86D;");
        Label subTag = new Label(wingSub);
        subTag.setStyle("-fx-font-size: 8px; -fx-font-weight: 700; -fx-text-fill: #94A3B8;");
        tagBox.getChildren().addAll(tag, subTag);
        rowContainer.getChildren().add(tagBox);

        // Cluster slots separated by architectural pillar markers
        HBox clustersBox = new HBox(10);
        clustersBox.setAlignment(Pos.CENTER);

        for (int i = 0; i < slotList.size(); i++) {
            ParkingSlot s = slotList.get(i);
            Button spotBtn = createSpotButton(s);
            clustersBox.getChildren().add(spotBtn);

            if ((i + 1) % 2 == 0 && (i + 1) < slotList.size()) {
                VBox pillar = new VBox();
                pillar.setPrefWidth(10);
                pillar.setMinHeight(78);
                pillar.setMaxHeight(78);
                pillar.setAlignment(Pos.CENTER);
                Region pillarCore = new Region();
                pillarCore.setStyle("-fx-background-color: rgba(255, 255, 255, 0.14); -fx-background-radius: 3px; -fx-min-width: 5px; -fx-max-width: 5px; -fx-min-height: 46px; -fx-max-height: 46px;");
                pillar.getChildren().add(pillarCore);
                clustersBox.getChildren().add(pillar);
            }
        }

        rowContainer.getChildren().add(clustersBox);
        return rowContainer;
    }

    private Button createSpotButton(ParkingSlot slotItem) {
        Button btn = new Button();
        btn.getStyleClass().add("floorplan-spot");
        btn.setMinWidth(58);
        btn.setPrefWidth(58);
        btn.setMaxWidth(58);

        String rawNum = slotItem.getSlotNumber();
        String displayNum = rawNum.contains("-") ? rawNum.substring(rawNum.lastIndexOf('-') + 1) : rawNum;

        boolean isCurrentSelected = isSameSlot(slotItem, currentSlot);

        VBox bayContent = new VBox(1);
        bayContent.setAlignment(Pos.CENTER);

        if (isCurrentSelected) {
            Label youTag = new Label("📍 YOURS");
            youTag.setStyle("-fx-font-size: 8px; -fx-font-weight: 900; -fx-text-fill: #ffffff;");

            Label pLbl = new Label("P");
            pLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: 900; -fx-text-fill: #ffffff;");

            Label numLbl = new Label(displayNum);
            numLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #ffffff;");

            bayContent.getChildren().addAll(youTag, pLbl, numLbl);
            btn.setGraphic(bayContent);
            btn.getStyleClass().add("floorplan-spot-selected");

            ScaleTransition st = new ScaleTransition(Duration.millis(140), btn);
            st.setToX(1.06);
            st.setToY(1.06);
            st.setAutoReverse(true);
            st.setCycleCount(2);
            st.play();
        } else {
            Label pLbl = new Label("P");
            pLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: 900; -fx-font-family: 'Segoe UI', Arial, sans-serif;");

            Label numLbl = new Label(displayNum);
            numLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: bold;");

            bayContent.getChildren().addAll(pLbl, numLbl);
            btn.setGraphic(bayContent);

            if (slotItem.isAvailable()) {
                btn.getStyleClass().add("floorplan-spot-available");
                pLbl.setStyle(pLbl.getStyle() + "-fx-text-fill: #0F172A;");
                numLbl.setStyle(numLbl.getStyle() + "-fx-text-fill: #1E293B;");

                btn.setOnAction(e -> {
                    this.currentSlot = slotItem;
                    screenManager.setSelectedParkingSlot(slotItem);
                    if (reservedSlotValueLbl != null) {
                        reservedSlotValueLbl.setText(slotItem.getSlotNumber());
                    }
                    if (vehicleClassValueLbl != null) {
                        vehicleClassValueLbl.setText(activeVehicleType + " (" + slotItem.getFloorLevel() + ")");
                    }
                    renderBlueprint();
                });
            } else if ("OCCUPIED".equalsIgnoreCase(slotItem.getStatus())) {
                btn.getStyleClass().add("floorplan-spot-occupied");
                pLbl.setStyle(pLbl.getStyle() + "-fx-text-fill: #ffffff;");
                numLbl.setStyle(numLbl.getStyle() + "-fx-text-fill: #fecaca;");
            } else if ("RESERVED".equalsIgnoreCase(slotItem.getStatus())) {
                btn.getStyleClass().add("floorplan-spot-reserved");
                pLbl.setStyle(pLbl.getStyle() + "-fx-text-fill: #ffffff;");
                numLbl.setStyle(numLbl.getStyle() + "-fx-text-fill: #fef3c7;");
            } else {
                btn.getStyleClass().add("floorplan-spot-blocked");
                pLbl.setStyle(pLbl.getStyle() + "-fx-text-fill: #94a3b8;");
                numLbl.setStyle(numLbl.getStyle() + "-fx-text-fill: #64748b;");
            }
        }

        return btn;
    }

    private boolean isSameSlot(ParkingSlot a, ParkingSlot b) {
        if (a == null || b == null) return false;
        if (a.getSlotId() == b.getSlotId()) return true;
        return a.getSlotNumber() != null && a.getSlotNumber().equalsIgnoreCase(b.getSlotNumber());
    }

    private HBox createLegendDot(String text, String fillColor) {
        HBox dotBox = new HBox(5);
        dotBox.setAlignment(Pos.CENTER);

        Region dot = new Region();
        dot.setStyle("-fx-background-color: " + fillColor + "; -fx-background-radius: 4px; -fx-min-width: 11px; -fx-max-width: 11px; -fx-min-height: 11px; -fx-max-height: 11px; -fx-border-color: rgba(255,255,255,0.45); -fx-border-radius: 4px;");

        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #CBD5E1; -fx-font-weight: 600;");

        dotBox.getChildren().addAll(dot, lbl);
        return dotBox;
    }

    private VBox createSummaryRow(String title, String val) {
        VBox box = new VBox(2);
        Label t = new Label(title);
        t.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");
        Label v = new Label(val);
        v.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        box.getChildren().addAll(t, v);
        return box;
    }
}
