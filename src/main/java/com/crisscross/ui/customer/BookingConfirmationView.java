package com.crisscross.ui.customer;

import com.crisscross.models.Reservation;
import com.crisscross.models.User;
import com.crisscross.services.AuthService;
import com.crisscross.services.ParkingPricingService;
import com.crisscross.ui.ScreenManager;
import com.crisscross.util.PassReceiptGenerator;
import com.crisscross.util.QRCodeUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.File;

public class BookingConfirmationView extends VBox {

    public BookingConfirmationView(ScreenManager screenManager, Reservation reservation) {
        this.setSpacing(24);
        this.setFillWidth(true);
        this.setAlignment(Pos.CENTER);

        User currentUser = AuthService.getInstance().getCurrentUser();
        String userEmail = currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail() : "";
        boolean isVit = ParkingPricingService.isVitEmail(userEmail);

        // Glass Digital Pass Card
        VBox passCard = new VBox(20);
        passCard.getStyleClass().add("glass-card-highlight");
        passCard.setMaxWidth(720);
        passCard.setAlignment(Pos.CENTER);
        passCard.setPadding(new Insets(32, 36, 32, 36));

        Label successIcon = new Label("🎉");
        successIcon.setStyle("-fx-font-size: 44px;");

        Label congratsTitle = new Label("Reservation Confirmed!");
        congratsTitle.getStyleClass().add("heading-hero");
        congratsTitle.setStyle("-fx-font-size: 28px; -fx-text-fill: #4ade80; -fx-font-weight: 800;");

        Label sub = new Label("Your digital campus event parking pass is ready for check-in.");
        sub.setStyle("-fx-font-size: 14px; -fx-text-fill: #cbd5e1;");

        // Digital Ticket Strip
        VBox ticketBox = new VBox(16);
        ticketBox.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, rgba(255, 255, 255, 0.06) 0%, rgba(35, 25, 20, 0.35) 50%, rgba(18, 12, 10, 0.50) 100%); "
                + "-fx-background-radius: 20px; "
                + "-fx-border-color: rgba(232, 184, 109, 0.45) rgba(242, 160, 106, 0.35) rgba(241, 110, 104, 0.25) rgba(232, 184, 109, 0.45); "
                + "-fx-border-radius: 20px; -fx-border-width: 1px; -fx-padding: 24px; "
                + "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.35), 20, 0.1, 0, 8);");

        HBox ticketHeader = new HBox(12);
        ticketHeader.setAlignment(Pos.CENTER_LEFT);

        Label brand = new Label("CrissCross Digital Parking Pass");
        brand.setStyle("-fx-font-weight: 800; -fx-font-size: 14px; -fx-text-fill: #E8B86D;");

        Region ticketSpacer = new Region();
        HBox.setHgrow(ticketSpacer, Priority.ALWAYS);

        String bookingCode = reservation != null ? reservation.getBookingCode() : "CRISS-2026-X892";
        Label codeBadge = new Label(bookingCode);
        codeBadge.setStyle("-fx-background-color: rgba(232, 184, 109, 0.20); -fx-text-fill: #E8B86D; -fx-font-size: 13px; -fx-font-weight: 800; -fx-padding: 5px 12px; -fx-background-radius: 12px; -fx-border-color: rgba(232, 184, 109, 0.40); -fx-border-radius: 12px;");

        ticketHeader.getChildren().addAll(brand, ticketSpacer, codeBadge);

        // Body Row: Authentic Scannable QR Code + Details Grid
        HBox bodyRow = new HBox(24);
        bodyRow.setAlignment(Pos.CENTER_LEFT);

        VBox qrBox = createAuthenticQR(reservation);

        GridPane detailsGrid = new GridPane();
        detailsGrid.setHgap(22);
        detailsGrid.setVgap(12);

        String eventTitle = reservation != null ? reservation.getEventTitle() : "Campus Event";
        String venue = reservation != null ? reservation.getEventVenue() : "Main Quad";
        String areaName = reservation != null ? reservation.getAreaName() : "SJT PARKING";
        String slotNum = reservation != null ? reservation.getSlotNumber() : "SJT-C01";
        String vType = reservation != null ? reservation.getVehicleType() : "CAR";
        String vPlate = reservation != null ? reservation.getVehicleNumber() : "TN-22-CR-1024";

        double totalAmount = reservation != null ? reservation.getTotalAmount() : (isVit ? 0.00 : 50.00);
        boolean isPaidBooking = totalAmount > 0.0;
        String feeString = !isPaidBooking ? "₹ 0.00 (VIT Waiver)" : ("₹ " + String.format("%.2f", totalAmount));
        String planString = !isPaidBooking ? "Complimentary Pass" : "4-Hr Visitor Pass";

        detailsGrid.add(createTicketField("Event", eventTitle), 0, 0);
        detailsGrid.add(createTicketField("Venue", venue), 1, 0);
        detailsGrid.add(createTicketField("Parking Zone", areaName), 0, 1);
        detailsGrid.add(createTicketField("Reserved Slot", slotNum), 1, 1);
        detailsGrid.add(createTicketField("Vehicle Type", vType), 0, 2);
        detailsGrid.add(createTicketField("Plate Number", vPlate), 1, 2);
        detailsGrid.add(createTicketField("Tariff Plan", planString), 0, 3);
        detailsGrid.add(createTicketField("Total Paid", feeString), 1, 3);

        bodyRow.getChildren().addAll(qrBox, detailsGrid);

        // Payment & Transaction Details Strip
        VBox paymentDetailsBox = new VBox(10);
        paymentDetailsBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-background-radius: 14px; -fx-padding: 14px 18px; -fx-border-color: rgba(232, 184, 109, 0.35); -fx-border-radius: 14px;");

        HBox payTitleRow = new HBox(10);
        payTitleRow.setAlignment(Pos.CENTER_LEFT);
        Label paySectionTitle = new Label("💳 Payment & Billing Receipt Details");
        paySectionTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #E8B86D;");
        Region paySp = new Region();
        HBox.setHgrow(paySp, Priority.ALWAYS);
        String payStatusStr = reservation != null ? reservation.getPaymentStatus() : (isPaidBooking ? "PAID & VERIFIED" : "WAIVED (100% FREE VIT PASS)");
        Label payStatusPill = new Label(payStatusStr);
        payStatusPill.setStyle("-fx-background-color: rgba(34, 197, 94, 0.22); -fx-text-fill: #4ade80; -fx-font-size: 10px; -fx-font-weight: 800; -fx-padding: 3px 10px; -fx-background-radius: 10px; -fx-border-color: rgba(74, 222, 128, 0.45); -fx-border-radius: 10px;");
        payTitleRow.getChildren().addAll(paySectionTitle, paySp, payStatusPill);

        GridPane payGrid = new GridPane();
        payGrid.setHgap(24);
        payGrid.setVgap(10);
        String payMethodStr = reservation != null ? reservation.getPaymentMethod() : (isPaidBooking ? "UPI Instant Pay" : "VIT Institutional Waiver");
        String txnIdStr = reservation != null ? reservation.getTransactionId() : "TXN-CRISS-2026-8492";
        String payerAcctStr = reservation != null ? reservation.getPayerAccountDetail() : (userEmail.isEmpty() ? "Verified Visitor" : userEmail);
        double overtimeRate = ParkingPricingService.getOvertimeHourlyRate(isPaidBooking ? "visitor@gmail.com" : "user@vit.ac.in", vType);

        payGrid.add(createTicketField("Payment Mode", payMethodStr), 0, 0);
        payGrid.add(createTicketField("Transaction Reference ID", txnIdStr), 1, 0);
        payGrid.add(createTicketField("Billing / Payer Account", payerAcctStr), 0, 1);
        payGrid.add(createTicketField("Overtime Policy (After 4 Hrs)", isPaidBooking ? ("+₹ " + String.format("%.2f", overtimeRate) + "/hr (12%)") : "₹ 0.00 (Waived)"), 1, 1);

        paymentDetailsBox.getChildren().addAll(payTitleRow, payGrid);

        // Policy / Scanning guidance banner inside pass
        HBox scanInfoBox = new HBox(10);
        scanInfoBox.setAlignment(Pos.CENTER_LEFT);
        scanInfoBox.setStyle("-fx-background-color: rgba(0, 0, 0, 0.25); -fx-background-radius: 10px; -fx-padding: 10px 14px;");

        Label gateScanHint = new Label("📷 Authentic QR Pass: Present this code at the barrier scanner. Valid for 4 hours; 12% surcharge applies for extended hours.");
        gateScanHint.setStyle("-fx-font-size: 11px; -fx-text-fill: #CBD5E1;");
        gateScanHint.setWrapText(true);
        scanInfoBox.getChildren().add(gateScanHint);

        ticketBox.getChildren().addAll(ticketHeader, bodyRow, paymentDetailsBox, scanInfoBox);

        // Action Buttons
        HBox actionBox = new HBox(14);
        actionBox.setAlignment(Pos.CENTER);

        Button myBookingsBtn = new Button("View in My Reservations");
        myBookingsBtn.getStyleClass().add("button-primary");
        myBookingsBtn.setOnAction(e -> screenManager.showMyReservations());

        Button downloadPassBtn = new Button("Download PDF Pass Receipt 📥");
        downloadPassBtn.getStyleClass().add("button-secondary");
        downloadPassBtn.setStyle("-fx-font-weight: bold; -fx-border-color: #E8B86D; -fx-text-fill: #E8B86D;");
        downloadPassBtn.setOnAction(e -> {
            try {
                File savedFile = PassReceiptGenerator.downloadAndOpenReceipt(getScene().getWindow(), reservation, currentUser);
                if (savedFile != null) {
                    screenManager.showConfirmationDialog(
                            "PDF Receipt Downloaded Successfully! 📥",
                            "Your official PDF parking pass & payment receipt has been saved to:\n\n"
                                    + savedFile.getAbsolutePath()
                                    + "\n\nYour PDF viewer and File Explorer have opened so you can view and print your pass.",
                            () -> {}
                    );
                }
            } catch (Exception ex) {
                screenManager.showConfirmationDialog("Download Notice", "Could not complete PDF download: " + ex.getMessage(), () -> {});
            }
        });

        Button dashBtn = new Button("Back to Dashboard");
        dashBtn.getStyleClass().add("button-secondary");
        dashBtn.setOnAction(e -> screenManager.showCustomerDashboard());

        actionBox.getChildren().addAll(myBookingsBtn, downloadPassBtn, dashBtn);

        passCard.getChildren().addAll(successIcon, congratsTitle, sub, ticketBox, actionBox);
        this.getChildren().add(passCard);
    }

    private VBox createAuthenticQR(Reservation reservation) {
        VBox qr = new VBox(6);
        qr.setAlignment(Pos.CENTER);
        qr.setPrefSize(126, 136);
        qr.setMinSize(126, 136);
        qr.setMaxSize(126, 136);
        qr.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 14px; -fx-padding: 8px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 10, 0.15, 0, 4);");

        String payload = QRCodeUtil.buildPayload(reservation);
        Image qrImage = QRCodeUtil.generateImage(payload, 120);

        ImageView qrView = new ImageView();
        if (qrImage != null) {
            qrView.setImage(qrImage);
            qrView.setFitWidth(104);
            qrView.setFitHeight(104);
            qrView.setPreserveRatio(true);
            qrView.setSmooth(false);
        }

        Label scanLbl = new Label("SCAN AT GATE");
        scanLbl.setStyle("-fx-font-size: 9px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");

        qr.getChildren().addAll(qrView, scanLbl);
        return qr;
    }

    private VBox createTicketField(String label, String value) {
        VBox b = new VBox(2);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        b.getChildren().addAll(l, v);
        return b;
    }
}
