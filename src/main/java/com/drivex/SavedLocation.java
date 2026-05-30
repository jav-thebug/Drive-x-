package com.drivex;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "saved_locations")
public class SavedLocation {

    @Id
    private String id;
    private String username;
    private String pickup;
    private String destination;
    private String rideType;
    private int fare;
    private double pickupLat;
private double pickupLon;
private double destinationLat;
private double destinationLon;
private double distanceKm;  // calculated distance
    private LocalDateTime savedAt;

    public SavedLocation() {}

    public SavedLocation(String username, String pickup, 
                         String destination, String rideType, int fare , double pickupLat, double pickupLon,
                         double destinationLat, double destinationLon, double distanceKm) {

        this.username    = username;
        this.pickup      = pickup;
        this.destination = destination;
        this.rideType    = rideType;
        this.fare        = fare;
        this.pickupLat = pickupLat;
        this.pickupLon = pickupLon;
        this.destinationLat = destinationLat;
        this.destinationLon = destinationLon;
        this.distanceKm = distanceKm;
        this.savedAt     = LocalDateTime.now();
    }

    // Getters
    public String getId()          { return id; }
    public String getUsername()    { return username; }
    public String getPickup()      { return pickup; }
    public String getDestination() { return destination; }
    public String getRideType()    { return rideType; }
    public int    getFare()        { return fare; }
    public double getPickupLat()   { return pickupLat; }
    public double getPickupLon()   { return pickupLon; }
    public double getDestinationLat() { return destinationLat; }
    public double getDestinationLon() { return destinationLon; }
    public double getDistanceKm()  { return distanceKm; }
    public LocalDateTime getSavedAt() { return savedAt; }
}