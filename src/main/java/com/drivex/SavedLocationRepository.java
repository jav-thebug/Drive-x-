package com.drivex;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SavedLocationRepository 
    extends MongoRepository<SavedLocation, String> {
    
    List<SavedLocation> findTop5ByUsernameOrderBySavedAtDesc(String username);
}