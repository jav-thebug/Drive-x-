// DriverRepository.java
package com.drivex;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface DriverRepository extends MongoRepository<Driver, String> {

    // Ek ride type ke saare drivers
    List<Driver> findByRideType(String rideType);

    // Available drivers sirf
    List<Driver> findByRideTypeAndStatus(String rideType, String status);

    // Cooldown check ke liye
    List<Driver> findByStatus(String status);
}