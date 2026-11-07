package com.crisscross.services;

import com.crisscross.dao.ParkingAreaDAO;
import com.crisscross.dao.ParkingSlotDAO;
import com.crisscross.models.Event;
import com.crisscross.models.ParkingArea;
import com.crisscross.models.ParkingSlot;

import java.util.List;

/**
 * Smart Proximity & Availability Parking Recommendation Engine.
 * Implements CrissCross core rule:
 * Evaluates Event Venue, Vehicle Type (Car vs Bike), and Slot Availability
 * among SJT PARKING, CDMM PARKING, and WOODY'S PARKING.
 */
public class RecommendationService {
    private static RecommendationService instance;
    private final ParkingAreaDAO areaDAO;
    private final ParkingSlotDAO slotDAO;

    public static class RecommendationResult {
        private final ParkingArea primaryArea;
        private final ParkingArea backupArea;
        private final int matchScore; // e.g. 98%
        private final String walkingDistance;
        private final int walkingMinutes;
        private final String recommendationReason;
        private final boolean primaryCongested;
        private final int availableSlotsInPrimary;

        public RecommendationResult(ParkingArea primaryArea, ParkingArea backupArea, int matchScore,
                                    String walkingDistance, int walkingMinutes, String recommendationReason,
                                    boolean primaryCongested, int availableSlotsInPrimary) {
            this.primaryArea = primaryArea;
            this.backupArea = backupArea;
            this.matchScore = matchScore;
            this.walkingDistance = walkingDistance;
            this.walkingMinutes = walkingMinutes;
            this.recommendationReason = recommendationReason;
            this.primaryCongested = primaryCongested;
            this.availableSlotsInPrimary = availableSlotsInPrimary;
        }

        public ParkingArea getPrimaryArea() { return primaryArea; }
        public ParkingArea getBackupArea() { return backupArea; }
        public int getMatchScore() { return matchScore; }
        public String getWalkingDistance() { return walkingDistance; }
        public int getWalkingMinutes() { return walkingMinutes; }
        public String getRecommendationReason() { return recommendationReason; }
        public boolean isPrimaryCongested() { return primaryCongested; }
        public int getAvailableSlotsInPrimary() { return availableSlotsInPrimary; }
    }

    private RecommendationService() {
        this.areaDAO = new ParkingAreaDAO();
        this.slotDAO = new ParkingSlotDAO();
    }

    public static synchronized RecommendationService getInstance() {
        if (instance == null) {
            instance = new RecommendationService();
        }
        return instance;
    }

    /**
     * Compute best and backup parking recommendation based on event venue and vehicle type.
     */
    public RecommendationResult recommend(Event event, String vehicleType) {
        String venue = (event != null && event.getVenue() != null) ? event.getVenue().toLowerCase() : "";
        String vType = (vehicleType != null) ? vehicleType.toUpperCase() : "CAR";

        ParkingArea sjt = areaDAO.findByCode("SJT");
        ParkingArea cdmm = areaDAO.findByCode("CDMM");
        ParkingArea woodys = areaDAO.findByCode("WOODYS");

        // Fallback safety if DB fetch is null
        if (sjt == null || cdmm == null || woodys == null) {
            List<ParkingArea> all = areaDAO.findAll();
            if (!all.isEmpty()) {
                ParkingArea first = all.get(0);
                ParkingArea second = all.size() > 1 ? all.get(1) : first;
                return new RecommendationResult(first, second, 90, "120m", 2, "Nearest open campus parking area.", false, 15);
            }
        }

        ParkingArea primary = sjt;
        ParkingArea backup = cdmm;
        String distance = "50m";
        int walkMins = 1;
        int matchScore = 98;
        String reason;

        if (venue.contains("sjt") || venue.contains("silver") || venue.contains("tech") || venue.contains("tower")) {
            primary = sjt;
            backup = cdmm;
            distance = "50m";
            walkMins = 1;
            matchScore = 99;
            reason = "Directly adjacent to " + event.getVenue() + " (< 1 min walk).";
        } else if (venue.contains("cdmm") || venue.contains("architecture") || venue.contains("design") || venue.contains("hall")) {
            primary = cdmm;
            backup = woodys;
            distance = "65m";
            walkMins = 1;
            matchScore = 97;
            reason = "Direct elevator & ground access to " + event.getVenue() + ".";
        } else if (venue.contains("woody") || venue.contains("ground") || venue.contains("sport") || venue.contains("arena")) {
            primary = woodys;
            backup = sjt;
            distance = "40m";
            walkMins = 1;
            matchScore = 99;
            reason = "Closest designated perimeter bay for " + event.getVenue() + ".";
        } else {
            // General campus event
            primary = sjt;
            backup = cdmm;
            distance = "150m";
            walkMins = 2;
            matchScore = 92;
            reason = "Central campus hub with maximum capacity.";
        }

        // Count free slots in primary area specifically for the requested vehicle type
        int freeSlotsPrimary = countFreeSlotsForType(primary.getAreaId(), vType);
        boolean congested = (freeSlotsPrimary <= 2);

        if (congested) {
            reason += " [High Traffic Notice: Only " + freeSlotsPrimary + " " + vType + " slots left! Consider " + backup.getName() + " backup]";
            matchScore = Math.max(75, matchScore - 15);
        }

        return new RecommendationResult(primary, backup, matchScore, distance, walkMins, reason, congested, freeSlotsPrimary);
    }

    private int countFreeSlotsForType(int areaId, String vehicleType) {
        List<ParkingSlot> slots = slotDAO.findByAreaAndType(areaId, vehicleType);
        int free = 0;
        for (ParkingSlot s : slots) {
            if ("AVAILABLE".equalsIgnoreCase(s.getStatus())) {
                free++;
            }
        }
        return free;
    }
}
