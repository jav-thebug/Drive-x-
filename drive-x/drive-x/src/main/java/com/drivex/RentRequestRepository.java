package com.drivex;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface RentRequestRepository extends MongoRepository<RentRequest, String> {
    List<RentRequest> findTop5ByOrderByRequestedAtDesc();
    List<RentRequest> findByUsernameOrderByRequestedAtDesc(String username);
}
