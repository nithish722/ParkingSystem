package com.crisscross.services;

import com.crisscross.dao.EventDAO;
import com.crisscross.dao.ParkingAreaDAO;
import com.crisscross.dao.ParkingSlotDAO;
import com.crisscross.dao.ReservationDAO;
import com.crisscross.dao.UserDAO;
import com.crisscross.models.ParkingArea;
import com.crisscross.models.ParkingSlot;
import com.crisscross.models.Reservation;
import com.crisscross.models.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ReportService {
    private static ReportService instance;
    private final ReservationDAO reservationDAO;
    private final ParkingAreaDAO areaDAO;
    private final ParkingSlotDAO slotDAO;
    private final EventDAO eventDAO;
    private final UserDAO userDAO;

    public static class ZoneCapacityDetail {
        public int areaId;
        public String areaName;
        public String areaCode;
        public String nearestVenue;
        public int totalSlots;
        public int filledSlots;
        public int availableSlots;
        public int reservedSlots;
        public int occupiedSlots;
        public int blockedSlots;
        public int carTotal;
        public int carFilled;
        public int carFree;
        public int bikeTotal;
        public int bikeFilled;
        public int bikeFree;
        public List<String> filledSlotBadges = new ArrayList<>();
    }

    public static class SystemStats {
        public int totalEvents;
        public int activeEvents;
        public int totalReservations;
        public int confirmedReservations;
        public int completedReservations;
        public int cancelledReservations;
        public int totalCapacity;
        public int totalAvailable;
        public int totalOccupied; // Total filled (Reserved + Occupied + Blocked)
        public int totalReservedSlots;
        public int totalOccupiedSlots;
        public int totalBlockedSlots;
        public double overallOccupancyRate;
        public int totalCarSlots;
        public int filledCarSlots;
        public int freeCarSlots;
        public int totalBikeSlots;
        public int filledBikeSlots;
        public int freeBikeSlots;
        public int totalUsers;
        public int totalCustomers;
        public int carReservations;
        public int bikeReservations;
        public double totalRevenueCollected;
        public int paidVisitorBookings;
        public int freeVitBookings;
        public Map<String, Integer> areaOccupancyMap = new LinkedHashMap<>();
        public Map<String, Integer> areaTotalMap = new LinkedHashMap<>();
        public List<ZoneCapacityDetail> zoneDetails = new ArrayList<>();
    }

    private ReportService() {
        this.reservationDAO = new ReservationDAO();
        this.areaDAO = new ParkingAreaDAO();
        this.slotDAO = new ParkingSlotDAO();
        this.eventDAO = new EventDAO();
        this.userDAO = new UserDAO();
    }

    public static synchronized ReportService getInstance() {
        if (instance == null) {
            instance = new ReportService();
        }
        return instance;
    }

    public SystemStats getSystemStats() {
        SystemStats s = new SystemStats();

        // 1. Events
        var events = eventDAO.findAll();
        s.totalEvents = events.size();
        s.activeEvents = (int) events.stream().filter(e -> !"CANCELLED".equalsIgnoreCase(e.getStatus())).count();

        // 2. Areas & Live Slot-by-Slot Capacity Calculation
        List<ParkingArea> areas = areaDAO.findAll();
        for (ParkingArea a : areas) {
            List<ParkingSlot> areaSlots = slotDAO.findByArea(a.getAreaId());

            ZoneCapacityDetail z = new ZoneCapacityDetail();
            z.areaId = a.getAreaId();
            z.areaName = a.getName();
            z.areaCode = a.getCode();
            z.nearestVenue = a.getNearestVenue();

            if (areaSlots != null && !areaSlots.isEmpty()) {
                z.totalSlots = areaSlots.size();
                for (ParkingSlot slot : areaSlots) {
                    boolean isCar = "CAR".equalsIgnoreCase(slot.getSlotType());
                    if (isCar) {
                        z.carTotal++;
                        s.totalCarSlots++;
                    } else {
                        z.bikeTotal++;
                        s.totalBikeSlots++;
                    }

                    String st = slot.getStatus() != null ? slot.getStatus().toUpperCase() : "AVAILABLE";
                    if ("AVAILABLE".equals(st)) {
                        z.availableSlots++;
                        if (isCar) {
                            z.carFree++;
                            s.freeCarSlots++;
                        } else {
                            z.bikeFree++;
                            s.freeBikeSlots++;
                        }
                    } else {
                        z.filledSlots++;
                        if (isCar) {
                            z.carFilled++;
                            s.filledCarSlots++;
                        } else {
                            z.bikeFilled++;
                            s.filledBikeSlots++;
                        }

                        if ("RESERVED".equals(st)) {
                            z.reservedSlots++;
                            s.totalReservedSlots++;
                        } else if ("OCCUPIED".equals(st)) {
                            z.occupiedSlots++;
                            s.totalOccupiedSlots++;
                        } else {
                            z.blockedSlots++;
                            s.totalBlockedSlots++;
                        }
                        z.filledSlotBadges.add(slot.getSlotNumber() + " (" + st + ")");
                    }
                }
            } else {
                z.totalSlots = a.getTotalCapacity();
                z.availableSlots = a.getAvailableCapacity();
                z.filledSlots = Math.max(0, z.totalSlots - z.availableSlots);
            }

            s.totalCapacity += z.totalSlots;
            s.totalAvailable += z.availableSlots;
            s.totalOccupied += z.filledSlots;

            s.areaOccupancyMap.put(a.getName(), z.filledSlots);
            s.areaTotalMap.put(a.getName(), z.totalSlots);
            s.zoneDetails.add(z);
        }

        if (s.totalCapacity > 0) {
            s.overallOccupancyRate = ((double) s.totalOccupied / s.totalCapacity) * 100.0;
        }

        // 3. Reservations & Revenue
        List<Reservation> reservations = reservationDAO.findAll();
        s.totalReservations = reservations.size();
        for (Reservation r : reservations) {
            boolean isCancelled = "CANCELLED".equalsIgnoreCase(r.getStatus());
            if ("CONFIRMED".equalsIgnoreCase(r.getStatus()) || "ACTIVE".equalsIgnoreCase(r.getStatus())) {
                s.confirmedReservations++;
            } else if ("COMPLETED".equalsIgnoreCase(r.getStatus())) {
                s.completedReservations++;
            } else if (isCancelled) {
                s.cancelledReservations++;
            }

            if ("CAR".equalsIgnoreCase(r.getVehicleType())) {
                s.carReservations++;
            } else if ("BIKE".equalsIgnoreCase(r.getVehicleType())) {
                s.bikeReservations++;
            }

            if (!isCancelled) {
                if (r.getTotalAmount() > 0.0) {
                    s.totalRevenueCollected += r.getTotalAmount();
                    s.paidVisitorBookings++;
                } else {
                    s.freeVitBookings++;
                }
            }
        }

        // 4. Users
        List<User> users = userDAO.findAll();
        s.totalUsers = users.size();
        s.totalCustomers = (int) users.stream().filter(u -> !"ADMIN".equalsIgnoreCase(u.getRole())).count();

        return s;
    }
}
