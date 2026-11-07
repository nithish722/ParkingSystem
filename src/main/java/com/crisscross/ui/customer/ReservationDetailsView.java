package com.crisscross.ui.customer;

import com.crisscross.models.Reservation;
import com.crisscross.models.User;
import com.crisscross.services.AuthService;
import com.crisscross.services.BookingService;
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

public class ReservationDetailsView extends VBox {

    public ReservationDetailsView(ScreenManager screenManager, Reservation reservation) {
        this.setSpacing(24);
        this.setFillWidth(true);

        User currentUser = AuthService.getInstance().getCurrentUser();
        String userEmail = currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail() : "";
        boolean isVit = ParkingPricingService.isVitEmail(userEmail);
        String vehicleType = reservation != null ? reservation.getVehicleType() : "CAR";
        double totalPaid = reservation != null ? reservation.getTotalAmount() : (isVit ? 0.00 : 50.00);

        Button backBtn = new Button("← Back to My Reservations");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setOnAction(e -> screenManager.showMyReservations());

        VBox card = new VBox(22);
        card.getStyleClass().add("glass-card");
        card.setMaxWidth(760);
        card.setPadding(new Insets(28));

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Reservation Details & Gate Pass");
        title.getStyleClass().add("heading-1");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusBadge = new Label(reservation != null ? reservation.getStatus() : "CONFIRMED");
        if (reservation != null && "CANCELLED".equalsIgnoreCase(reservation.getStatus())) {
            statusBadge.getStyleClass().add("badge-occupied");
        } else {
            statusBadge.getStyleClass().add("badge-available");
        }
        topRow.getChildren().addAll(title, spacer, statusBadge);

        // QR Code & Details Split Row
        HBox contentRow = new HBox(24);
        contentRow.setAlignment(Pos.CENTER_LEFT);

        // QR Code card
        VBox qrBox = new VBox(6);
        qrBox.setAlignment(Pos.CENTER);
        qrBox.setPrefSize(134, 144);
        qrBox.setMinSize(134, 144);
        qrBox.setMaxSize(134, 144);
        qrBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 14px; -fx-padding: 10px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 10, 0.15, 0, 4);");

        String payload = QRCodeUtil.buildPayload(reservation);
        Image qrImage = QRCodeUtil.generateImage(payload, 120);
        ImageView qrView = new ImageView();
        if (qrImage != null) {
            qrView.setImage(qrImage);
            qrView.setFitWidth(108);
            qrView.setFitHeight(108);
            qrView.setPreserveRatio(true);
            qrView.setSmooth(false);
        }

        Label scanLbl = new Label("SCAN AT GATE");
        scanLbl.setStyle("-fx-font-size: 9px; -fx-font-weight: 900; -fx-text-fill: #0f172a;");
        qrBox.getChildren().addAll(qrView, scanLbl);

        // Details Block
        GridPane grid = new GridPane();
        grid.setHgap(28);
        grid.setVgap(14);
        HBox.setHgrow(grid, Priority.ALWAYS);
        grid.setStyle("-fx-background-color: rgba(255, 255, 255, 0.03); -fx-background-radius: 16px; -fx-padding: 18px 22px;");

        grid.add(createField("Booking Reference Code", reservation != null ? reservation.getBookingCode() : "CRISS-2026-X892"), 0, 0);
        grid.add(createField("Campus Event", reservation != null ? reservation.getEventTitle() : "Campus Event"), 1, 0);

        grid.add(createField("Assigned Venue", reservation != null ? reservation.getEventVenue() : "Main Quad"), 0, 1);
        grid.add(createField("Parking Area Zone", reservation != null ? reservation.getAreaName() : "SJT PARKING"), 1, 1);

        grid.add(createField("Designated Slot", reservation != null ? reservation.getSlotNumber() : "SJT-C01"), 0, 2);
        grid.add(createField("Vehicle", (reservation != null ? reservation.getVehicleType() : "CAR") + " - " + (reservation != null ? reservation.getVehicleNumber() : "TN-22-CR-1024")), 1, 2);

