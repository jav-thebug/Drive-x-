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
@RequestMapping("/api/courier")
public class CourierController {

    private final CourierService courierService;

    public CourierController(CourierService courierService) {
        this.courierService = courierService;
    }

    // Naya parcel request — distanceKm ab nahi chahiye (OSRM se aayega)
   @PostMapping("/request")
    public Map<String, Object> newRequest(@RequestBody Map<String, Object> body) {
        return courierService.newRequest(
                (String) body.get("username"),
                (String) body.get("pickupAddress"),
                (String) body.get("dropAddress"),
                toDouble(body.get("pickupLat")),
                toDouble(body.get("pickupLon")),
                toDouble(body.get("dropLat")),
                toDouble(body.get("dropLon")),
                toDouble(body.get("weightKg"))
        );
    }
    // Status check
    @GetMapping("/status/{requestId}")
    public Map<String, Object> getStatus(@PathVariable String requestId) {
        return courierService.getRequestStatus(requestId);
    }

    // Batch manually complete karo (testing ke liye)
    @PostMapping("/complete/{batchId}")
    public Map<String, Object> completeBatch(@PathVariable String batchId) {
        return courierService.completeBatch(batchId);
    }

    // Recent deliveries
    @GetMapping("/recent")
    public List<Map<String, Object>> getRecent() {
        return courierService.getRecentDeliveries();
    }
//right sidebar
@GetMapping("/stats/{username}")
public Map<String, Object> getStats(@PathVariable String username) {
    return courierService.getCourierStats(username);
}

    // Helper
    private double toDouble(Object val) {
        if (val == null) return 0.0;
        return Double.parseDouble(val.toString());
    }
}