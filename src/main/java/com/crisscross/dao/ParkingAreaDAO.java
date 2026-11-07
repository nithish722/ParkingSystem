package com.crisscross.dao;

import com.crisscross.database.DBConnection;
import com.crisscross.database.MockDatabaseStore;
import com.crisscross.models.ParkingArea;
import com.crisscross.models.ParkingSlot;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ParkingAreaDAO {
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();

    public List<ParkingArea> findAll() {
        List<ParkingArea> list = new ArrayList<>();
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT * FROM parking_areas ORDER BY area_id ASC";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(syncAreaWithLiveSlots(mapResultSetToArea(rs)));
                }
                return list;
            } catch (SQLException e) {
                System.err.println("[ParkingAreaDAO] SQL Error: " + e.getMessage());
            }
        }
        for (ParkingArea area : MockDatabaseStore.getInstance().getParkingAreas().values()) {
            list.add(syncAreaWithLiveSlots(area));
        }
        list.sort(Comparator.comparingInt(ParkingArea::getAreaId));
        return list;
    }

    public ParkingArea findById(int areaId) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT * FROM parking_areas WHERE area_id = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, areaId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return syncAreaWithLiveSlots(mapResultSetToArea(rs));
                }
            } catch (SQLException e) {
                System.err.println("[ParkingAreaDAO] SQL Error: " + e.getMessage());
            }
        }
        return syncAreaWithLiveSlots(MockDatabaseStore.getInstance().getParkingAreas().get(areaId));
    }

    public ParkingArea findByCode(String code) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "SELECT * FROM parking_areas WHERE code = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, code);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return syncAreaWithLiveSlots(mapResultSetToArea(rs));
                }
            } catch (SQLException e) {
                System.err.println("[ParkingAreaDAO] SQL Error: " + e.getMessage());
            }
        }
        for (ParkingArea pa : MockDatabaseStore.getInstance().getParkingAreas().values()) {
            if (pa.getCode() != null && pa.getCode().equalsIgnoreCase(code)) {
                return syncAreaWithLiveSlots(pa);
            }
        }
        return null;
    }

    private ParkingArea syncAreaWithLiveSlots(ParkingArea area) {
        if (area == null) return null;
        try {
            List<ParkingSlot> slots = slotDAO.findByArea(area.getAreaId());
            if ((slots == null || slots.isEmpty()) && area.getTotalCapacity() > 0) {
                ensureSlotsProvisionedForArea(area);
                slots = slotDAO.findByArea(area.getAreaId());
            }
            if (slots != null && !slots.isEmpty()) {
                int total = slots.size();
                int avail = (int) slots.stream().filter(ParkingSlot::isAvailable).count();
                area.setTotalCapacity(total);
                area.setAvailableCapacity(avail);
            }
        } catch (Exception ignored) {}
        return area;
    }

    /**
     * Ensures that a ParkingArea has its corresponding ParkingSlot entries generated
     * up to area.getTotalCapacity() so that Status Monitor, Slot Management, and Customer
     * views always display interactive slots immediately after adding or resizing an area.
     */
    public void ensureSlotsProvisionedForArea(ParkingArea area) {
        if (area == null || area.getAreaId() <= 0) return;

        int targetCapacity = Math.max(1, area.getTotalCapacity());
        List<ParkingSlot> existingSlots = slotDAO.findByArea(area.getAreaId());
        if (existingSlots.size() >= targetCapacity) {
            return;
        }

        String rawCode = (area.getCode() != null && !area.getCode().trim().isEmpty())
                ? area.getCode().trim().toUpperCase(Locale.ROOT)
                : ("Z" + area.getAreaId());
        String codePrefix = rawCode.replaceAll("[^A-Z0-9]", "");
        if (codePrefix.isEmpty()) {
            codePrefix = "Z" + area.getAreaId();
        }

        Set<String> usedNumbers = new HashSet<>();
        int existingCars = 0;
        int existingBikes = 0;
        for (ParkingSlot s : existingSlots) {
            if (s.getSlotNumber() != null) {
                usedNumbers.add(s.getSlotNumber().toUpperCase(Locale.ROOT));
            }
            if ("BIKE".equalsIgnoreCase(s.getSlotType())) {
                existingBikes++;
            } else {
                existingCars++;
            }
        }

        String vType = area.getVehicleType() != null ? area.getVehicleType().trim().toUpperCase(Locale.ROOT) : "BOTH";
        int targetCars;
        int targetBikes;
        if ("CAR".equals(vType)) {
            targetCars = targetCapacity;
            targetBikes = 0;
        } else if ("BIKE".equals(vType)) {
            targetCars = 0;
            targetBikes = targetCapacity;
        } else {
            targetCars = (targetCapacity + 1) / 2;
            targetBikes = targetCapacity - targetCars;
        }

        int carsToAdd = Math.max(0, targetCars - existingCars);
        int bikesToAdd = Math.max(0, targetBikes - existingBikes);
        int remainingNeeded = (targetCapacity - existingSlots.size()) - (carsToAdd + bikesToAdd);
        if (remainingNeeded > 0) {
            if ("BIKE".equals(vType)) {
                bikesToAdd += remainingNeeded;
            } else {
                carsToAdd += remainingNeeded;
            }
        }

        int nextCarIdx = existingCars + 1;
        for (int i = 0; i < carsToAdd; i++) {
            String slotNum;
            do {
                slotNum = String.format(Locale.US, "%s-C%02d", codePrefix, nextCarIdx++);
            } while (usedNumbers.contains(slotNum));
            usedNumbers.add(slotNum);

            ParkingSlot slot = new ParkingSlot(0, area.getAreaId(), slotNum, "CAR", "Ground Level", "AVAILABLE");
            slot.setAreaName(area.getName());
            slotDAO.insert(slot);
        }

        int nextBikeIdx = existingBikes + 1;
        for (int i = 0; i < bikesToAdd; i++) {
            String slotNum;
            do {
                slotNum = String.format(Locale.US, "%s-B%02d", codePrefix, nextBikeIdx++);
            } while (usedNumbers.contains(slotNum));
            usedNumbers.add(slotNum);

            ParkingSlot slot = new ParkingSlot(0, area.getAreaId(), slotNum, "BIKE", "North Bay", "AVAILABLE");
            slot.setAreaName(area.getName());
            slotDAO.insert(slot);
        }
    }

    public boolean insert(ParkingArea area) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "INSERT INTO parking_areas (name, code, vehicle_type, total_capacity, available_capacity, location_desc, hourly_rate, nearest_venue, walking_time_mins, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, area.getName());
                ps.setString(2, area.getCode());
                ps.setString(3, area.getVehicleType());
                ps.setInt(4, area.getTotalCapacity());
                ps.setInt(5, area.getAvailableCapacity());
                ps.setString(6, area.getLocationDesc());
                ps.setDouble(7, area.getHourlyRate());
                ps.setString(8, area.getNearestVenue());
                ps.setInt(9, area.getWalkingTimeMins());
                ps.setString(10, area.getStatus());
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (rs.next()) area.setAreaId(rs.getInt(1));
                    }
                    ensureSlotsProvisionedForArea(area);
                    syncAreaWithLiveSlots(area);
                    return true;
                }
            } catch (SQLException e) {
                System.err.println("[ParkingAreaDAO] Insert Error: " + e.getMessage());
                return false;
            }
        }

        int newId = MockDatabaseStore.getInstance().nextAreaId();
        area.setAreaId(newId);
        MockDatabaseStore.getInstance().getParkingAreas().put(newId, area);
        ensureSlotsProvisionedForArea(area);
        syncAreaWithLiveSlots(area);
        MockDatabaseStore.getInstance().saveToDisk();
        return true;
    }

    public boolean update(ParkingArea area) {
        if (DBConnection.getInstance().isMySQLConnected()) {
            String sql = "UPDATE parking_areas SET name = ?, code = ?, vehicle_type = ?, total_capacity = ?, available_capacity = ?, location_desc = ?, hourly_rate = ?, nearest_venue = ?, walking_time_mins = ?, status = ? WHERE area_id = ?";
            try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, area.getName());
                ps.setString(2, area.getCode());
                ps.setString(3, area.getVehicleType());
                ps.setInt(4, area.getTotalCapacity());
                ps.setInt(5, area.getAvailableCapacity());
                ps.setString(6, area.getLocationDesc());
                ps.setDouble(7, area.getHourlyRate());
                ps.setString(8, area.getNearestVenue());
                ps.setInt(9, area.getWalkingTimeMins());
                ps.setString(10, area.getStatus());
                ps.setInt(11, area.getAreaId());
                boolean ok = ps.executeUpdate() > 0;
                if (ok) {
                    ensureSlotsProvisionedForArea(area);
                    syncAreaWithLiveSlots(area);
                }
                return ok;
            } catch (SQLException e) {
                System.err.println("[ParkingAreaDAO] Update Error: " + e.getMessage());
            }
        }
        MockDatabaseStore.getInstance().getParkingAreas().put(area.getAreaId(), area);
        ensureSlotsProvisionedForArea(area);
        syncAreaWithLiveSlots(area);
        MockDatabaseStore.getInstance().saveToDisk();
        return true;
    }

    public boolean updateAvailableCapacity(int areaId, int delta) {
        ParkingArea area = findById(areaId);
        if (area != null) {
            int newCap = Math.max(0, Math.min(area.getTotalCapacity(), area.getAvailableCapacity() + delta));
            area.setAvailableCapacity(newCap);
            return update(area);
        }
        return false;
    }

    private ParkingArea mapResultSetToArea(ResultSet rs) throws SQLException {
        return new ParkingArea(
                rs.getInt("area_id"),
                rs.getString("name"),
                rs.getString("code"),
                rs.getString("vehicle_type"),
                rs.getInt("total_capacity"),
                rs.getInt("available_capacity"),
                rs.getString("location_desc"),
                rs.getDouble("hourly_rate"),
                rs.getString("nearest_venue"),
                rs.getInt("walking_time_mins"),
                rs.getString("status")
        );
    }
}
