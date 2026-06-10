package com.drivex;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class RentService {

    private final RentRequestRepository repo;

    public RentService(RentRequestRepository repo) {
        this.repo = repo;
    }

    public Map<String, Object> newRequest(String username, String carType, int durationHours, double amount,
                                         String contactName, String contactPhone, String contactAddress) {
        RentRequest req = new RentRequest(username, carType, durationHours, amount, contactName, contactPhone, contactAddress);
        RentRequest saved = repo.save(req);

        // For demo: immediately assign a car id and set ongoing
        saved.setAssignedCarId("CAR-" + (int)(Math.random()*9000 + 1000));
        saved.setAssignedDriverName("Driver " + (int)(Math.random()*90 + 10));
        saved.setStatus("ongoing");
        saved.setStartedAt(LocalDateTime.now());
        repo.save(saved);

        Map<String,Object> resp = new HashMap<>();
        resp.put("requestId", saved.getId());
        resp.put("status", saved.getStatus());
        resp.put("carId", saved.getAssignedCarId());
        resp.put("driver", saved.getAssignedDriverName());
        resp.put("contactName", saved.getContactName());
        resp.put("contactPhone", saved.getContactPhone());
        resp.put("contactAddress", saved.getContactAddress());
        return resp;
    }

    public Map<String, Object> getRequestStatus(String id) {
        Map<String,Object> resp = new HashMap<>();
        repo.findById(id).ifPresentOrElse(r -> {
            resp.put("requestId", r.getId());
            resp.put("status", r.getStatus());
            resp.put("carId", r.getAssignedCarId());
            resp.put("driver", r.getAssignedDriverName());
            resp.put("requestedAt", r.getRequestedAt());
            resp.put("durationHours", r.getDurationHours());
            resp.put("amount", r.getAmount());
            resp.put("contactName", r.getContactName());
            resp.put("contactPhone", r.getContactPhone());
            resp.put("contactAddress", r.getContactAddress());
        }, () -> {
            resp.put("error", "not_found");
        });
        return resp;
    }

    public List<RentRequest> recent() {
        return repo.findTop5ByOrderByRequestedAtDesc();
    }
}
