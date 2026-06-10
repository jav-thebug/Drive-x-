package com.drivex;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/ride")
public class RideController {

    private final RideQueueService queueService;

    public RideController(RideQueueService queueService) {
        this.queueService = queueService;
    }

    // User ne ride confirm ki
    @PostMapping("/confirm")
    public Map<String, Object> confirmRide(@RequestBody Map<String, Object> body) {
        return queueService.confirmRide(
            (String)  body.get("username"),
            (String)  body.get("rideType"),
            (Boolean) body.getOrDefault("isPremium", false),
            (String)  body.get("pickup"),
            (String)  body.get("destination"),
            toDouble(body.get("pickupLat")),
            toDouble(body.get("pickupLon")),
            toDouble(body.get("destLat")),
            toDouble(body.get("destLon")),
            toDouble(body.get("distanceKm"))
        );
    }

    // Frontend polling — status check
    @GetMapping("/status/{requestId}")
    public Map<String, Object> getStatus(@PathVariable String requestId) {
        return queueService.getRideStatus(requestId);
    }

    // Trip complete (driver ne complete kiya)
    @PostMapping("/complete/{requestId}")
    public Map<String, Object> completeRide(@PathVariable String requestId) {
        return queueService.completeRide(requestId);
    }
@GetMapping("/recent")
public List<Map<String, Object>> getRecentRides() {
    return queueService.getRecentRides();
}

@GetMapping("/stats/{username}")
public Map<String, Object> getUserStats(@PathVariable String username) {
    return queueService.getUserStats(username);
}
    private double toDouble(Object val) {
        if (val == null) return 0.0;
        return Double.parseDouble(val.toString());
    }
}