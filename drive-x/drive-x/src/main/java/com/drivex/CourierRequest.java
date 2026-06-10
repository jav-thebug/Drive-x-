package com.drivex;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "courier_requests")
public class CourierRequest {

    @Id
    private String id;

    private String username;
    private String pickupAddress;
    private String dropAddress;

    private double pickupLat, pickupLon;
    private double dropLat,   dropLon;

    private double weightKg;
    private double distanceKm;

    // Weight category auto-set
    private String weightCategory; // "light", "medium", "heavy"
    private double baseFare;
    private double perKmRate;
    private double finalFare;

    // Zone
    private int pickupZone;
    private int dropZone;

    // Status
    private String status = "pending"; // pending → batched → assigned → completed

    // Batch info
    private String batchId;

    // Driver info
    private String assignedDriverId;
    private String assignedDriverName;

    // Timestamps
    private LocalDateTime requestedAt        = LocalDateTime.now();
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;
    private LocalDateTime simulationStartedAt;

    // ─── Constructor ───────────────────────────────
    public CourierRequest(String username,
                          String pickupAddress, String dropAddress,
                          double pickupLat,     double pickupLon,
                          double dropLat,       double dropLon,
                          double weightKg,      double distanceKm) {
        this.username      = username;
        this.pickupAddress = pickupAddress;
        this.dropAddress   = dropAddress;
        this.pickupLat     = pickupLat;
        this.pickupLon     = pickupLon;
        this.dropLat       = dropLat;
        this.dropLon       = dropLon;
        this.weightKg      = weightKg;
        this.distanceKm    = distanceKm;

        // ✅ Weight se fare auto-set
        if (weightKg <= 1.0) {
            this.weightCategory = "light";
            this.baseFare       = 100.0;
            this.perKmRate      = 20.0;
        } else if (weightKg <= 5.0) {
            this.weightCategory = "medium";
            this.baseFare       = 150.0;
            this.perKmRate      = 30.0;
        } else {
            this.weightCategory = "heavy";
            this.baseFare       = 200.0;
            this.perKmRate      = 40.0;
        }
    }

    // ─── Getters & Setters ─────────────────────────
    public String getId()                          { return id; }
    public String getUsername()                    { return username; }
    public String getPickupAddress()               { return pickupAddress; }
    public String getDropAddress()                 { return dropAddress; }
    public double getPickupLat()                   { return pickupLat; }
    public double getPickupLon()                   { return pickupLon; }
    public double getDropLat()                     { return dropLat; }
    public double getDropLon()                     { return dropLon; }
    public double getWeightKg()                    { return weightKg; }
    public double getDistanceKm()                  { return distanceKm; }
    public void   setDistanceKm(double d)          { this.distanceKm = d; }
    public String getWeightCategory()              { return weightCategory; }
    public double getBaseFare()                    { return baseFare; }
    public double getPerKmRate()                   { return perKmRate; }
    public double getFinalFare()                   { return finalFare; }
    public void   setFinalFare(double f)           { this.finalFare = f; }
    public int    getPickupZone()                  { return pickupZone; }
    public void   setPickupZone(int z)             { this.pickupZone = z; }
    public int    getDropZone()                    { return dropZone; }
    public void   setDropZone(int z)               { this.dropZone = z; }
    public String getStatus()                      { return status; }
    public void   setStatus(String s)              { this.status = s; }
    public String getBatchId()                     { return batchId; }
    public void   setBatchId(String b)             { this.batchId = b; }
    public String getAssignedDriverId()            { return assignedDriverId; }
    public void   setAssignedDriverId(String d)    { this.assignedDriverId = d; }
    public String getAssignedDriverName()          { return assignedDriverName; }
    public void   setAssignedDriverName(String n)  { this.assignedDriverName = n; }
    public LocalDateTime getRequestedAt()          { return requestedAt; }
    public LocalDateTime getAssignedAt()           { return assignedAt; }
    public void   setAssignedAt(LocalDateTime t)   { this.assignedAt = t; }
    public LocalDateTime getCompletedAt()          { return completedAt; }
    public void   setCompletedAt(LocalDateTime t)  { this.completedAt = t; }
    public LocalDateTime getSimulationStartedAt()  { return simulationStartedAt; }
    public void setSimulationStartedAt(LocalDateTime t) { this.simulationStartedAt = t; }
}