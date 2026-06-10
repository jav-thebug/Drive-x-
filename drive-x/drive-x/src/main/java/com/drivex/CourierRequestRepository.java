package com.drivex;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CourierRequestRepository 
    extends MongoRepository<CourierRequest, String> {

    List<CourierRequest> findByStatus(String status);

    List<CourierRequest> findByStatusAndPickupZone(String status, int pickupZone);

    List<CourierRequest> findTop5ByOrderByRequestedAtDesc();

    List<CourierRequest> findByUsernameOrderByRequestedAtDesc(String username);
}