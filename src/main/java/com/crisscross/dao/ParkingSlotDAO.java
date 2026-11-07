package com.crisscross.dao;

import com.crisscross.database.DBConnection;
import com.crisscross.database.MockDatabaseStore;
import com.crisscross.models.ParkingArea;
import com.crisscross.models.ParkingSlot;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ParkingSlotDAO {

    public List<ParkingSlot> findAll() {
        List<ParkingSlot> list = new ArrayList<>();
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT s.*, a.name AS area_name FROM parking_slots s " +
                         "JOIN parking_areas a ON s.area_id = a.area_id ORDER BY s.slot_id ASC";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapResultSetToSlot(rs));
                }
                return list;
            } catch (SQLException e) {
                System.err.println("[ParkingSlotDAO] SQL Error: " + e.getMessage());
            }
        }
        for (ParkingSlot s : MockDatabaseStore.getInstance().getParkingSlots().values()) {
            enrichAreaName(s);
            list.add(s);
        }
        list.sort(Comparator.comparingInt(ParkingSlot::getSlotId));
        return list;
    }

    public List<ParkingSlot> findByArea(int areaId) {
        List<ParkingSlot> list = new ArrayList<>();
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT s.*, a.name AS area_name FROM parking_slots s " +
                         "JOIN parking_areas a ON s.area_id = a.area_id WHERE s.area_id = ? ORDER BY s.slot_id ASC";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, areaId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToSlot(rs));
                    }
                }
                return list;
            } catch (SQLException e) {
                System.err.println("[ParkingSlotDAO] SQL Error: " + e.getMessage());
            }
        }
        for (ParkingSlot s : MockDatabaseStore.getInstance().getParkingSlots().values()) {
            if (s.getAreaId() == areaId) {
                enrichAreaName(s);
                list.add(s);
            }
        }
        list.sort(Comparator.comparingInt(ParkingSlot::getSlotId));
        return list;
    }

    public List<ParkingSlot> findByAreaAndType(int areaId, String vehicleType) {
        List<ParkingSlot> list = new ArrayList<>();
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT s.*, a.name AS area_name FROM parking_slots s " +
                         "JOIN parking_areas a ON s.area_id = a.area_id WHERE s.area_id = ? AND s.slot_type = ? ORDER BY s.slot_id ASC";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, areaId);
                ps.setString(2, vehicleType);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToSlot(rs));
                    }
                }
                return list;
            } catch (SQLException e) {
                System.err.println("[ParkingSlotDAO] SQL Error: " + e.getMessage());
            }
        }
        for (ParkingSlot s : MockDatabaseStore.getInstance().getParkingSlots().values()) {
            if (s.getAreaId() == areaId && s.getSlotType() != null && s.getSlotType().equalsIgnoreCase(vehicleType)) {
                enrichAreaName(s);
                list.add(s);
            }
        }
        list.sort(Comparator.comparingInt(ParkingSlot::getSlotId));
        return list;
    }

    public ParkingSlot findById(int slotId) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT s.*, a.name AS area_name FROM parking_slots s " +
                         "JOIN parking_areas a ON s.area_id = a.area_id WHERE s.slot_id = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, slotId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return mapResultSetToSlot(rs);
                }
            } catch (SQLException e) {
                System.err.println("[ParkingSlotDAO] SQL Error: " + e.getMessage());
            }
        }
        ParkingSlot s = MockDatabaseStore.getInstance().getParkingSlots().get(slotId);
        enrichAreaName(s);
        return s;
    }

    public boolean insert(ParkingSlot slot) {
        enrichAreaName(slot);
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "INSERT INTO parking_slots (area_id, slot_number, slot_type, floor_level, status) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, slot.getAreaId());
                ps.setString(2, slot.getSlotNumber());
                ps.setString(3, slot.getSlotType());
                ps.setString(4, slot.getFloorLevel());
                ps.setString(5, slot.getStatus());
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) slot.setSlotId(rs.getInt(1));
                    }
                    return true;
                }
            } catch (SQLException e) {
                System.err.println("[ParkingSlotDAO] Insert Error: " + e.getMessage());
                return false;
            }
        }

        int newId = MockDatabaseStore.getInstance().nextSlotId();
        slot.setSlotId(newId);
        MockDatabaseStore.getInstance().getParkingSlots().put(newId, slot);
        MockDatabaseStore.getInstance().saveToDisk();
        return true;
    }

    public boolean update(ParkingSlot slot) {
        enrichAreaName(slot);
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "UPDATE parking_slots SET area_id = ?, slot_number = ?, slot_type = ?, floor_level = ?, status = ? WHERE slot_id = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, slot.getAreaId());
                ps.setString(2, slot.getSlotNumber());
                ps.setString(3, slot.getSlotType());
                ps.setString(4, slot.getFloorLevel());
                ps.setString(5, slot.getStatus());
                ps.setInt(6, slot.getSlotId());
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("[ParkingSlotDAO] Update Error: " + e.getMessage());
            }
        }
        MockDatabaseStore.getInstance().getParkingSlots().put(slot.getSlotId(), slot);
        MockDatabaseStore.getInstance().saveToDisk();
        return true;
    }

    public boolean updateStatus(int slotId, String newStatus) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "UPDATE parking_slots SET status = ? WHERE slot_id = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, newStatus);
                ps.setInt(2, slotId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("[ParkingSlotDAO] UpdateStatus Error: " + e.getMessage());
            }
        }
        ParkingSlot s = MockDatabaseStore.getInstance().getParkingSlots().get(slotId);
        if (s != null) {
            s.setStatus(newStatus);
            MockDatabaseStore.getInstance().saveToDisk();
            return true;
        }
        return false;
    }

    public boolean delete(int slotId) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "DELETE FROM parking_slots WHERE slot_id = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, slotId);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("[ParkingSlotDAO] Delete Error: " + e.getMessage());
            }
        }
        boolean removed = MockDatabaseStore.getInstance().getParkingSlots().remove(slotId) != null;
        if (removed) MockDatabaseStore.getInstance().saveToDisk();
        return removed;
    }

    private void enrichAreaName(ParkingSlot slot) {
        if (slot == null) return;
        ParkingArea area = MockDatabaseStore.getInstance().getParkingAreas().get(slot.getAreaId());
        if (area != null && area.getName() != null) {
            slot.setAreaName(area.getName());
        } else if (slot.getAreaName() == null || slot.getAreaName().isEmpty()) {
            slot.setAreaName("Zone " + slot.getAreaId());
        }
    }

    private ParkingSlot mapResultSetToSlot(ResultSet rs) throws SQLException {
        ParkingSlot s = new ParkingSlot(
                rs.getInt("slot_id"),
                rs.getInt("area_id"),
                rs.getString("slot_number"),
                rs.getString("slot_type"),
                rs.getString("floor_level"),
                rs.getString("status")
        );
        try {
            s.setAreaName(rs.getString("area_name"));
        } catch (SQLException ignored) {}
        return s;
    }
}
