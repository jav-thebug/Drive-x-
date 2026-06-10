// QueueStateRepository.java
package com.drivex;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface QueueStateRepository extends MongoRepository<QueueState, String> {
    // _id = rideType, isliye findById("Bike") kaam karega
}