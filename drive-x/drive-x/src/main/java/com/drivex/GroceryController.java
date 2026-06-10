package com.drivex;

import java.util.ArrayList;
import java.util.HashMap;
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
@RequestMapping("/api/grocery")
public class GroceryController {

    private final GroceryService groceryService;

    public GroceryController(GroceryService groceryService) {
        this.groceryService = groceryService;
    }

    @PostMapping("/request")
    public Map<String, Object> newRequest(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> items = body.get("items") instanceof List ? (List<String>) body.get("items") : new ArrayList<>();

        GroceryRequest request = groceryService.createRequest(
            (String) body.get("username"),
            items,
            (String) body.get("pickupAddress"),
            (String) body.get("dropAddress"),
            (String) body.get("notes")
        );

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("requestId", request.getId());
        response.put("status", request.getStatus());
        response.put("totalItems", request.getTotalItems());
        return response;
    }

    @GetMapping("/status/{requestId}")
    public Map<String, Object> getStatus(@PathVariable String requestId) {
        return groceryService.getRequestStatus(requestId);
    }

    @PostMapping("/complete/{requestId}")
    public Map<String, Object> completeRequest(@PathVariable String requestId) {
        return groceryService.completeRequest(requestId);
    }

    @GetMapping("/recent")
    public List<GroceryRequest> getRecent() {
        return groceryService.getRecentRequests();
    }
}
