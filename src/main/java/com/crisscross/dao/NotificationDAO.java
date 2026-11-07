package com.crisscross.dao;

import com.crisscross.database.DBConnection;
import com.crisscross.database.MockDatabaseStore;
import com.crisscross.models.Notification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public List<Notification> findAll() {
        List<Notification> list = new ArrayList<>();
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT * FROM notifications ORDER BY created_at DESC";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapResultSetToNotification(rs));
                }
                return list;
            } catch (SQLException e) {
                System.err.println("[NotificationDAO] SQL Error: " + e.getMessage());
            }
        }
        list.addAll(MockDatabaseStore.getInstance().getNotifications().values());
        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return list;
    }

    public List<Notification> findForRole(String role) {
        List<Notification> list = new ArrayList<>();
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT * FROM notifications WHERE target_role = 'ALL' OR target_role = ? ORDER BY created_at DESC";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, role);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToNotification(rs));
                    }
                }
                return list;
            } catch (SQLException e) {
                System.err.println("[NotificationDAO] SQL Error: " + e.getMessage());
            }
        }
        for (Notification n : MockDatabaseStore.getInstance().getNotifications().values()) {
            if ("ALL".equalsIgnoreCase(n.getTargetRole()) || n.getTargetRole().equalsIgnoreCase(role)) {
                list.add(n);
            }
        }
        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return list;
    }

    public boolean insert(Notification notification) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "INSERT INTO notifications (title, message, priority, target_role) VALUES (?, ?, ?, ?)";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, notification.getTitle());
                ps.setString(2, notification.getMessage());
                ps.setString(3, notification.getPriority());
                ps.setString(4, notification.getTargetRole());
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) notification.setNotificationId(rs.getInt(1));
                    }
                    return true;
                }
            } catch (SQLException e) {
                System.err.println("[NotificationDAO] Insert Error: " + e.getMessage());
                return false;
            }
        }

        int newId = MockDatabaseStore.getInstance().nextNotificationId();
        notification.setNotificationId(newId);
        MockDatabaseStore.getInstance().getNotifications().put(newId, notification);
        MockDatabaseStore.getInstance().saveToDisk();
        return true;
    }

    public boolean delete(int notificationId) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "DELETE FROM notifications WHERE notification_id = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, notificationId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("[NotificationDAO] Delete Error: " + e.getMessage());
            }
        }
        boolean removed = MockDatabaseStore.getInstance().getNotifications().remove(notificationId) != null;
        if (removed) MockDatabaseStore.getInstance().saveToDisk();
        return removed;
    }

    private Notification mapResultSetToNotification(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationId(rs.getInt("notification_id"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setPriority(rs.getString("priority"));
        n.setTargetRole(rs.getString("target_role"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) n.setCreatedAt(ts.toLocalDateTime());
        return n;
    }
}
