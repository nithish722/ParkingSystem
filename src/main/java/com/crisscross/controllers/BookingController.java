package com.crisscross.controllers;

import com.crisscross.dao.ReservationDAO;
import com.crisscross.models.Reservation;
import com.crisscross.services.BookingService;
import com.crisscross.ui.ScreenManager;

import java.util.List;

public class BookingController {
    private final BookingService bookingService = BookingService.getInstance();
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final ScreenManager screenManager = ScreenManager.getInstance();

    public List<Reservation> getUserBookings(int userId) {
        return reservationDAO.findByUserId(userId);
    }

    public List<Reservation> getAllBookings() {
        return reservationDAO.findAll();
    }

    public Reservation createReservation(int userId, String userName, int eventId, String eventTitle,
                                         String eventVenue, int areaId, String areaName, int slotId,
                                         String slotNumber, String vehicleType, String vehicleNumber,
                                         int durationHours, double totalAmount, String notes) throws Exception {
        Reservation res = bookingService.createBooking(userId, userName, eventId, eventTitle, eventVenue,
                areaId, areaName, slotId, slotNumber, vehicleType, vehicleNumber, durationHours, totalAmount, notes);
        screenManager.showBookingConfirmation(res);
        return res;
    }

    public boolean cancelReservation(int reservationId) throws Exception {
        return bookingService.cancelBooking(reservationId);
    }
}
