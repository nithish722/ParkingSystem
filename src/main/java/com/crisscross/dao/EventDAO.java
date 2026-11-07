package com.crisscross.dao;

import com.crisscross.database.DBConnection;
import com.crisscross.database.MockDatabaseStore;
import com.crisscross.models.Event;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {

    public List<Event> findAll() {
        List<Event> list = new ArrayList<>();
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT * FROM events ORDER BY event_id ASC";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapResultSetToEvent(rs));
                }
                return list;
            } catch (SQLException e) {
                System.err.println("[EventDAO] SQL Error: " + e.getMessage());
            }
        }
        list.addAll(MockDatabaseStore.getInstance().getEvents().values());
        return list;
    }

    public List<Event> findUpcoming() {
        List<Event> list = new ArrayList<>();
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT * FROM events WHERE status != 'CANCELLED' ORDER BY event_date ASC";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapResultSetToEvent(rs));
                }
                return list;
            } catch (SQLException e) {
                System.err.println("[EventDAO] SQL Error: " + e.getMessage());
            }
        }
        for (Event e : MockDatabaseStore.getInstance().getEvents().values()) {
            if (!"CANCELLED".equalsIgnoreCase(e.getStatus())) {
                list.add(e);
            }
        }
        return list;
    }

    public Event findById(int eventId) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT * FROM events WHERE event_id = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, eventId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapResultSetToEvent(rs);
                }
            } catch (SQLException e) {
                System.err.println("[EventDAO] SQL Error: " + e.getMessage());
            }
        }
        return MockDatabaseStore.getInstance().getEvents().get(eventId);
    }

    public boolean insert(Event event) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "INSERT INTO events (title, venue, event_date, event_time, description, organizer, category, expected_attendees, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, event.getTitle());
                ps.setString(2, event.getVenue());
                ps.setString(3, event.getEventDate());
                ps.setString(4, event.getEventTime());
                ps.setString(5, event.getDescription());
                ps.setString(6, event.getOrganizer());
                ps.setString(7, event.getCategory());
                ps.setInt(8, event.getExpectedAttendees());
                ps.setString(9, event.getStatus());
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            event.setEventId(rs.getInt(1));
                        }
                    }
                    return true;
                }
            } catch (SQLException e) {
                System.err.println("[EventDAO] Insert Error: " + e.getMessage());
                return false;
            }
        }

        int newId = MockDatabaseStore.getInstance().nextEventId();
        event.setEventId(newId);
        MockDatabaseStore.getInstance().getEvents().put(newId, event);
        MockDatabaseStore.getInstance().saveToDisk();
        return true;
    }

    public boolean update(Event event) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "UPDATE events SET title = ?, venue = ?, event_date = ?, event_time = ?, description = ?, organizer = ?, category = ?, expected_attendees = ?, status = ? WHERE event_id = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, event.getTitle());
                ps.setString(2, event.getVenue());
                ps.setString(3, event.getEventDate());
                ps.setString(4, event.getEventTime());
                ps.setString(5, event.getDescription());
                ps.setString(6, event.getOrganizer());
                ps.setString(7, event.getCategory());
                ps.setInt(8, event.getExpectedAttendees());
                ps.setString(9, event.getStatus());
                ps.setInt(10, event.getEventId());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("[EventDAO] Update Error: " + e.getMessage());
            }
        }
        MockDatabaseStore.getInstance().getEvents().put(event.getEventId(), event);
        MockDatabaseStore.getInstance().saveToDisk();
        return true;
    }

    public boolean delete(int eventId) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "DELETE FROM events WHERE event_id = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, eventId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("[EventDAO] Delete Error: " + e.getMessage());
            }
        }
        boolean removed = MockDatabaseStore.getInstance().getEvents().remove(eventId) != null;
        if (removed) MockDatabaseStore.getInstance().saveToDisk();
        return removed;
    }

    private Event mapResultSetToEvent(ResultSet rs) throws SQLException {
        Event e = new Event();
        e.setEventId(rs.getInt("event_id"));
        e.setTitle(rs.getString("title"));
        e.setVenue(rs.getString("venue"));
        e.setEventDate(rs.getString("event_date"));
        e.setEventTime(rs.getString("event_time"));
        e.setDescription(rs.getString("description"));
        e.setOrganizer(rs.getString("organizer"));
        e.setCategory(rs.getString("category"));
        e.setExpectedAttendees(rs.getInt("expected_attendees"));
        e.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) e.setCreatedAt(ts.toLocalDateTime());
        return e;
    }
}
