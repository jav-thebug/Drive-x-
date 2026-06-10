package com.drivex;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "grocery_requests")
public class GroceryRequest {

    @Id
    private String id;

    private String username;
    private List<String> items;
    private String pickupAddress;
    private String dropAddress;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private int totalItems;
    private String notes;

    public GroceryRequest() {
    }

    public GroceryRequest(String username, List<String> items, String pickupAddress, String dropAddress, String notes) {
        this.username = username;
        this.items = items;
        this.pickupAddress = pickupAddress;
        this.dropAddress = dropAddress;
        this.notes = notes;
        this.totalItems = items == null ? 0 : items.size();
        this.status = "pending";
        this.requestedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getItems() {
        return items;
    }

    public void setItems(List<String> items) {
        this.items = items;
        this.totalItems = items == null ? 0 : items.size();
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getDropAddress() {
        return dropAddress;
    }

    public void setDropAddress(String dropAddress) {
        this.dropAddress = dropAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
