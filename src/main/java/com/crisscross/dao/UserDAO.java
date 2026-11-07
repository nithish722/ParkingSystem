package com.crisscross.dao;

import com.crisscross.database.DBConnection;
import com.crisscross.database.MockDatabaseStore;
import com.crisscross.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User findByUsernameOrEmail(String identifier) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT * FROM users WHERE (username = ? OR email = ?) AND status = 'ACTIVE' LIMIT 1";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, identifier);
                ps.setString(2, identifier);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToUser(rs);
                    }
                }
            } catch (SQLException e) {
                System.err.println("[UserDAO] SQL Error: " + e.getMessage());
            }
        }

        // Mock Fallback
        for (User u : MockDatabaseStore.getInstance().getUsers().values()) {
            if ((u.getUsername().equalsIgnoreCase(identifier) || u.getEmail().equalsIgnoreCase(identifier))
                    && !"SUSPENDED".equalsIgnoreCase(u.getStatus())) {
                return u;
            }
        }
        return null;
    }

    public User findById(int userId) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT * FROM users WHERE user_id = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToUser(rs);
                    }
                }
            } catch (SQLException e) {
                System.err.println("[UserDAO] SQL Error: " + e.getMessage());
            }
        }
        return MockDatabaseStore.getInstance().getUsers().get(userId);
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT * FROM users ORDER BY user_id ASC";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapResultSetToUser(rs));
                }
                return list;
            } catch (SQLException e) {
                System.err.println("[UserDAO] SQL Error: " + e.getMessage());
            }
        }
        list.addAll(MockDatabaseStore.getInstance().getUsers().values());
        return list;
    }

    public boolean insert(User user) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "INSERT INTO users (full_name, email, username, password, phone, vehicle_number, role, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getFullName());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getUsername());
                ps.setString(4, user.getPassword());
                ps.setString(5, user.getPhone());
                ps.setString(6, user.getVehicleNumber());
                ps.setString(7, user.getRole());
                ps.setString(8, user.getStatus());
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) {
                            user.setUserId(rs.getInt(1));
                        }
                    }
                    return true;
                }
            } catch (SQLException e) {
                System.err.println("[UserDAO] SQL Insert Error: " + e.getMessage());
                return false;
            }
        }

        // Mock store insert
        int newId = MockDatabaseStore.getInstance().nextUserId();
        user.setUserId(newId);
        MockDatabaseStore.getInstance().getUsers().put(newId, user);
        MockDatabaseStore.getInstance().saveToDisk();
        return true;
    }

    public boolean update(User user) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "UPDATE users SET full_name = ?, email = ?, phone = ?, vehicle_number = ?, password = ?, role = ?, status = ? WHERE user_id = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.getFullName());
                ps.setString(2, user.getEmail());
                ps.setString(3, user.getPhone());
                ps.setString(4, user.getVehicleNumber());
                ps.setString(5, user.getPassword());
                ps.setString(6, user.getRole());
                ps.setString(7, user.getStatus());
                ps.setInt(8, user.getUserId());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("[UserDAO] SQL Update Error: " + e.getMessage());
            }
        }
        MockDatabaseStore.getInstance().getUsers().put(user.getUserId(), user);
        MockDatabaseStore.getInstance().saveToDisk();
        return true;
    }

    public boolean updatePassword(int userId, String newPassword) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "UPDATE users SET password = ? WHERE user_id = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newPassword);
                ps.setInt(2, userId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("[UserDAO] Password Update Error: " + e.getMessage());
            }
        }
        User u = MockDatabaseStore.getInstance().getUsers().get(userId);
        if (u != null) {
            u.setPassword(newPassword);
            MockDatabaseStore.getInstance().saveToDisk();
            return true;
        }
        return false;
    }

    public boolean isEmailOrUsernameTaken(String email, String username, int excludeUserId) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT COUNT(*) FROM users WHERE (email = ? OR username = ?) AND user_id != ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email);
                ps.setString(2, username);
                ps.setInt(3, excludeUserId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1) > 0;
                }
            } catch (SQLException ignored) {}
        }

        for (User u : MockDatabaseStore.getInstance().getUsers().values()) {
            if (u.getUserId() != excludeUserId &&
                    (u.getEmail().equalsIgnoreCase(email) || u.getUsername().equalsIgnoreCase(username))) {
                return true;
            }
        }
        return false;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setPhone(rs.getString("phone"));
        u.setVehicleNumber(rs.getString("vehicle_number"));
        u.setRole(rs.getString("role"));
        u.setStatus(rs.getString("status"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) u.setCreatedAt(ts.toLocalDateTime());
        return u;
    }
}
