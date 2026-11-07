package com.crisscross.services;

/**
 * Service managing parking tariffs and pricing rules.
 *
 * Rules:
 * 1. VIT Email (contains 'vit.ac.in'): 100% Complimentary Free Event Parking Pass (₹ 0.00).
 * 2. Visitor / External Emails:
 *    - Base duration: 4 Hours
 *    - Bike: ₹ 30.00
 *    - Car: ₹ 50.00
 *    - Overtime (after 4 hours): 12% extra from the original charge per hour
 *      (Car: +₹ 6.00 / hr; Bike: +₹ 3.60 / hr).
 */
public class ParkingPricingService {

    public static final int BASE_DURATION_HOURS = 4;
    public static final double CAR_BASE_FEE = 50.00;
    public static final double BIKE_BASE_FEE = 30.00;
    public static final double OVERTIME_PERCENTAGE = 0.12; // 12%

    /**
     * Determines whether the provided email qualifies as a verified VIT community account.
     * Supports both @vit.ac.in (faculty/staff/students) and @vitstudent.ac.in (students).
     */
    public static boolean isVitEmail(String email) {
        if (email == null) return false;
        String normalized = email.trim().toLowerCase();
        return normalized.contains("vit.ac.in")
                || normalized.contains("vitstudent.ac.in")
                || normalized.endsWith("@vit.edu");
    }

    /**
     * Computes the base booking charge for a 4-hour reservation.
     */
    public static double getBaseCharge(String email, String vehicleType) {
        if (isVitEmail(email)) {
            return 0.00;
        }
        if ("BIKE".equalsIgnoreCase(vehicleType)) {
            return BIKE_BASE_FEE;
        } else {
            return CAR_BASE_FEE;
        }
    }

    /**
     * Calculates the hourly overtime surcharge (12% of original charge).
     */
    public static double getOvertimeHourlyRate(String email, String vehicleType) {
        if (isVitEmail(email)) {
            return 0.00;
        }
        double base = getBaseCharge(email, vehicleType);
        return Math.round(base * OVERTIME_PERCENTAGE * 100.0) / 100.0;
    }

    /**
     * Formatted label of the user's category (VITian vs Visitor).
     */
    public static String getTariffCategory(String email) {
        if (isVitEmail(email)) {
            return "🎓 VIT Community Member (Complimentary Pass)";
        } else {
            return "💳 External Campus Visitor Standard Tariff";
        }
    }

    /**
     * Concise summary string of tariff & overtime rules.
     */
    public static String getTariffSummary(String email, String vehicleType) {
        if (isVitEmail(email)) {
            return "Complimentary 4-Hour Event Pass (₹ 0.00 - VIT Waiver)";
        } else {
            double base = getBaseCharge(email, vehicleType);
            double extra = getOvertimeHourlyRate(email, vehicleType);
            return "₹ " + String.format("%.2f", base) + " for 4 Hours (+₹ " + String.format("%.2f", extra) + "/hr overtime [12%])";
        }
    }

    /**
     * Detailed multi-line explanation for receipts and summary dialogs.
     */
    public static String getOvertimePolicyDescription(String email, String vehicleType) {
        if (isVitEmail(email)) {
            return "Overtime policy: Waived for verified VIT campus members (vit.ac.in).";
        } else {
            double extra = getOvertimeHourlyRate(email, vehicleType);
            return "Pass validity: 4 Hours from gate check-in. Overtime hours charged at 12% of base tariff (+₹ "
                    + String.format("%.2f", extra) + " per extra hour beyond 4 hours).";
        }
    }
}
