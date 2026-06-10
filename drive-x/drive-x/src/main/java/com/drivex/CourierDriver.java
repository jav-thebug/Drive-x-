package com.drivex;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "courier_drivers")
public class CourierDriver {

    @Id
    private String id;

    private String name;
    private String status        = "available"; // available | busy
    private int    queuePosition = 0;
    private int    totalDeliveries = 0;

    private String        currentBatchId;
    private LocalDateTime lastReturnedAt;

    public CourierDriver() {}

    public CourierDriver(String name, int queuePosition) {
        this.name          = name;
        this.queuePosition = queuePosition;
    }

    public void incrementDeliveries() { this.totalDeliveries++; }

    // ─── Getters & Setters ─────────────────────────
    public String getId()                              { return id; }
    public String getName()                            { return name; }
    public String getStatus()                          { return status; }
    public void   setStatus(String s)                  { this.status = s; }
    public int    getQueuePosition()                   { return queuePosition; }
    public void   setQueuePosition(int p)              { this.queuePosition = p; }
    public int    getTotalDeliveries()                 { return totalDeliveries; }
    public String getCurrentBatchId()                  { return currentBatchId; }
    public void   setCurrentBatchId(String b)          { this.currentBatchId = b; }
    public LocalDateTime getLastReturnedAt()           { return lastReturnedAt; }
    public void   setLastReturnedAt(LocalDateTime t)   { this.lastReturnedAt = t; }
}