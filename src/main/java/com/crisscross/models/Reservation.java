package com.crisscross.models;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;
    private int reservationId;
    private String bookingCode; // e.g., 'CRISS-2026-X892'
    private int userId;
    private String userName;
    private int eventId;
    private String eventTitle;
    private String eventVenue;
    private int areaId;
    private String areaName;
    private int slotId;
    private String slotNumber;
    private String vehicleType;   // 'CAR', 'BIKE'
    private String vehicleNumber;
    private LocalDateTime bookingTime;
    private int durationHours;
    private double totalAmount;
    private String status;        // 'CONFIRMED', 'ACTIVE', 'COMPLETED', 'CANCELLED'
    private String notes;

    public Reservation() {
        this.bookingTime = LocalDateTime.now();
        this.status = "CONFIRMED";
        this.durationHours = 3;
        this.totalAmount = 0.00;
    }

    public Reservation(int reservationId, String bookingCode, int userId, int eventId, int areaId,
                       int slotId, String vehicleType, String vehicleNumber, LocalDateTime bookingTime,
                       int durationHours, double totalAmount, String status, String notes) {
        this.reservationId = reservationId;
        this.bookingCode = bookingCode;
        this.userId = userId;
        this.eventId = eventId;
        this.areaId = areaId;
        this.slotId = slotId;
        this.vehicleType = vehicleType;
        this.vehicleNumber = vehicleNumber;
        this.bookingTime = bookingTime != null ? bookingTime : LocalDateTime.now();
        this.durationHours = durationHours;
        this.totalAmount = totalAmount;
        this.status = status != null ? status : "CONFIRMED";
        this.notes = notes;
    }

    public int getReservationId() { return reservationId; }
    public void setReservationId(int reservationId) { this.reservationId = reservationId; }

    public String getBookingCode() { return bookingCode; }
    public void setBookingCode(String bookingCode) { this.bookingCode = bookingCode; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public String getEventVenue() { return eventVenue; }
    public void setEventVenue(String eventVenue) { this.eventVenue = eventVenue; }

    public int getAreaId() { return areaId; }
    public void setAreaId(int areaId) { this.areaId = areaId; }

    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }

    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }

    public String getSlotNumber() { return slotNumber; }
    public void setSlotNumber(String slotNumber) { this.slotNumber = slotNumber; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }

    public LocalDateTime getBookingTime() { return bookingTime; }
    public void setBookingTime(LocalDateTime bookingTime) { this.bookingTime = bookingTime; }

    public int getDurationHours() { return durationHours; }
    public void setDurationHours(int durationHours) { this.durationHours = durationHours; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPaymentStatus() {
        if (notes != null && notes.startsWith("PAID")) {
            return "PAID & VERIFIED";
        }
        if (totalAmount > 0.0) {
            return "PAID & VERIFIED";
        }
        return "WAIVED (100% FREE VIT PASS)";
    }

    public String getPaymentMethod() {
        String extracted = extractNoteField("Method:");
        if (extracted != null && !extracted.isEmpty()) {
            return extracted;
        }
        return totalAmount > 0.0 ? "UPI Instant Pay" : "VIT Institutional Waiver";
    }

    public String getTransactionId() {
        String extracted = extractNoteField("TxnID:");
        if (extracted != null && !extracted.isEmpty()) {
            return extracted;
        }
        String suffix = (bookingCode != null && bookingCode.contains("-"))
                ? bookingCode.substring(bookingCode.lastIndexOf('-') + 1)
                : "8492";
        return totalAmount > 0.0 ? ("TXN-CRISS-2026-" + suffix) : ("VIT-WAIVER-" + suffix);
    }

    public String getPayerAccountDetail() {
        String extracted = extractNoteField("Account:");
        if (extracted != null && !extracted.isEmpty()) {
            return extracted;
        }
        return totalAmount > 0.0 ? "External Visitor Account" : "Verified VIT Email Account";
    }

    private String extractNoteField(String key) {
        if (notes == null || !notes.contains(key)) return null;
        int start = notes.indexOf(key) + key.length();
        int end = notes.indexOf("|", start);
        if (end == -1) {
            return notes.substring(start).trim();
        }
        return notes.substring(start, end).trim();
    }

    @Override
    public String toString() {
        return bookingCode + " - " + slotNumber + " (" + status + ")";
    }
}
