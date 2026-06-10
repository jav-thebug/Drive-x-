package com.drivex;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface GroceryRequestRepository extends MongoRepository<GroceryRequest, String> {

    List<GroceryRequest> findTop5ByUsernameOrderByRequestedAtDesc(String username);

    List<GroceryRequest> findByStatus(String status);

    List<GroceryRequest> findTop5ByOrderByRequestedAtDesc();
}
