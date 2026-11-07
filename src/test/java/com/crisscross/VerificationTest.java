package com.crisscross;

import com.crisscross.models.Reservation;
import com.crisscross.models.User;
import com.crisscross.services.AuthService;
import com.crisscross.services.ParkingPricingService;
import com.crisscross.util.PassReceiptGenerator;
import com.crisscross.util.QRCodeUtil;

public class VerificationTest {
    public static void main(String[] args) {
        System.out.println("=== Starting CrissCross Feature Verification ===");

        // 1. Password Constraints Verification
        System.out.println("\n--- Testing Password Constraints ---");
        testPassword("short1!", "user", false, "Too short (<12)");
        testPassword("nocaps12345!@", "user", false, "No uppercase");
        testPassword("NONUMBERS!@#ABC", "user", false, "No numbers");
        testPassword("NoSpecial123456", "user", false, "No special characters");
        testPassword("MyPass12345!john", "john", false, "Contains username");
        testPassword("Complex@Pass1234", "john", true, "Valid >=12, caps, num, special, no username");
        testPassword("VitParking#2026", "priya", true, "Valid >=12, caps, num, special, no username");

        // 2. Parking Pricing Service Verification
        System.out.println("\n--- Testing Parking Pricing & Overtime Policy ---");
        // VIT email checks
        assert ParkingPricingService.isVitEmail("student@vit.ac.in") : "student@vit.ac.in should be VIT email";
        assert ParkingPricingService.isVitEmail("faculty.name@vit.ac.in") : "faculty should be VIT email";
        assert ParkingPricingService.isVitEmail("alumni@vitstudent.ac.in") : "vitstudent.ac.in should be VIT email";
        assert !ParkingPricingService.isVitEmail("visitor@gmail.com") : "gmail.com should not be VIT email";
        assert !ParkingPricingService.isVitEmail("guest@campus.edu") : "campus.edu should not be VIT email";

        // Charges for VIT email
        double vitCarFee = ParkingPricingService.getBaseCharge("alex@vit.ac.in", "CAR");
        double vitBikeFee = ParkingPricingService.getBaseCharge("alex@vit.ac.in", "BIKE");
        double vitOvertimeRate = ParkingPricingService.getOvertimeHourlyRate("alex@vit.ac.in", "CAR");
        System.out.println("VIT Car Fee: " + vitCarFee + " (Expected: 0.0)");
        System.out.println("VIT Bike Fee: " + vitBikeFee + " (Expected: 0.0)");
        System.out.println("VIT Overtime Surcharge: " + vitOvertimeRate + " (Expected: 0.0)");
        assert vitCarFee == 0.00 : "VIT Car fee should be 0.00";
        assert vitBikeFee == 0.00 : "VIT Bike fee should be 0.00";
        assert vitOvertimeRate == 0.00 : "VIT Overtime fee should be 0.00";

        // Charges for external visitor email
        double extCarFee = ParkingPricingService.getBaseCharge("visitor@gmail.com", "CAR");
        double extBikeFee = ParkingPricingService.getBaseCharge("visitor@gmail.com", "BIKE");
        double extCarOvertime = ParkingPricingService.getOvertimeHourlyRate("visitor@gmail.com", "CAR");
        double extBikeOvertime = ParkingPricingService.getOvertimeHourlyRate("visitor@gmail.com", "BIKE");
        System.out.println("External Car Fee (4 hrs): " + extCarFee + " (Expected: 50.0)");
        System.out.println("External Bike Fee (4 hrs): " + extBikeFee + " (Expected: 30.0)");
        System.out.println("External Car Overtime (+12%): " + extCarOvertime + " (Expected: 6.0)");
        System.out.println("External Bike Overtime (+12%): " + extBikeOvertime + " (Expected: 3.6)");
        assert extCarFee == 50.00 : "External Car fee must be 50.00";
        assert extBikeFee == 30.00 : "External Bike fee must be 30.00";
        assert extCarOvertime == 6.00 : "External Car overtime must be 6.00 (12% of 50)";
        assert extBikeOvertime == 3.60 : "External Bike overtime must be 3.60 (12% of 30)";

        // 3. QR Code Vector Generation Verification
        System.out.println("\n--- Testing QR Code SVG Generation ---");
        Reservation testRes = new Reservation(101, "CRISS-2026-TEST", 2, 1, 1, 1, "CAR", "TN-22-CR-1024", null, 4, 50.00, "CONFIRMED", "Testing");
        testRes.setEventTitle("Tech Symposium 2026");
        testRes.setEventVenue("SJT Auditorium");
        testRes.setAreaName("SJT PARKING");
        testRes.setSlotNumber("SJT-C10");
        testRes.setUserName("Alex Reynolds");

        String payload = QRCodeUtil.buildPayload(testRes);
        System.out.println("Generated QR Gate Payload:\n" + payload);
        assert payload.contains("CRISS-2026-TEST") : "Payload missing booking code";
        assert payload.contains("SJT-C10") : "Payload missing slot";
        assert payload.contains("TN-22-CR-1024") : "Payload missing plate";

        String svg = QRCodeUtil.generateSvg(payload);
        assert svg != null && svg.contains("<svg") && svg.contains("<path") : "SVG generation failed";
        System.out.println("Generated QR SVG length: " + svg.length() + " chars (Valid vector XML)");

        // 4. Pass Receipt PDF & HTML Output Verification
        System.out.println("\n--- Testing Native PDF & HTML Pass Receipt Generation ---");
        User testUser = new User(2, "Alex Reynolds", "alex@gmail.com", "alex", "Pass@12345678", "9845012345", "TN-22-CR-1024", "CUSTOMER", "ACTIVE");
        byte[] pdfBytes = PassReceiptGenerator.generateReceiptPdfBytes(testRes, testUser);
        String pdfContent = new String(pdfBytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        assert pdfContent.startsWith("%PDF-1.4") : "Receipt must be a valid native PDF-1.4 document";
        assert pdfContent.contains("CRISS-2026-TEST") : "PDF missing booking code";
        assert pdfContent.contains("SJT-C10") : "PDF missing slot number";
        System.out.println("Native PDF Receipt generated (" + pdfBytes.length + " bytes) with %PDF-1.4 header.");

        String receiptHtml = PassReceiptGenerator.generateReceiptHtml(testRes, testUser);
        assert receiptHtml.contains("CRISS-2026-TEST") : "Receipt missing code";
        assert receiptHtml.contains("Tech Symposium 2026") : "Receipt missing event";
        assert receiptHtml.contains("SJT-C10") : "Receipt missing slot";
        assert receiptHtml.contains("₹ 50.00") : "Receipt missing tariff amount";
        assert receiptHtml.contains("12%") : "Receipt missing 12% overtime notice";
        assert receiptHtml.contains("<svg") : "Receipt missing embedded QR SVG";
        System.out.println("Pass Receipt HTML successfully generated (" + receiptHtml.length() + " chars) with scannable vector QR code and policy breakdown.");

        // Check VIT receipt
        User vitUser = new User(5, "Priya", "priya@vit.ac.in", "priyavit", "Priya#Pass2026", "9888877777", "TN-22-VT-2026", "CUSTOMER", "ACTIVE");
        Reservation vitRes = new Reservation(102, "CRISS-2026-VIT01", 5, 1, 1, 1, "CAR", "TN-22-VT-2026", null, 4, 0.00, "CONFIRMED", "VIT Free Pass");
        vitRes.setEventTitle("Tech Symposium 2026");
        vitRes.setEventVenue("SJT Auditorium");
        vitRes.setAreaName("SJT PARKING");
        vitRes.setSlotNumber("SJT-C11");
        vitRes.setUserName("Priya");
        String vitReceiptHtml = PassReceiptGenerator.generateReceiptHtml(vitRes, vitUser);
        assert vitReceiptHtml.contains("FREE PASS (100% WAIVED)") : "VIT receipt missing waiver badge";
        assert vitReceiptHtml.contains("₹ 0.00 (FREE)") : "VIT receipt missing ₹ 0.00 amount";
        System.out.println("VIT Complimentary Receipt verified successfully.");

        System.out.println("\n>>> ALL VERIFICATION CHECKS PASSED SUCCESSFULLY! <<<");
    }

    private static void testPassword(String pwd, String user, boolean expectValid, String caseDesc) {
        try {
            AuthService.validatePasswordConstraints(pwd, user);
            if (!expectValid) {
                System.err.println("FAIL [" + caseDesc + "]: Expected failure but passed for '" + pwd + "'");
                System.exit(1);
            } else {
                System.out.println("PASS [" + caseDesc + "]: Accepted valid password");
            }
        } catch (Exception e) {
            if (expectValid) {
                System.err.println("FAIL [" + caseDesc + "]: Expected valid but threw: " + e.getMessage());
                System.exit(1);
            } else {
                System.out.println("PASS [" + caseDesc + "]: Correctly rejected: " + e.getMessage());
            }
        }
    }
}
