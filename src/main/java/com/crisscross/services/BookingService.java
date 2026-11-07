package com.crisscross.services;

import com.crisscross.dao.NotificationDAO;
import com.crisscross.dao.ParkingAreaDAO;
import com.crisscross.dao.ParkingSlotDAO;
import com.crisscross.dao.ReservationDAO;
import com.crisscross.models.Notification;
import com.crisscross.models.ParkingSlot;
import com.crisscross.models.Reservation;

import java.time.LocalDateTime;
import java.util.Random;

public class BookingService {
    private static BookingService instance;
    private final ReservationDAO reservationDAO;
    private final ParkingSlotDAO slotDAO;
    private final ParkingAreaDAO areaDAO;
    private final NotificationDAO notificationDAO;
    private final Random random = new Random();

    private BookingService() {
        this.reservationDAO = new ReservationDAO();
        this.slotDAO = new ParkingSlotDAO();
        this.areaDAO = new ParkingAreaDAO();
        this.notificationDAO = new NotificationDAO();
    }

    public static synchronized BookingService getInstance() {
        if (instance == null) {
            instance = new BookingService();
        }
        return instance;
    }

    public synchronized Reservation createBooking(int userId, String userName, int eventId, String eventTitle,
                                                  String eventVenue, int areaId, String areaName, int slotId,
                                                  String slotNumber, String vehicleType, String vehicleNumber,
                                                  int durationHours, double totalAmount, String notes) throws Exception {

        ParkingSlot slot = slotDAO.findById(slotId);
        if (slot == null) {
            throw new Exception("Selected parking slot could not be found.");
        }
        if (!slot.isAvailable()) {
            throw new Exception("Slot " + slot.getSlotNumber() + " is no longer available. Please select another slot.");
        }

        String bookingCode = generateUniqueBookingCode();

        Reservation reservation = new Reservation();
        reservation.setBookingCode(bookingCode);
        reservation.setUserId(userId);
        reservation.setUserName(userName);
        reservation.setEventId(eventId);
        reservation.setEventTitle(eventTitle);
        reservation.setEventVenue(eventVenue);
        reservation.setAreaId(areaId);
        reservation.setAreaName(areaName);
        reservation.setSlotId(slotId);
        reservation.setSlotNumber(slotNumber);
        reservation.setVehicleType(vehicleType != null ? vehicleType : "CAR");
        reservation.setVehicleNumber(vehicleNumber != null ? vehicleNumber.toUpperCase() : "TN-00-XX-0000");
        reservation.setDurationHours(durationHours > 0 ? durationHours : 3);
        reservation.setTotalAmount(totalAmount);
        reservation.setStatus("CONFIRMED");
        reservation.setNotes(notes != null ? notes : "Event Parking Reservation");
        reservation.setBookingTime(LocalDateTime.now());

        boolean ok = reservationDAO.insert(reservation);
        if (!ok) {
            throw new Exception("Failed to record reservation. Please try again.");
        }

        // Update slot status to RESERVED
        slotDAO.updateStatus(slotId, "RESERVED");

        // Decrement area capacity
        areaDAO.updateAvailableCapacity(areaId, -1);

        // Send confirmation announcement / notification
        Notification notif = new Notification(0, "Booking Confirmed: " + bookingCode,
                "Your slot " + slotNumber + " at " + areaName + " has been confirmed for " + eventTitle + ".",
                "INFO", "CUSTOMER", LocalDateTime.now());
        notificationDAO.insert(notif);

        return reservation;
    }

    public synchronized boolean cancelBooking(int reservationId) throws Exception {
        Reservation res = null;
        for (Reservation r : reservationDAO.findAll()) {
            if (r.getReservationId() == reservationId) {
                res = r;
                break;
            }
        }

        if (res == null) {
            throw new Exception("Reservation not found.");
        }

        if ("CANCELLED".equalsIgnoreCase(res.getStatus())) {
            throw new Exception("Reservation is already cancelled.");
        }

        boolean updated = reservationDAO.updateStatus(reservationId, "CANCELLED");
        if (updated) {
            // Free the slot
            slotDAO.updateStatus(res.getSlotId(), "AVAILABLE");
            // Increment available capacity
            areaDAO.updateAvailableCapacity(res.getAreaId(), 1);

            Notification notif = new Notification(0, "Booking Cancelled: " + res.getBookingCode(),
                    "Your parking reservation for slot " + res.getSlotNumber() + " was cancelled successfully.",
                    "WARNING", "CUSTOMER", LocalDateTime.now());
            notificationDAO.insert(notif);
            return true;
        }
        return false;
    }

    private String generateUniqueBookingCode() {
        int randNum = 1000 + random.nextInt(9000);
        char letter = (char) ('A' + random.nextInt(26));
        return "CRISS-2026-" + letter + randNum;
    }
}
