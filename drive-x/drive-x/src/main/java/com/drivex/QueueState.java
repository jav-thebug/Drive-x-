package com.drivex;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "queue_state")
public class QueueState {

    // _id = rideType itself, e.g. "Bike", "Auto"
    @Id
    private String rideType;

    // Anti-starvation counter
    // Kitne consecutive premium users serve hue
    private int consecutivePremiumServed;

    // Kitne normal users force serve karne hain abhi
    private int forcedNormalRemaining;

    // Round Robin pointer — kaun sa driver index last assign hua
    private int lastAssignedDriverIndex;

    // Dynamic ETA ke liye — average ride duration track karo
    private double avgRideDurationMinutes;
    private int    totalRidesTracked;

    public QueueState() {}

    public QueueState(String rideType) {
        this.rideType                  = rideType;
        this.consecutivePremiumServed  = 0;
        this.forcedNormalRemaining     = 0;
        this.lastAssignedDriverIndex   = -1; // -1 = start from beginning
        this.avgRideDurationMinutes    = 12; // default assumption
        this.totalRidesTracked         = 0;
    }

    // Getters & Setters
    public String getRideType()                           { return rideType; }
    public int    getConsecutivePremiumServed()           { return consecutivePremiumServed; }
    public void   setConsecutivePremiumServed(int c)      { this.consecutivePremiumServed = c; }
    public int    getForcedNormalRemaining()              { return forcedNormalRemaining; }
    public void   setForcedNormalRemaining(int f)         { this.forcedNormalRemaining = f; }
    public int    getLastAssignedDriverIndex()            { return lastAssignedDriverIndex; }
    public void   setLastAssignedDriverIndex(int i)       { this.lastAssignedDriverIndex = i; }
    public double getAvgRideDurationMinutes()             { return avgRideDurationMinutes; }
    public int    getTotalRidesTracked()                  { return totalRidesTracked; }

    // Running average update karo
    public void updateAvgDuration(double newDurationMinutes) {
        this.avgRideDurationMinutes =
            (avgRideDurationMinutes * totalRidesTracked + newDurationMinutes)
            / (totalRidesTracked + 1);
        this.totalRidesTracked++;
    }
}