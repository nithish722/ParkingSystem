package com.crisscross.dao;

import com.crisscross.database.DBConnection;
import com.crisscross.database.MockDatabaseStore;
import com.crisscross.models.Reservation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    public List<Reservation> findAll() {
        List<Reservation> list = new ArrayList<>();
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT r.*, u.full_name AS user_name, e.title AS event_title, e.venue AS event_venue, " +
                         "a.name AS area_name, s.slot_number " +
                         "FROM reservations r " +
                         "JOIN users u ON r.user_id = u.user_id " +
                         "JOIN events e ON r.event_id = e.event_id " +
                         "JOIN parking_areas a ON r.area_id = a.area_id " +
                         "JOIN parking_slots s ON r.slot_id = s.slot_id " +
                         "ORDER BY r.booking_time DESC";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapResultSetToReservation(rs));
                }
                return list;
            } catch (SQLException e) {
                System.err.println("[ReservationDAO] SQL Error: " + e.getMessage());
            }
        }
        list.addAll(MockDatabaseStore.getInstance().getReservations().values());
        list.sort((a, b) -> b.getBookingTime().compareTo(a.getBookingTime()));
        return list;
    }

    public List<Reservation> findByUserId(int userId) {
        List<Reservation> list = new ArrayList<>();
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT r.*, u.full_name AS user_name, e.title AS event_title, e.venue AS event_venue, " +
                         "a.name AS area_name, s.slot_number " +
                         "FROM reservations r " +
                         "JOIN users u ON r.user_id = u.user_id " +
                         "JOIN events e ON r.event_id = e.event_id " +
                         "JOIN parking_areas a ON r.area_id = a.area_id " +
                         "JOIN parking_slots s ON r.slot_id = s.slot_id " +
                         "WHERE r.user_id = ? " +
                         "ORDER BY r.booking_time DESC";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToReservation(rs));
                    }
                }
                return list;
            } catch (SQLException e) {
                System.err.println("[ReservationDAO] SQL Error: " + e.getMessage());
            }
        }
        for (Reservation r : MockDatabaseStore.getInstance().getReservations().values()) {
            if (r.getUserId() == userId) {
                list.add(r);
            }
        }
        list.sort((a, b) -> b.getBookingTime().compareTo(a.getBookingTime()));
        return list;
    }

    public Reservation findByBookingCode(String code) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT r.*, u.full_name AS user_name, e.title AS event_title, e.venue AS event_venue, " +
                         "a.name AS area_name, s.slot_number " +
                         "FROM reservations r " +
                         "JOIN users u ON r.user_id = u.user_id " +
                         "JOIN events e ON r.event_id = e.event_id " +
                         "JOIN parking_areas a ON r.area_id = a.area_id " +
                         "JOIN parking_slots s ON r.slot_id = s.slot_id " +
                         "WHERE r.booking_code = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapResultSetToReservation(rs);
                }
            } catch (SQLException e) {
                System.err.println("[ReservationDAO] SQL Error: " + e.getMessage());
            }
        }
        for (Reservation r : MockDatabaseStore.getInstance().getReservations().values()) {
            if (r.getBookingCode().equalsIgnoreCase(code)) return r;
        }
        return null;
    }

    public boolean insert(Reservation r) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "INSERT INTO reservations (booking_code, user_id, event_id, area_id, slot_id, vehicle_type, vehicle_number, duration_hours, total_amount, status, notes) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, r.getBookingCode());
                ps.setInt(2, r.getUserId());
                ps.setInt(3, r.getEventId());
                ps.setInt(4, r.getAreaId());
                ps.setInt(5, r.getSlotId());
                ps.setString(6, r.getVehicleType());
                ps.setString(7, r.getVehicleNumber());
                ps.setInt(8, r.getDurationHours());
                ps.setDouble(9, r.getTotalAmount());
                ps.setString(10, r.getStatus());
                ps.setString(11, r.getNotes());
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) r.setReservationId(rs.getInt(1));
                    }
                    return true;
                }
            } catch (SQLException e) {
                System.err.println("[ReservationDAO] Insert Error: " + e.getMessage());
                return false;
            }
        }

        int newId = MockDatabaseStore.getInstance().nextReservationId();
        r.setReservationId(newId);
        MockDatabaseStore.getInstance().getReservations().put(newId, r);
        MockDatabaseStore.getInstance().saveToDisk();
        return true;
    }

    public boolean updateStatus(int reservationId, String status) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "UPDATE reservations SET status = ? WHERE reservation_id = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, status);
                ps.setInt(2, reservationId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("[ReservationDAO] UpdateStatus Error: " + e.getMessage());
            }
        }
        Reservation r = MockDatabaseStore.getInstance().getReservations().get(reservationId);
        if (r != null) {
            r.setStatus(status);
            MockDatabaseStore.getInstance().saveToDisk();
            return true;
        }
        return false;
    }

    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setReservationId(rs.getInt("reservation_id"));
        r.setBookingCode(rs.getString("booking_code"));
        r.setUserId(rs.getInt("user_id"));
        r.setEventId(rs.getInt("event_id"));
        r.setAreaId(rs.getInt("area_id"));
        r.setSlotId(rs.getInt("slot_id"));
        r.setVehicleType(rs.getString("vehicle_type"));
        r.setVehicleNumber(rs.getString("vehicle_number"));
        Timestamp ts = rs.getTimestamp("booking_time");
        if (ts != null) r.setBookingTime(ts.toLocalDateTime());
        r.setDurationHours(rs.getInt("duration_hours"));
        r.setTotalAmount(rs.getDouble("total_amount"));
        r.setStatus(rs.getString("status"));
        r.setNotes(rs.getString("notes"));

        // Joined fields
        try { r.setUserName(rs.getString("user_name")); } catch (SQLException ignored) {}
        try { r.setEventTitle(rs.getString("event_title")); } catch (SQLException ignored) {}
        try { r.setEventVenue(rs.getString("event_venue")); } catch (SQLException ignored) {}
        try { r.setAreaName(rs.getString("area_name")); } catch (SQLException ignored) {}
        try { r.setSlotNumber(rs.getString("slot_number")); } catch (SQLException ignored) {}

        return r;
    }
}
