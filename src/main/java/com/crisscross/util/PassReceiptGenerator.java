package com.crisscross.util;

import com.crisscross.models.Reservation;
import com.crisscross.models.User;
import com.crisscross.services.ParkingPricingService;
import com.crisscross.services.ReportService;
import io.nayuki.qrcodegen.QrCode;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generates official, scannable digital parking pass receipts in native Adobe PDF (.pdf) format.
 * Includes vector graphics, structured reservation & payment details, and an embedded vector QR code.
 */
public class PassReceiptGenerator {

    /**
     * Opens a FileChooser, saves the native PDF receipt (.pdf), reveals it in File Explorer,
     * and opens it in the system's default PDF viewer.
     *
     * @return Saved PDF File, or null if user cancelled.
     */
    public static File downloadAndOpenReceipt(Window ownerWindow, Reservation reservation, User user) throws Exception {
        if (reservation == null) {
            throw new Exception("No active reservation to download.");
        }

        String code = reservation.getBookingCode();
        String safeCode = code != null ? code.replaceAll("[^a-zA-Z0-9_-]", "_") : "CRISS_PASS";
        String defaultFileName = "CrissCross_Parking_Pass_" + safeCode + ".pdf";

        // Default to user's Downloads directory
        File defaultDir = Paths.get(System.getProperty("user.home"), "Downloads").toFile();
        if (!defaultDir.exists()) {
            defaultDir = Paths.get(System.getProperty("user.home")).toFile();
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Digital Parking Pass Receipt (PDF)");
        fileChooser.setInitialDirectory(defaultDir);
        fileChooser.setInitialFileName(defaultFileName);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf")
        );

        File targetFile = fileChooser.showSaveDialog(ownerWindow);
        if (targetFile == null) {
            return null; // User cancelled dialog
        }

        // Ensure .pdf extension
        if (!targetFile.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            targetFile = new File(targetFile.getParentFile(), targetFile.getName() + ".pdf");
        }

        // Generate native PDF binary bytes and write to disk
        byte[] pdfBytes = generateReceiptPdfBytes(reservation, user);
        Files.write(targetFile.toPath(), pdfBytes);

        // 1. Reveal in Windows File Explorer
        try {
            new ProcessBuilder("explorer.exe", "/select,", targetFile.getAbsolutePath()).start();
        } catch (Exception ex) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(targetFile.getParentFile());
                }
            } catch (Exception ignored) {}
        }

        // 2. Open PDF file in default PDF viewer
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(targetFile);
            }
        } catch (Exception ignored) {}

        return targetFile;
    }

    /**
     * Exports an Admin System Capacity & Reservations Audit Report as a native PDF file.
     */
    public static File exportAdminAuditReportPdf(Window ownerWindow, ReportService.SystemStats stats) throws Exception {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        String defaultFileName = "CrissCross_Admin_Capacity_Audit_" + ts + ".pdf";

        File defaultDir = Paths.get(System.getProperty("user.home"), "Downloads").toFile();
        if (!defaultDir.exists()) {
            defaultDir = Paths.get(System.getProperty("user.home")).toFile();
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Admin Capacity & Audit Report (PDF)");
        fileChooser.setInitialDirectory(defaultDir);
        fileChooser.setInitialFileName(defaultFileName);
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF Document (*.pdf)", "*.pdf")
        );

        File targetFile = fileChooser.showSaveDialog(ownerWindow);
        if (targetFile == null) {
            return null;
        }
        if (!targetFile.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            targetFile = new File(targetFile.getParentFile(), targetFile.getName() + ".pdf");
        }

        byte[] pdfBytes = generateAdminAuditPdfBytes(stats);
        Files.write(targetFile.toPath(), pdfBytes);

        try {
            new ProcessBuilder("explorer.exe", "/select,", targetFile.getAbsolutePath()).start();
        } catch (Exception ignored) {}

        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(targetFile);
            }
        } catch (Exception ignored) {}

        return targetFile;
    }

    /**
     * Builds a 100% standards-compliant Adobe PDF 1.4 document with vector layout,
     * reservation details, payment receipt details, and a scannable vector QR code.
     */
    public static byte[] generateReceiptPdfBytes(Reservation reservation, User user) {
        String bookingCode = safeText(reservation.getBookingCode(), "CRISS-2026-X892");
        String eventTitle = safeText(reservation.getEventTitle(), "Campus Event");
        String eventVenue = safeText(reservation.getEventVenue(), "Main Campus Venue");
        String areaName = safeText(reservation.getAreaName(), "SJT PARKING");
        String slotNum = safeText(reservation.getSlotNumber(), "SJT-C01");
        String vehicleType = safeText(reservation.getVehicleType(), "CAR");
        String vehiclePlate = safeText(reservation.getVehicleNumber(), "TN-22-CR-1024");
        String resStatus = safeText(reservation.getStatus(), "CONFIRMED");

        String userEmail = (user != null && user.getEmail() != null && !user.getEmail().isEmpty())
                ? user.getEmail()
                : "visitor@campus.edu";
        String userName = (user != null && user.getFullName() != null && !user.getFullName().isEmpty())
                ? user.getFullName()
                : safeText(reservation.getUserName(), "Alex Reynolds");
        String userPhone = (user != null && user.getPhone() != null && !user.getPhone().isEmpty())
                ? user.getPhone()
                : "+91 9845012345";

        double totalPaid = reservation.getTotalAmount();
        boolean isPaidVisitor = totalPaid > 0.0;
        double overtimeRate = isPaidVisitor
                ? ParkingPricingService.getOvertimeHourlyRate("visitor@gmail.com", vehicleType)
                : 0.00;

        String accountCategory = isPaidVisitor
                ? "External Campus Visitor (Standard Tariff)"
                : "VIT Community Member (100% Fee Waiver)";
        String paymentStatus = reservation.getPaymentStatus();
        String paymentMethod = reservation.getPaymentMethod();
        String transactionId = reservation.getTransactionId();
        String payerAccount = reservation.getPayerAccountDetail();
        String amountFormatted = isPaidVisitor
                ? String.format(Locale.US, "INR %.2f (PAID)", totalPaid)
                : "INR 0.00 (100% FREE VIT WAIVER)";
        String baseChargeFormatted = isPaidVisitor
                ? String.format(Locale.US, "INR %.2f (4-Hour Pass)", totalPaid)
                : "INR 0.00 (Complimentary 4-Hr Pass)";
        String overtimeFormatted = isPaidVisitor
                ? String.format(Locale.US, "+INR %.2f / hr after 4 hours (12%%)", overtimeRate)
                : "INR 0.00 / hr (Waived for VIT Email)";

        String generatedTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a"));
        String bookingTimestamp = reservation.getBookingTime() != null
                ? reservation.getBookingTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
                : generatedTimestamp;

        StringBuilder s = new StringBuilder(16384);

        // 1. Page Background (#F1F5F9)
        fillRect(s, 0, 0, 595, 842, 0.945, 0.961, 0.976);

        // 2. Main Receipt Card Container (#0F1428 dark slate card)
        fillStrokeRect(s, 28, 30, 539, 782, 0.059, 0.078, 0.157, 0.910, 0.722, 0.427, 1.5);

        // 3. Top Header Banner (#E8B86D Warm Amber Gold)
        fillRect(s, 28, 734, 539, 78, 0.910, 0.722, 0.427);
        // Coral accent stripe below header
        fillRect(s, 28, 728, 539, 6, 0.945, 0.431, 0.408);

        // Brand Text in Header
        drawText(s, "F2", 18, 46, 782, 0.059, 0.078, 0.157, "CRISSCROSS SMART EVENT PARKING");
        drawText(s, "F1", 10, 46, 764, 0.120, 0.150, 0.250, "Official Digital Gate Access Pass & Payment Receipt");
        drawText(s, "F2", 9, 46, 747, 0.180, 0.210, 0.320, "VIT VELLORE CAMPUS • AUTOMATED BARRIER VERIFICATION");

        // Booking Code Badge Box on Header Right
        fillRect(s, 392, 748, 158, 48, 0.059, 0.078, 0.157);
        drawText(s, "F1", 8, 404, 781, 0.910, 0.722, 0.427, "BOOKING REFERENCE CODE");
        drawText(s, "F3", 13, 404, 760, 1.000, 1.000, 1.000, bookingCode);

        // 4. QR Code & Gate Verification Banner (y=576..714)
        fillStrokeRect(s, 44, 574, 507, 140, 0.090, 0.118, 0.220, 0.220, 0.270, 0.400, 1.0);

        // White QR Box (x=58, y=584, w=120, h=120)
        fillRect(s, 58, 584, 120, 120, 1.0, 1.0, 1.0);

        // Draw Vector QR Code inside the white box
        String qrPayload = QRCodeUtil.buildPayload(reservation);
        try {
            QrCode qr = QrCode.encodeText(qrPayload, QrCode.Ecc.MEDIUM);
            int modules = qr.size;
            double quietZone = 8.0;
            double availSize = 120.0 - (quietZone * 2.0);
            double modSize = availSize / modules;
            double startX = 58.0 + quietZone;
            double topY = 584.0 + 120.0 - quietZone;

            s.append("0.059 0.090 0.165 rg\n");
            for (int row = 0; row < modules; row++) {
                for (int col = 0; col < modules; col++) {
                    if (qr.getModule(col, row)) {
                        double rx = startX + (col * modSize);
                        double ry = topY - ((row + 1) * modSize);
                        s.append(String.format(Locale.US, "%.2f %.2f %.2f %.2f re\n", rx, ry, modSize + 0.15, modSize + 0.15));
                    }
                }
            }
            s.append("f\n");
        } catch (Exception ignored) {}

        // QR Right Info
        drawText(s, "F2", 13, 196, 688, 1.0, 1.0, 1.0, "OFFICIAL GATE BARRIER CHECK-IN PASS");
        drawText(s, "F1", 9.5, 196, 670, 0.796, 0.835, 0.882, "Present this PDF pass at the " + truncate(areaName, 28) + " entry gate scanner.");
        drawText(s, "F1", 9.5, 196, 655, 0.796, 0.835, 0.882, "Camera automatically verifies your reserved bay (" + slotNum + ") & plate (" + vehiclePlate + ").");

        // Status & Payment Pill Badges
        if (isPaidVisitor) {
            fillStrokeRect(s, 196, 616, 210, 26, 0.086, 0.396, 0.204, 0.290, 0.871, 0.502, 1.0);
            drawText(s, "F2", 9.5, 208, 625, 0.290, 0.871, 0.502, "PAYMENT VERIFIED: " + amountFormatted);
        } else {
            fillStrokeRect(s, 196, 616, 235, 26, 0.086, 0.396, 0.204, 0.290, 0.871, 0.502, 1.0);
            drawText(s, "F2", 9.5, 208, 625, 0.290, 0.871, 0.502, "VIT FEE WAIVER: INR 0.00 (100% FREE)");
        }

        fillStrokeRect(s, 440, 616, 95, 26, 0.150, 0.200, 0.350, 0.910, 0.722, 0.427, 1.0);
        drawText(s, "F2", 9.5, 452, 625, 0.910, 0.722, 0.427, resStatus);

        drawText(s, "F1", 8.5, 196, 594, 0.580, 0.639, 0.722, "Booking Timestamp: " + bookingTimestamp + "   |   Pass Duration: 4 Hours Base Access");

        // 5. SECTION 1: EVENT & PARKING SLOT ALLOCATION DETAILS (y=424..558)
        drawSectionHeader(s, 44, 540, 507, "1. EVENT & DESIGNATED PARKING SLOT DETAILS");
        fillStrokeRect(s, 44, 422, 507, 114, 0.082, 0.106, 0.200, 0.200, 0.250, 0.380, 0.8);

        drawField(s, 58, 508, "CAMPUS EVENT TITLE", truncate(eventTitle, 38), false);
        drawField(s, 310, 508, "EVENT VENUE LOCATION", truncate(eventVenue, 34), false);

        drawField(s, 58, 472, "ALLOCATED PARKING AREA", truncate(areaName, 34), true);
        drawField(s, 310, 472, "RESERVED PARKING SLOT", slotNum + " (Designated Bay)", true);

        drawField(s, 58, 436, "VEHICLE CLASSIFICATION", vehicleType + " (Campus Registered)", false);
        drawField(s, 310, 436, "VEHICLE PLATE NUMBER", vehiclePlate, false);

        // 6. SECTION 2: ATTENDEE & ACCOUNT CREDENTIALS (y=316..406)
        drawSectionHeader(s, 44, 390, 507, "2. ATTENDEE & ACCOUNT CREDENTIALS");
        fillStrokeRect(s, 44, 308, 507, 78, 0.082, 0.106, 0.200, 0.200, 0.250, 0.380, 0.8);

        drawField(s, 58, 358, "BOOKED FOR (ATTENDEE NAME)", truncate(userName, 34), false);
        drawField(s, 310, 358, "REGISTERED ACCOUNT EMAIL", truncate(userEmail, 36), false);

        drawField(s, 58, 322, "CONTACT PHONE NUMBER", truncate(userPhone, 28), false);
        drawField(s, 310, 322, "CUSTOMER TARIFF CATEGORY", truncate(accountCategory, 36), false);

        // 7. SECTION 3: PAYMENT & BILLING RECEIPT DETAILS (y=164..292)
        drawSectionHeader(s, 44, 276, 507, "3. PAYMENT & BILLING TRANSACTION DETAILS");
        fillStrokeRect(s, 44, 158, 507, 114, 0.082, 0.106, 0.200, 0.910, 0.722, 0.427, 1.0);

        drawField(s, 58, 244, "PAYMENT STATUS", paymentStatus, true);
        drawField(s, 310, 244, "PAYMENT METHOD / MODE", truncate(paymentMethod, 34), false);

        drawField(s, 58, 208, "TRANSACTION REFERENCE ID", transactionId, false);
        drawField(s, 310, 208, "BILLING / PAYER CREDENTIAL", truncate(payerAccount, 34), false);

        drawField(s, 58, 172, "BASE 4-HOUR SLOT CHARGE", baseChargeFormatted, false);
        drawField(s, 310, 172, "TOTAL AMOUNT PAID", amountFormatted, true);

        // 8. SECTION 4: DURATION & OVERTIME SURCHARGE POLICY (y=82..144)
        fillStrokeRect(s, 44, 82, 507, 62, 0.120, 0.105, 0.145, 0.910, 0.722, 0.427, 0.9);
        drawText(s, "F2", 9.5, 58, 126, 0.910, 0.722, 0.427, "DURATION & OVERTIME SURCHARGE POLICY:");
        drawText(s, "F1", 8.8, 58, 110, 0.850, 0.880, 0.920,
                "Pass Validity: 4 Hours from gate check-in. Overtime Surcharge Rate: " + overtimeFormatted + ".");
        drawText(s, "F1", 8.8, 58, 95, 0.720, 0.760, 0.820,
                "Please ensure your vehicle is parked strictly within bay " + slotNum + " at " + areaName + ".");

        // 9. Footer Bar (y=30..70)
        fillRect(s, 28, 30, 539, 38, 0.039, 0.051, 0.110);
        drawText(s, "F1", 8.5, 44, 46, 0.580, 0.639, 0.722, "Receipt Generated: " + generatedTimestamp);
        drawText(s, "F2", 8.5, 340, 46, 0.910, 0.722, 0.427, "CrissCross Smart Event Parking System • Official PDF");

        return assembleSinglePagePdf(s.toString());
    }

    /**
     * Generates an HTML preview string of the parking pass receipt (with embedded SVG QR code
     * and payment breakdown) for automated verification and preview rendering.
     */
    public static String generateReceiptHtml(Reservation reservation, User user) {
        String bookingCode = safeText(reservation.getBookingCode(), "CRISS-2026-X892");
        String eventTitle = safeText(reservation.getEventTitle(), "Campus Event");
        String eventVenue = safeText(reservation.getEventVenue(), "Main Campus Venue");
        String areaName = safeText(reservation.getAreaName(), "SJT PARKING");
        String slotNum = safeText(reservation.getSlotNumber(), "SJT-C01");
        String vehicleType = safeText(reservation.getVehicleType(), "CAR");
        String vehiclePlate = safeText(reservation.getVehicleNumber(), "TN-22-CR-1024");
        String userEmail = (user != null && user.getEmail() != null && !user.getEmail().isEmpty())
                ? user.getEmail()
                : "visitor@campus.edu";
        String userName = (user != null && user.getFullName() != null && !user.getFullName().isEmpty())
                ? user.getFullName()
                : safeText(reservation.getUserName(), "Alex Reynolds");

        boolean isVit = ParkingPricingService.isVitEmail(userEmail) && reservation.getTotalAmount() <= 0.0;
        double baseCharge = isVit ? 0.00 : (reservation.getTotalAmount() > 0 ? reservation.getTotalAmount() : ParkingPricingService.getBaseCharge(userEmail, vehicleType));
        double overtimeRate = ParkingPricingService.getOvertimeHourlyRate(userEmail, vehicleType);

        String tariffCategory = isVit ? "VIT Campus Member — FREE PASS (100% WAIVED)" : "Visitor Standard 4-Hour Tariff";
        String totalDisplay = isVit ? "₹ 0.00 (FREE)" : String.format(Locale.US, "₹ %.2f", baseCharge);
        String overtimeDisplay = isVit
                ? "₹ 0.00 / hr (Waived for VIT Email)"
                : String.format(Locale.US, "+₹ %.2f / hr (12%% of base charge per extra hour beyond 4 hours)", overtimeRate);

        String qrSvg = QRCodeUtil.generateSvg(QRCodeUtil.buildPayload(reservation));

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>CrissCross Parking Pass - " + bookingCode + "</title></head>"
                + "<body>"
                + "<h1>CrissCross Digital Parking Pass & Receipt</h1>"
                + "<p>Booking Code: " + bookingCode + "</p>"
                + "<p>Attendee: " + userName + " (" + userEmail + ")</p>"
                + "<p>Event: " + eventTitle + " (" + eventVenue + ")</p>"
                + "<p>Parking Zone: " + areaName + " | Slot: " + slotNum + "</p>"
                + "<p>Vehicle: " + vehicleType + " | Plate: " + vehiclePlate + "</p>"
                + "<p>Tariff Category: " + tariffCategory + "</p>"
                + "<p>Total Amount Paid: " + totalDisplay + "</p>"
                + "<p>Payment Status: " + reservation.getPaymentStatus() + " | Method: " + reservation.getPaymentMethod() + " | Txn: " + reservation.getTransactionId() + "</p>"
                + "<p>Overtime Policy: " + overtimeDisplay + "</p>"
                + "<div class=\"qr\">" + qrSvg + "</div>"
                + "</body></html>";
    }

    /**
     * Builds a comprehensive PDF Audit Report for the Admin Reports Dashboard.
     */
    private static byte[] generateAdminAuditPdfBytes(ReportService.SystemStats stats) {
        StringBuilder s = new StringBuilder(16384);
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm:ss a"));

        fillRect(s, 0, 0, 595, 842, 0.945, 0.961, 0.976);
        fillStrokeRect(s, 28, 30, 539, 782, 0.059, 0.078, 0.157, 0.910, 0.722, 0.427, 1.5);

        fillRect(s, 28, 734, 539, 78, 0.910, 0.722, 0.427);
        drawText(s, "F2", 18, 46, 780, 0.059, 0.078, 0.157, "CRISSCROSS ADMIN CAPACITY & AUDIT REPORT");
        drawText(s, "F1", 10, 46, 760, 0.120, 0.150, 0.250, "Real-Time Campus Parking Slot Capacity, Occupancy & Revenue Summary");
        drawText(s, "F2", 9, 46, 744, 0.180, 0.210, 0.320, "Generated: " + ts);

        drawSectionHeader(s, 44, 696, 507, "1. OVERALL SYSTEM CAPACITY & BOOKING METRICS");
        fillStrokeRect(s, 44, 578, 507, 114, 0.082, 0.106, 0.200, 0.200, 0.250, 0.380, 0.8);

        drawField(s, 58, 662, "TOTAL CAMPUS SLOT CAPACITY", stats.totalCapacity + " Total Bays", false);
        drawField(s, 310, 662, "FILLED / ALLOCATED SLOTS",
                stats.totalOccupied + " Filled (" + String.format(Locale.US, "%.1f%%", stats.overallOccupancyRate) + ")", true);

        drawField(s, 58, 626, "AVAILABLE EMPTY SLOTS", stats.totalAvailable + " Free Bays", false);
        drawField(s, 310, 626, "CAR vs BIKE BAYS FILLED",
                "Car: " + stats.filledCarSlots + "/" + stats.totalCarSlots + "  |  Bike: " + stats.filledBikeSlots + "/" + stats.totalBikeSlots, false);

        drawField(s, 58, 590, "ACTIVE / TOTAL RESERVATIONS",
                stats.confirmedReservations + " Confirmed / " + stats.totalReservations + " Total", false);
        drawField(s, 310, 590, "VISITOR REVENUE COLLECTED",
                String.format(Locale.US, "INR %.2f (%d Paid Bookings)", stats.totalRevenueCollected, stats.paidVisitorBookings), true);

        drawSectionHeader(s, 44, 540, 507, "2. ZONE-BY-ZONE PARKING SLOT CAPACITY BREAKDOWN");
        int y = 490;
        for (ReportService.ZoneCapacityDetail z : stats.zoneDetails) {
            fillStrokeRect(s, 44, y - 38, 507, 68, 0.082, 0.106, 0.200, 0.220, 0.270, 0.400, 0.8);
            double pct = z.totalSlots > 0 ? (z.filledSlots * 100.0 / z.totalSlots) : 0.0;
            drawText(s, "F2", 11, 58, y + 12, 0.910, 0.722, 0.427,
                    z.areaName + " (" + z.areaCode + ")  -  " + z.filledSlots + " / " + z.totalSlots + " Filled (" + String.format(Locale.US, "%.1f%%", pct) + ")");
            drawText(s, "F1", 9, 58, y - 4, 0.850, 0.880, 0.920,
                    "Available: " + z.availableSlots + " Free   |   Reserved: " + z.reservedSlots + "   |   Occupied: " + z.occupiedSlots + "   |   Blocked: " + z.blockedSlots);
            drawText(s, "F1", 9, 58, y - 20, 0.720, 0.780, 0.860,
                    "Car Slots: " + z.carFilled + "/" + z.carTotal + " Filled (" + z.carFree + " Free)   |   Bike Slots: " + z.bikeFilled + "/" + z.bikeTotal + " Filled (" + z.bikeFree + " Free)");
            y -= 82;
        }

        fillRect(s, 28, 30, 539, 38, 0.039, 0.051, 0.110);
        drawText(s, "F1", 8.5, 44, 46, 0.580, 0.639, 0.722, "CrissCross Admin Analytics • Exported on " + ts);

        return assembleSinglePagePdf(s.toString());
    }

    private static void drawSectionHeader(StringBuilder s, double x, double y, double w, String title) {
        fillRect(s, x, y, w, 20, 0.145, 0.180, 0.310);
        fillRect(s, x, y, 4, 20, 0.945, 0.431, 0.408);
        drawText(s, "F2", 9.5, x + 12, y + 6, 0.910, 0.722, 0.427, title);
    }

    private static void drawField(StringBuilder s, double x, double y, String label, String value, boolean highlight) {
        drawText(s, "F1", 8.0, x, y + 13, 0.580, 0.639, 0.722, label);
        if (highlight) {
            drawText(s, "F2", 11.0, x, y, 0.949, 0.627, 0.416, value);
        } else {
            drawText(s, "F2", 10.5, x, y, 0.973, 0.980, 0.988, value);
        }
    }

    private static void fillRect(StringBuilder s, double x, double y, double w, double h, double r, double g, double b) {
        s.append(String.format(Locale.US, "%.3f %.3f %.3f rg\n%.2f %.2f %.2f %.2f re f\n", r, g, b, x, y, w, h));
    }

    private static void fillStrokeRect(StringBuilder s, double x, double y, double w, double h,
                                       double fr, double fg, double fb,
                                       double sr, double sg, double sb, double lineWidth) {
        s.append(String.format(Locale.US, "%.2f w\n%.3f %.3f %.3f rg\n%.3f %.3f %.3f RG\n%.2f %.2f %.2f %.2f re B\n",
                lineWidth, fr, fg, fb, sr, sg, sb, x, y, w, h));
    }

    private static void drawText(StringBuilder s, String fontKey, double fontSize, double x, double y,
                                 double r, double g, double b, String text) {
        String clean = sanitizePdfText(text);
        s.append(String.format(Locale.US, "BT /%s %.1f Tf %.3f %.3f %.3f rg %.2f %.2f Td (%s) Tj ET\n",
                fontKey, fontSize, r, g, b, x, y, clean));
    }

    private static String sanitizePdfText(String text) {
        if (text == null) return "";
        String replaced = text
                .replace("₹", "INR ")
                .replace("•", "-")
                .replace("—", "-")
                .replace("–", "-");
        StringBuilder sb = new StringBuilder(replaced.length());
        for (int i = 0; i < replaced.length(); i++) {
            char c = replaced.charAt(i);
            if (c == '\\' || c == '(' || c == ')') {
                sb.append('\\').append(c);
            } else if (c >= 32 && c <= 126) {
                sb.append(c);
            } else {
                sb.append(' ');
            }
        }
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    private static String safeText(String val, String fallback) {
        return (val != null && !val.trim().isEmpty()) ? val.trim() : fallback;
    }

    private static String truncate(String val, int maxLen) {
        if (val == null) return "";
        return val.length() <= maxLen ? val : val.substring(0, maxLen - 3) + "...";
    }

    /**
     * Packages a PDF content stream into a complete, valid ISO-32000 PDF 1.4 binary document.
     */
    private static byte[] assembleSinglePagePdf(String contentStream) {
        try {
            byte[] streamBytes = contentStream.getBytes(StandardCharsets.ISO_8859_1);
            ByteArrayOutputStream out = new ByteArrayOutputStream(streamBytes.length + 2048);
            List<Integer> offsets = new ArrayList<>();

            writeAscii(out, "%PDF-1.4\n%\u00E2\u00E3\u00CF\u00D3\n");

            // Object 1: Catalog
            offsets.add(out.size());
            writeAscii(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

            // Object 2: Pages
            offsets.add(out.size());
            writeAscii(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");

            // Object 3: Page (A4 595 x 842)
            offsets.add(out.size());
            writeAscii(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Resources << /Font << /F1 4 0 R /F2 5 0 R /F3 6 0 R >> >> "
                    + "/Contents 7 0 R >>\nendobj\n");

            // Object 4: Font F1 (Helvetica)
            offsets.add(out.size());
            writeAscii(out, "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n");

            // Object 5: Font F2 (Helvetica-Bold)
            offsets.add(out.size());
            writeAscii(out, "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");

            // Object 6: Font F3 (Courier-Bold)
            offsets.add(out.size());
            writeAscii(out, "6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold /Encoding /WinAnsiEncoding >>\nendobj\n");

            // Object 7: Page Content Stream
            offsets.add(out.size());
            writeAscii(out, "7 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n");
            out.write(streamBytes);
            writeAscii(out, "\nendstream\nendobj\n");

            // Cross-Reference Table (xref)
            int xrefOffset = out.size();
            int totalObjects = offsets.size() + 1;
            writeAscii(out, "xref\n0 " + totalObjects + "\n");
            writeAscii(out, "0000000000 65535 f \n");
            for (int offset : offsets) {
                writeAscii(out, String.format(Locale.US, "%010d 00000 n \n", offset));
            }

            // Trailer
            writeAscii(out, "trailer\n<< /Size " + totalObjects + " /Root 1 0 R >>\n");
            writeAscii(out, "startxref\n" + xrefOffset + "\n%%EOF\n");

            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build PDF document: " + e.getMessage(), e);
        }
    }

    private static void writeAscii(ByteArrayOutputStream out, String s) throws Exception {
        out.write(s.getBytes(StandardCharsets.ISO_8859_1));
    }
}
