package com.crisscross.models;

import java.io.Serializable;

public class ParkingSlot implements Serializable {
    private static final long serialVersionUID = 1L;
    private int slotId;
    private int areaId;
    private String areaName;
    private String slotNumber; // e.g. 'SJT-C01', 'CDMM-B05'
    private String slotType;   // 'CAR', 'BIKE'
    private String floorLevel; // 'Ground Level', 'Lower Deck', etc.
    private String status;     // 'AVAILABLE', 'RESERVED', 'OCCUPIED', 'BLOCKED'

    public ParkingSlot() {
        this.slotType = "CAR";
        this.status = "AVAILABLE";
        this.floorLevel = "Ground Level";
    }

    public ParkingSlot(int slotId, int areaId, String slotNumber, String slotType, String floorLevel, String status) {
        this.slotId = slotId;
        this.areaId = areaId;
        this.slotNumber = slotNumber;
        this.slotType = slotType;
        this.floorLevel = floorLevel;
        this.status = status;
    }

    public int getSlotId() { return slotId; }
    public void setSlotId(int slotId) { this.slotId = slotId; }

    public int getAreaId() { return areaId; }
    public void setAreaId(int areaId) { this.areaId = areaId; }

    public String getAreaName() { return areaName; }
    public void setAreaName(String areaName) { this.areaName = areaName; }

    public String getSlotNumber() { return slotNumber; }
    public void setSlotNumber(String slotNumber) { this.slotNumber = slotNumber; }

    public String getSlotType() { return slotType; }
    public void setSlotType(String slotType) { this.slotType = slotType; }

    public String getFloorLevel() { return floorLevel; }
    public void setFloorLevel(String floorLevel) { this.floorLevel = floorLevel; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isAvailable() {
        return "AVAILABLE".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return slotNumber + " [" + slotType + " - " + status + "]";
    }
}
