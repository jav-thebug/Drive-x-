package com.drivex;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class GroceryService {

    private final GroceryRequestRepository requestRepo;

    public GroceryService(GroceryRequestRepository requestRepo) {
        this.requestRepo = requestRepo;
    }

    public GroceryRequest createRequest(String username, List<String> items, String pickupAddress, String dropAddress, String notes) {
        GroceryRequest request = new GroceryRequest(username, items, pickupAddress, dropAddress, notes);
        return requestRepo.save(request);
    }

    public Map<String, Object> getRequestStatus(String requestId) {
        return requestRepo.findById(requestId)
            .map(req -> {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("requestId", req.getId());
                result.put("status", req.getStatus());
                result.put("username", req.getUsername());
                result.put("totalItems", req.getTotalItems());
                result.put("pickupAddress", req.getPickupAddress());
                result.put("dropAddress", req.getDropAddress());
                return result;
            })
            .orElseGet(() -> {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "Request not found");
                return result;
            });
    }

    public Map<String, Object> completeRequest(String requestId) {
        return requestRepo.findById(requestId)
            .map(req -> {
                req.setStatus("completed");
                req.setCompletedAt(LocalDateTime.now());
                requestRepo.save(req);
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("requestId", req.getId());
                result.put("status", req.getStatus());
                return result;
            })
            .orElseGet(() -> {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "Request not found");
                return result;
            });
    }

    public List<GroceryRequest> getRecentRequests() {
        return requestRepo.findTop5ByOrderByRequestedAtDesc();
    }
}
