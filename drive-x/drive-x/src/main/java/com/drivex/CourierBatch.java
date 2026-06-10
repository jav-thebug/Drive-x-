package com.drivex;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "courier_batches")
public class CourierBatch {

    @Id
    private String id;

    private int    pickupZone;
    private int    dropZone;
    private String status = "open"; // open → assigned → completed

    private List<String> requestIds      = new ArrayList<>();
    private double       totalDistanceKm = 0.0;

    // ✅ Drop coordinates store karo saving formula ke liye
    private List<double[]> dropCoords = new ArrayList<>();

    private String        assignedDriverId;
    private String        assignedDriverName;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;
    private LocalDateTime simulationStartedAt;

    private static final int MAX_SIZE = 3;

    public CourierBatch(int pickupZone, int dropZone) {
        this.pickupZone = pickupZone;
        this.dropZone   = dropZone;
    }

    public void addRequest(String requestId, double distanceKm,
                           double dropLat, double dropLon) {
        requestIds.add(requestId);
        totalDistanceKm += distanceKm;
        dropCoords.add(new double[]{dropLat, dropLon});
    }

    public boolean isFull() {
        return requestIds.size() >= MAX_SIZE;
    }

    // ─── Getters & Setters ─────────────────────────
    public String       getId()                         { return id; }
    public int          getPickupZone()                 { return pickupZone; }
    public int          getDropZone()                   { return dropZone; }
    public String       getStatus()                     { return status; }
    public void         setStatus(String s)             { this.status = s; }
    public List<String> getRequestIds()                 { return requestIds; }
    public double       getTotalDistanceKm()            { return totalDistanceKm; }
    public void         setTotalDistanceKm(double d)    { this.totalDistanceKm = d; }
    public List<double[]> getDropCoords()               { return dropCoords; }
    public String       getAssignedDriverId()           { return assignedDriverId; }
    public void         setAssignedDriverId(String d)   { this.assignedDriverId = d; }
    public String       getAssignedDriverName()         { return assignedDriverName; }
    public void         setAssignedDriverName(String n) { this.assignedDriverName = n; }
    public LocalDateTime getAssignedAt()                { return assignedAt; }
    public void          setAssignedAt(LocalDateTime t) { this.assignedAt = t; }
    public LocalDateTime getCompletedAt()               { return completedAt; }
    public void          setCompletedAt(LocalDateTime t){ this.completedAt = t; }
    public LocalDateTime getSimulationStartedAt()       { return simulationStartedAt; }
    public void setSimulationStartedAt(LocalDateTime t) { this.simulationStartedAt = t; }
}