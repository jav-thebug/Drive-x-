package com.drivex;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "drivers")
public class Driver {

    @Id
    private String id;

    private String name;

    // "Bike" / "Auto" / "Car" / "CarAC" / "InterCity"
    private String rideType;

    // "available" / "busy" / "resting"
    private String status;

    private String currentRequestId; // kis user pe assign hai

    private double currentLat;
    private double currentLon;

    // Inter-city cooldown ke liye
    private LocalDateTime cooldownUntil;

    private int totalRidesCompleted;

    // Constructors
    public Driver() {}

    public Driver(String name, String rideType) {
        this.name      = name;
        this.rideType  = rideType;
        this.status    = "available";
        this.currentLat = 24.8607; // default Karachi center
        this.currentLon = 67.0011;
        this.totalRidesCompleted = 0;
    }

    // Getters & Setters
    public String getId()                        { return id; }
    public String getName()                      { return name; }
    public String getRideType()                  { return rideType; }
    public String getStatus()                    { return status; }
    public void   setStatus(String status)       { this.status = status; }
    public String getCurrentRequestId()          { return currentRequestId; }
    public void   setCurrentRequestId(String id) { this.currentRequestId = id; }
    public double getCurrentLat()                { return currentLat; }
    public void   setCurrentLat(double lat)      { this.currentLat = lat; }
    public double getCurrentLon()                { return currentLon; }
    public void   setCurrentLon(double lon)      { this.currentLon = lon; }
    public LocalDateTime getCooldownUntil()      { return cooldownUntil; }
    public void setCooldownUntil(LocalDateTime t){ this.cooldownUntil = t; }
    public int getTotalRidesCompleted()          { return totalRidesCompleted; }
    public void incrementRides()                 { this.totalRidesCompleted++; }
}