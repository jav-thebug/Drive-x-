package com.drivex;

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
@RequestMapping("/api/rent")
public class RentController {

    private final RentService rentService;

    public RentController(RentService rentService) {
        this.rentService = rentService;
    }

    @PostMapping("/request")
    public Map<String, Object> newRequest(@RequestBody Map<String,Object> body) {
        String username = (String) body.getOrDefault("username", "guest");
        String carType = (String) body.getOrDefault("carType", "AC");
        int duration = Integer.parseInt(body.getOrDefault("durationHours", "1").toString());
        double amount = Double.parseDouble(body.getOrDefault("amount", "0").toString());
        String contactName = (String) body.getOrDefault("contactName", null);
        String contactPhone = (String) body.getOrDefault("contactPhone", null);
        String contactAddress = (String) body.getOrDefault("contactAddress", null);
        return rentService.newRequest(username, carType, duration, amount, contactName, contactPhone, contactAddress);
    }

    @GetMapping("/status/{requestId}")
    public Map<String, Object> getStatus(@PathVariable String requestId) {
        return rentService.getRequestStatus(requestId);
    }
}
