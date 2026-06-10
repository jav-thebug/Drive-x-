package com.drivex;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CourierDriverRepository 
    extends MongoRepository<CourierDriver, String> {
    
    // Queue ke front wala driver — sabse chota queuePosition
    List<CourierDriver> findByStatusOrderByQueuePositionAsc(String status);
    
    // Sab drivers queue order mein
    List<CourierDriver> findAllByOrderByQueuePositionAsc();
}