package com.crisscross.models;

import java.io.Serializable;

public class ParkingArea implements Serializable {
    private static final long serialVersionUID = 1L;
    private int areaId;
    private String name; // 'SJT PARKING', 'CDMM PARKING', 'WOODY\'S PARKING'
    private String code; // 'SJT', 'CDMM', 'WOODYS'
    private String vehicleType; // 'CAR', 'BIKE', 'BOTH'
    private int totalCapacity;
    private int availableCapacity;
    private String locationDesc;
    private double hourlyRate;
    private String nearestVenue;
    private int walkingTimeMins;
    private String status; // 'OPEN', 'FULL', 'MAINTENANCE'

    public ParkingArea() {
        this.vehicleType = "BOTH";
        this.status = "OPEN";
        this.hourlyRate = 0.00;
        this.walkingTimeMins = 2;
    }

    public ParkingArea(int areaId, String name, String code, String vehicleType, int totalCapacity,
                       int availableCapacity, String locationDesc, double hourlyRate,
                       String nearestVenue, int walkingTimeMins, String status) {
        this.areaId = areaId;
        this.name = name;
        this.code = code;
        this.vehicleType = vehicleType;
        this.totalCapacity = totalCapacity;
        this.availableCapacity = availableCapacity;
        this.locationDesc = locationDesc;
        this.hourlyRate = hourlyRate;
        this.nearestVenue = nearestVenue;
        this.walkingTimeMins = walkingTimeMins;
        this.status = status;
    }

    public int getAreaId() { return areaId; }
    public void setAreaId(int areaId) { this.areaId = areaId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public int getTotalCapacity() { return totalCapacity; }
    public void setTotalCapacity(int totalCapacity) { this.totalCapacity = totalCapacity; }

    public int getAvailableCapacity() { return availableCapacity; }
    public void setAvailableCapacity(int availableCapacity) { this.availableCapacity = availableCapacity; }

    public String getLocationDesc() { return locationDesc; }
    public void setLocationDesc(String locationDesc) { this.locationDesc = locationDesc; }

    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }

    public String getNearestVenue() { return nearestVenue; }
    public void setNearestVenue(String nearestVenue) { this.nearestVenue = nearestVenue; }

    public int getWalkingTimeMins() { return walkingTimeMins; }
    public void setWalkingTimeMins(int walkingTimeMins) { this.walkingTimeMins = walkingTimeMins; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getOccupiedCount() {
        return Math.max(0, totalCapacity - availableCapacity);
    }

    public double getOccupancyRate() {
        if (totalCapacity == 0) return 0.0;
        return (double) getOccupiedCount() / totalCapacity * 100.0;
    }

    @Override
    public String toString() {
        return name + " (" + availableCapacity + "/" + totalCapacity + " free)";
    }
}