        boolean isPaid = totalPaid > 0.0;
        grid.add(createField("Tariff & Amount Billed", !isPaid ? "₹ 0.00 (100% Free VIT Waiver)" : ("₹ " + String.format("%.2f", totalPaid) + " Visitor Standard")), 0, 3);
        grid.add(createField("Base Duration", "4 Hours Event Parking"), 1, 3);

        grid.add(createField("Payment Status", reservation != null ? reservation.getPaymentStatus() : (isPaid ? "PAID & VERIFIED" : "WAIVED (FREE)")), 0, 4);
        grid.add(createField("Payment Mode", reservation != null ? reservation.getPaymentMethod() : (isPaid ? "UPI Instant Pay" : "VIT Institutional Waiver")), 1, 4);

        grid.add(createField("Transaction Reference ID", reservation != null ? reservation.getTransactionId() : "TXN-CRISS-2026-8492"), 0, 5);
        grid.add(createField("Billing Account", reservation != null ? reservation.getPayerAccountDetail() : (userEmail.isEmpty() ? "Verified Account" : userEmail)), 1, 5);

        contentRow.getChildren().addAll(qrBox, grid);

        // Policy Note
        VBox policyBox = new VBox(4);
        policyBox.setStyle("-fx-background-color: rgba(232, 184, 109, 0.08); -fx-background-radius: 12px; -fx-padding: 12px 16px; -fx-border-color: rgba(232, 184, 109, 0.22); -fx-border-radius: 12px;");
        Label polTitle = new Label("⏱️ Pass Validity & Overtime Surcharge Notice");
        polTitle.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #E8B86D;");
        Label polDesc = new Label(ParkingPricingService.getOvertimePolicyDescription(userEmail, vehicleType));
        polDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #CBD5E1;");
        polDesc.setWrapText(true);
        policyBox.getChildren().addAll(polTitle, polDesc);

        card.getChildren().addAll(topRow, contentRow, policyBox);

        // Action Row
        HBox actionBox = new HBox(16);
        actionBox.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel This Reservation");
        cancelBtn.getStyleClass().add("button-danger");
        boolean canCancel = reservation != null && !"CANCELLED".equalsIgnoreCase(reservation.getStatus());
        cancelBtn.setDisable(!canCancel);
        cancelBtn.setOnAction(e -> {
            screenManager.showConfirmationDialog("Cancel Reservation", "Cancel reservation " + reservation.getBookingCode() + "?", () -> {
                try {
                    BookingService.getInstance().cancelBooking(reservation.getReservationId());
                    screenManager.showMyReservations();
                } catch (Exception ex) {
                    System.err.println(ex.getMessage());
                }
            });
        });

        Button printBtn = new Button("Download PDF Pass Receipt 📥");
        printBtn.getStyleClass().add("button-primary");
        printBtn.setOnAction(e -> {
            try {
                File saved = PassReceiptGenerator.downloadAndOpenReceipt(getScene().getWindow(), reservation, currentUser);
                if (saved != null) {
                    screenManager.showConfirmationDialog(
                            "PDF Receipt Downloaded Successfully! 📥",
                            "Your official PDF parking pass & payment receipt has been saved to:\n\n"
                                    + saved.getAbsolutePath()
                                    + "\n\nYour PDF viewer and File Explorer have opened for immediate printing and access.",
                            () -> {}
                    );
                }
            } catch (Exception ex) {
                screenManager.showConfirmationDialog("Download Notice", "Could not complete PDF download: " + ex.getMessage(), () -> {});
            }
        });

        actionBox.getChildren().addAll(printBtn, cancelBtn);
        card.getChildren().add(actionBox);

        this.getChildren().addAll(backBtn, card);
    }

    private VBox createField(String label, String value) {
        VBox b = new VBox(3);
        Label l = new Label(label);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");
        Label v = new Label(value);
        v.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");
        b.getChildren().addAll(l, v);
        return b;
    }
}
