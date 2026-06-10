// RideRequestRepository.java
package com.drivex;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface RideRequestRepository extends MongoRepository<RideRequest, String> {

    // FCFS — pehle aane wala pehle (requestedAt ascending)
    Optional<RideRequest> findFirstByRideTypeAndStatusAndLaneOrderByRequestedAtAsc(
        String rideType, String status, String lane
    );

    // Queue mein kitne waiting hain
    long countByRideTypeAndStatus(String rideType, String status);

    // Premium lane mein kitne hain (60% cap ke liye)
    long countByRideTypeAndStatusAndLane(String rideType, String status, String lane);

    // User ki recent requests
    List<RideRequest> findTop5ByUsernameOrderByRequestedAtDesc(String username);

    List<RideRequest> findByStatus(String status);

// Recent 5 rides — poore system ki
List<RideRequest> findTop5ByOrderByRequestedAtDesc();

long countByRideTypeAndStatusAndRequestedAtBefore(
    String rideType,
    String status,
    java.time.LocalDateTime requestedAt
);

// User ki is mahine ki rides
@Query("{ 'username': ?0, 'status': 'completed', 'requestedAt': { $gte: ?1 } }")
List<RideRequest> findByUsernameAndMonth(String username, LocalDateTime startOfMonth);    

// Specific request status check
    Optional<RideRequest> findById(String id);
}