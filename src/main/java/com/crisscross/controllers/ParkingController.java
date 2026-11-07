package com.crisscross.controllers;

import com.crisscross.dao.ParkingAreaDAO;
import com.crisscross.dao.ParkingSlotDAO;
import com.crisscross.models.Event;
import com.crisscross.models.ParkingArea;
import com.crisscross.models.ParkingSlot;
import com.crisscross.services.RecommendationService;
import com.crisscross.ui.ScreenManager;

import java.util.List;

public class ParkingController {
    private final ParkingAreaDAO areaDAO = new ParkingAreaDAO();
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final RecommendationService recommendationService = RecommendationService.getInstance();
    private final ScreenManager screenManager = ScreenManager.getInstance();

    public List<ParkingArea> getAllAreas() {
        return areaDAO.findAll();
    }

    public List<ParkingSlot> getSlotsByAreaAndType(int areaId, String vehicleType) {
        return slotDAO.findByAreaAndType(areaId, vehicleType);
    }

    public RecommendationService.RecommendationResult getRecommendation(Event event, String vehicleType) {
        return recommendationService.recommend(event, vehicleType);
    }

    public void navigateToSlotSelection(Event event, ParkingArea area, String vehicleType) {
        screenManager.showSlotSelection(event, area, vehicleType);
    }

    public boolean updateSlotStatus(int slotId, String status) {
        return slotDAO.updateStatus(slotId, status);
    }
}
