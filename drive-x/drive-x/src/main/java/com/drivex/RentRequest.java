package com.drivex;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "rent_requests")
public class RentRequest {

    @Id
    private String id;

    private String username;
    private String carType; // "AC" or "Non-AC"
    private int durationHours; // hours rented
    private double amount;

    // Personal info
    private String contactName;
    private String contactPhone;
    private String contactAddress;

    private String status; // pending / ongoing / completed / cancelled
    private LocalDateTime requestedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private String assignedCarId;
    private String assignedDriverName;

    public RentRequest() {}

    public RentRequest(String username, String carType, int durationHours, double amount) {
        this(username, carType, durationHours, amount, null, null, null);
    }

    public RentRequest(String username, String carType, int durationHours, double amount,
                       String contactName, String contactPhone, String contactAddress) {
        this.username = username;
        this.carType = carType;
        this.durationHours = durationHours;
        this.amount = amount;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactAddress = contactAddress;
        this.status = "pending";
        this.requestedAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getCarType() { return carType; }
    public int getDurationHours() { return durationHours; }
    public double getAmount() { return amount; }
    public String getStatus() { return status; }
    public void setStatus(String s) { this.status = s; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime t) { this.startedAt = t; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime t) { this.completedAt = t; }
    public String getAssignedCarId() { return assignedCarId; }
    public void setAssignedCarId(String id) { this.assignedCarId = id; }
    public String getAssignedDriverName() { return assignedDriverName; }
    public void setAssignedDriverName(String n) { this.assignedDriverName = n; }
    public String getContactName() { return contactName; }
    public void setContactName(String n) { this.contactName = n; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String p) { this.contactPhone = p; }
    public String getContactAddress() { return contactAddress; }
    public void setContactAddress(String a) { this.contactAddress = a; }
}
