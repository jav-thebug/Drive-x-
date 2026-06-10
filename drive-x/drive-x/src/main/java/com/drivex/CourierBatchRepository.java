package com.drivex;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CourierBatchRepository 
    extends MongoRepository<CourierBatch, String> {

    // Open batches — jisme abhi add ho sakta hai
    List<CourierBatch> findByStatus(String status);

    // Same zone ki open batch dhundo
    List<CourierBatch> findByStatusAndPickupZoneAndDropZone(
        String status, int pickupZone, int dropZone
    );

        List<CourierBatch> findByStatusInAndPickupZoneAndDropZone(
        List<String> statuses, int pickupZone, int dropZone
    );
    
    // SJF — sabse choti distance wali batch pehle
    List<CourierBatch> findByStatusOrderByTotalDistanceKmAsc(String status);
}