package com.drivex;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "ride_requests")
public class RideRequest {

    @Id
    private String id;

    private String username;
    private String rideType;

    // Premium system
    private boolean isPremium;
    private String  lane;           // "premium" / "normal"
    private boolean premiumOverflow; // cap full hone pe normal lane mein

    // Status
    // "waiting" / "assigned" / "completed" / "cancelled"
    private String status;

    // FCFS ke liye — jo pehle aaya usse pehle serve karo
    private LocalDateTime requestedAt;
    private LocalDateTime assignedAt;
    private LocalDateTime completedAt;

    // Driver assignment
    private String assignedDriverId;
    private String assignedDriverName;

    // Fare calculation
    private double baseFare;
    private double premiumFare;    // baseFare × 1.8
    private double discountPercent;
    private double finalFare;
    private double savedAmount;

    // Location data
    private String pickup;
    private String destination;
    private double pickupLat;
    private double pickupLon;
    private double destLat;
    private double destLon;
    private double distanceKm;

    private int rideDurationMinutes; // destination tak ka estimated time
    // Queue info (frontend ko dikhane ke liye)
    private int    queuePosition;
    private double estimatedWaitMinutes;

    // Simulation ke liye
private LocalDateTime simulationStartedAt;
private boolean simulationTriggered;

    // Constructors
    public RideRequest() {}

    public RideRequest(String username, String rideType, boolean isPremium,
                       String pickup, String destination,
                       double pickupLat, double pickupLon,
                       double destLat, double destLon, double distanceKm) {
        this.username    = username;
        this.rideType    = rideType;
        this.isPremium   = isPremium;
        this.pickup      = pickup;
        this.destination = destination;
        this.pickupLat   = pickupLat;
        this.pickupLon   = pickupLon;
        this.destLat     = destLat;
        this.destLon     = destLon;
        this.distanceKm  = distanceKm;
        this.status      = "waiting";
        this.requestedAt = LocalDateTime.now();
        this.discountPercent = 0;
        this.premiumOverflow = false;
    }

    // Getters & Setters
    public String getId()                            { return id; }
    public String getUsername()                      { return username; }
    public String getRideType()                      { return rideType; }
    public boolean isPremium()                       { return isPremium; }
    public String getLane()                          { return lane; }
    public void   setLane(String lane)               { this.lane = lane; }
    public boolean isPremiumOverflow()               { return premiumOverflow; }
    public void   setPremiumOverflow(boolean val)    { this.premiumOverflow = val; }
    public String getStatus()                        { return status; }
    public void   setStatus(String status)           { this.status = status; }
    public LocalDateTime getRequestedAt()            { return requestedAt; }
    public LocalDateTime getAssignedAt()             { return assignedAt; }
    public void   setAssignedAt(LocalDateTime t)     { this.assignedAt = t; }
    public LocalDateTime getCompletedAt()            { return completedAt; }
    public void   setCompletedAt(LocalDateTime t)    { this.completedAt = t; }
    public String getAssignedDriverId()              { return assignedDriverId; }
    public void   setAssignedDriverId(String id)     { this.assignedDriverId = id; }
    public String getAssignedDriverName()            { return assignedDriverName; }
    public void   setAssignedDriverName(String name) { this.assignedDriverName = name; }
    public double getBaseFare()                      { return baseFare; }
    public void   setBaseFare(double f)              { this.baseFare = f; }
    public double getPremiumFare()                   { return premiumFare; }
    public void   setPremiumFare(double f)           { this.premiumFare = f; }
    public double getDiscountPercent()               { return discountPercent; }
    public void   setDiscountPercent(double d)       { this.discountPercent = d; }
    public double getFinalFare()                     { return finalFare; }
    public void   setFinalFare(double f)             { this.finalFare = f; }
    public double getSavedAmount()                   { return savedAmount; }
    public void   setSavedAmount(double s)           { this.savedAmount = s; }
    public String getPickup()                        { return pickup; }
    public String getDestination()                   { return destination; }
    public double getPickupLat()                     { return pickupLat; }
    public double getPickupLon()                     { return pickupLon; }
    public double getDestLat()                       { return destLat; }
    public double getDestLon()                       { return destLon; }
    public double getDistanceKm()                    { return distanceKm; }
    public int    getQueuePosition()                 { return queuePosition; }
    public void   setQueuePosition(int p)            { this.queuePosition = p; }
    public double getEstimatedWaitMinutes()          { return estimatedWaitMinutes; }
    public void   setEstimatedWaitMinutes(double m)  { this.estimatedWaitMinutes = m; }
    public LocalDateTime getSimulationStartedAt()           { return simulationStartedAt; }
public void setSimulationStartedAt(LocalDateTime t)     { this.simulationStartedAt = t; }
public boolean isSimulationTriggered()                  { return simulationTriggered; }
public void setSimulationTriggered(boolean b)           { this.simulationTriggered = b; }
public int getRideDurationMinutes()          { return rideDurationMinutes; }
public void setRideDurationMinutes(int m)    { this.rideDurationMinutes = m; }
}