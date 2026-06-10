package com.drivex;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RideQueueService {

    private final RideRequestRepository requestRepo;
    private final DriverRepository      driverRepo;
    private final QueueStateRepository  queueRepo;

    // Premium lane max 60% of total queue
    private static final double PREMIUM_LANE_CAP = 0.60;

    // Anti-starvation: 2 consecutive premium ke baad force normal
    private static final int CONSECUTIVE_PREMIUM_LIMIT = 2;
    private static final int FORCED_NORMAL_COUNT        = 2;

    // Hard cap: 12 users per queue
    private static final int HARD_CAP = 12;

    public RideQueueService(RideRequestRepository requestRepo,
                            DriverRepository driverRepo,
                            QueueStateRepository queueRepo) {
        this.requestRepo = requestRepo;
        this.driverRepo  = driverRepo;
        this.queueRepo   = queueRepo;
    }

    // ─────────────────────────────────────────────
    // 1. CONFIRM RIDE — main entry point
    // ─────────────────────────────────────────────
    public synchronized Map<String, Object> confirmRide(
            String username, String rideType, boolean isPremium,
            String pickup, String destination,
            double pickupLat, double pickupLon,
            double destLat, double destLon, double distanceKm) {

        // Hard cap check
        long totalWaiting = requestRepo.countByRideTypeAndStatus(rideType, "waiting");
        if (totalWaiting >= HARD_CAP) {
            // Doosri queues suggest karo
            return Map.of(
                "success", false,
                "message", "Queue full! Try another vehicle type.",
                "suggestAlternatives", true
            );
        }

        // Fare calculate karo
        double baseFare    = calculateBaseFare(rideType, distanceKm);
        double premiumFare = Math.round(baseFare * 1.8 * 100.0) / 100.0;

        // Naya request banao
        RideRequest req = new RideRequest(
            username, rideType, isPremium,
            pickup, destination,
            pickupLat, pickupLon, destLat, destLon, distanceKm
        );
        req.setBaseFare(baseFare);
        req.setPremiumFare(isPremium ? premiumFare : baseFare);
        req.setFinalFare(isPremium ? premiumFare : baseFare);

        // Lane assign karo (premium 60% cap check)
        assignLane(req, rideType);

        // Ride duration estimate: avg speed 25 km/h city mein
        int rideMins = (int) Math.ceil((distanceKm / 25.0) * 60);
        req.setRideDurationMinutes(Math.max(1, rideMins));

        // MongoDB mein save karo
        requestRepo.save(req);

        // Queue position aur ETA calculate karo
        int queuePos = calculateQueuePosition(req);
        double waitEta = calculateWaitEta(rideType, queuePos);
        req.setQueuePosition(queuePos);
        req.setEstimatedWaitMinutes(waitEta);
        requestRepo.save(req);

        // Available driver hai toh turant assign karo
      // BAAD MEIN (replace karo)
Driver driver = findNextDriverRoundRobin(rideType);
if (driver != null) {
    RideRequest nextUser = findNextUserForDriver(rideType);
    if (nextUser != null) {
        assignDriverToRequest(driver, nextUser);
        // agar nextUser != req, toh req waiting mein rahega
    } else {
        assignDriverToRequest(driver, req);
    }
}

       Map<String, Object> result = new java.util.HashMap<>();
result.put("success",        true);
result.put("requestId",      req.getId());
result.put("status",         req.getStatus());
result.put("lane",           req.getLane());
result.put("isPremium",      isPremium);
result.put("baseFare",       baseFare);
result.put("finalFare",      req.getFinalFare());
result.put("queuePosition",  queuePos);
result.put("estimatedWait",  waitEta);
result.put("rideDurationSeconds", req.getRideDurationMinutes());
result.put("driverAssigned", driver != null);
result.put("driverName",     driver != null ? driver.getName() : null);
return result;
    }

    // ─────────────────────────────────────────────
    // 2. LANE ASSIGNMENT — 60% premium cap
    // ─────────────────────────────────────────────
    private void assignLane(RideRequest req, String rideType) {
        if (!req.isPremium()) {
            req.setLane("normal");
            return;
        }

        long totalWaiting   = requestRepo.countByRideTypeAndStatus(rideType, "waiting");
        long premiumWaiting = requestRepo.countByRideTypeAndStatusAndLane(rideType, "waiting", "premium");

        // Agar total 0 hai division se bachao
        double premiumRatio = totalWaiting == 0 ? 0
            : (double) premiumWaiting / totalWaiting;

        if (premiumRatio >= PREMIUM_LANE_CAP) {
            // Premium lane full — normal lane mein daalo, fare premium rakho
            req.setLane("normal");
            req.setPremiumOverflow(true);
        } else {
            req.setLane("premium");
        }
    }

    // ─────────────────────────────────────────────
    // 3. ROUND ROBIN DRIVER SELECTION
    // ─────────────────────────────────────────────
    private Driver findNextDriverRoundRobin(String rideType) {
        List<Driver> allDrivers = driverRepo.findByRideType(rideType);
        if (allDrivers.isEmpty()) return null;

        QueueState state = getOrCreateQueueState(rideType);
        int lastIdx = state.getLastAssignedDriverIndex();
        int n       = allDrivers.size();

        // Round Robin: lastIdx se aage se dhundo
        for (int i = 1; i <= n; i++) {
            int idx    = (lastIdx + i) % n;
            Driver d   = allDrivers.get(idx);

            if ("available".equals(d.getStatus())) {
                // RR pointer update karo
                state.setLastAssignedDriverIndex(idx);
                queueRepo.save(state);
                return d;
            }
        }
        return null; // koi available nahi
    }

    // ─────────────────────────────────────────────
    // 4. ANTI-STARVATION — next user decide karo
    // ─────────────────────────────────────────────
    private RideRequest findNextUserForDriver(String rideType) {
        QueueState state = getOrCreateQueueState(rideType);

        // Case 1: Forced normal remaining hai
        if (state.getForcedNormalRemaining() > 0) {
            Optional<RideRequest> normalUser = requestRepo
                .findFirstByRideTypeAndStatusAndLaneOrderByRequestedAtAsc(
                    rideType, "waiting", "normal"
                );
            if (normalUser.isPresent()) {
                state.setForcedNormalRemaining(
                    state.getForcedNormalRemaining() - 1
                );
                queueRepo.save(state);
                return normalUser.get();
            }
        }

        // Case 2: 2 consecutive premium limit reach ho gaya
        if (state.getConsecutivePremiumServed() >= CONSECUTIVE_PREMIUM_LIMIT) {
            // Force normal serve karo
            Optional<RideRequest> normalUser = requestRepo
                .findFirstByRideTypeAndStatusAndLaneOrderByRequestedAtAsc(
                    rideType, "waiting", "normal"
                );
            if (normalUser.isPresent()) {
                state.setConsecutivePremiumServed(0);
                state.setForcedNormalRemaining(FORCED_NORMAL_COUNT - 1);
                queueRepo.save(state);
                return normalUser.get();
            }
        }

        // Case 3: Normal flow — premium pehle
        Optional<RideRequest> premiumUser = requestRepo
            .findFirstByRideTypeAndStatusAndLaneOrderByRequestedAtAsc(
                rideType, "waiting", "premium"
            );
        if (premiumUser.isPresent()) {
            state.setConsecutivePremiumServed(
                state.getConsecutivePremiumServed() + 1
            );
            queueRepo.save(state);
            return premiumUser.get();
        }

        // Case 4: Premium koi nahi — normal FCFS
        state.setConsecutivePremiumServed(0);
        queueRepo.save(state);
        return requestRepo
            .findFirstByRideTypeAndStatusAndLaneOrderByRequestedAtAsc(
                rideType, "waiting", "normal"
            )
            .orElse(null);
    }

    // ─────────────────────────────────────────────
    // 5. DRIVER ASSIGN KARO
    // ─────────────────────────────────────────────
    private void assignDriverToRequest(Driver driver, RideRequest req) {
        // Discount calculate karo (aging)
        double discount = calculateDiscount(req);
        double finalFare = req.isPremium()
            ? Math.round(req.getPremiumFare() * (1 - discount / 100) * 100.0) / 100.0
            : Math.round(req.getBaseFare()    * (1 - discount / 100) * 100.0) / 100.0;

        req.setDiscountPercent(discount);
        req.setFinalFare(finalFare);
        req.setSavedAmount(Math.round((req.isPremium()
            ? req.getPremiumFare() : req.getBaseFare()) - finalFare));
        req.setStatus("assigned");
        req.setSimulationStartedAt(LocalDateTime.now());
        req.setSimulationTriggered(true);
        req.setAssignedDriverId(driver.getId());
        req.setAssignedDriverName(driver.getName());
        req.setAssignedAt(LocalDateTime.now());
        requestRepo.save(req);

        driver.setStatus("busy");
        driver.setCurrentRequestId(req.getId());
        driverRepo.save(driver);
    }

    // ─────────────────────────────────────────────
    // 6. TRIP COMPLETE — driver free karo + next assign
    // ─────────────────────────────────────────────
    public synchronized Map<String, Object> completeRide(String requestId) {
        RideRequest req = requestRepo.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Request not found"));

        if ("completed".equals(req.getStatus())) {
            return Map.of(
                "success",  true,
                "message",  "Ride already completed",
                "requestId", requestId
            );
        }


    // NULL CHECK PEHLE
    if (req.getAssignedDriverId() == null) {
        req.setStatus("completed");
        req.setCompletedAt(LocalDateTime.now());
        requestRepo.save(req);
        return Map.of("success", true, "message", "Ride completed (no driver)", "requestId", requestId);
    }

        req.setStatus("completed");
        req.setCompletedAt(LocalDateTime.now());
        requestRepo.save(req);

        // Avg duration update karo
        if (req.getAssignedAt() != null) {
            long mins = ChronoUnit.MINUTES.between(
                req.getAssignedAt(), req.getCompletedAt()
            );
            QueueState state = getOrCreateQueueState(req.getRideType());
            state.updateAvgDuration(mins);
            queueRepo.save(state);
        }


Driver driver = driverRepo.findById(req.getAssignedDriverId()).orElse(null);

        if (driver != null) {
            driver.incrementRides();

            // Inter-city driver → cooldown
            if ("InterCity".equals(driver.getRideType())) {
                driver.setStatus("resting");
                driver.setCooldownUntil(LocalDateTime.now().plusMinutes(15));
                driver.setCurrentRequestId(null);
                driverRepo.save(driver);
            } else {
                driver.setStatus("available");
                driver.setCurrentRequestId(null);
                driverRepo.save(driver);

                // Next waiting user ko assign karo
                RideRequest nextUser = findNextUserForDriver(driver.getRideType());
                if (nextUser != null) {
                    Driver nextDriver = findNextDriverRoundRobin(driver.getRideType());
                    if (nextDriver != null) {
                        assignDriverToRequest(nextDriver, nextUser);
                    }
                }
            }
        }

        return Map.of(
            "success",  true,
            "message",  "Ride completed",
            "requestId", requestId
        );
    }

    // ─────────────────────────────────────────────
    // 7. AGING — DISCOUNT CALCULATE KARO
    // ─────────────────────────────────────────────
    private double calculateDiscount(RideRequest req) {
        long waitMinutes = ChronoUnit.MINUTES.between(
            req.getRequestedAt(), LocalDateTime.now()
        );

        if (req.isPremium()) {
            // Har 5 min = 5%, max 20%
            return Math.min((waitMinutes / 5) * 5.0, 20.0);
        } else {
            // Har 10 min = 5%, max 15%
            return Math.min((waitMinutes / 10) * 5.0, 15.0);
        }
    }

    // ─────────────────────────────────────────────
    // 8. FARE CALCULATE KARO
    // ─────────────────────────────────────────────
    private double calculateBaseFare(String rideType, double distanceKm) {
        int currentHour = java.time.LocalTime.now().getHour();
        boolean isPeak  = (currentHour >= 8 && currentHour < 10)
                       || (currentHour >= 17 && currentHour < 20);
        double peakMult = isPeak ? 1.5 : 1.0;

        double base, perKm;
        switch (rideType) {
            case "Bike"      -> { base = 30;  perKm = 15; }
            case "Auto"      -> { base = 50;  perKm = 25; }
            case "CarAC"     -> { base = 100; perKm = 45; }
            case "InterCity" -> { base = 500; perKm = 60; }
            default          -> { base = 80;  perKm = 35; } // Car
        }
        return Math.round((base + perKm * distanceKm) * peakMult * 100.0) / 100.0;
    }

    // ─────────────────────────────────────────────
    // 9. DYNAMIC ETA
    // ─────────────────────────────────────────────
private int calculateQueuePosition(RideRequest req) {
    long ahead = requestRepo.countByRideTypeAndStatusAndRequestedAtBefore(
        req.getRideType(),
        "waiting",
        req.getRequestedAt()
    );
    return (int) ahead + 1;
}

    private double calculateWaitEta(String rideType, int queuePosition) {
        QueueState state     = getOrCreateQueueState(rideType);
        long totalDrivers    = driverRepo.findByRideTypeAndStatus(rideType, "available").size()
                             + driverRepo.findByRideTypeAndStatus(rideType, "busy").size();
        if (totalDrivers == 0) totalDrivers = 5; // fallback

        double avgDuration   = state.getAvgRideDurationMinutes();
        return Math.round((queuePosition * avgDuration) / totalDrivers * 10.0) / 10.0;
    }

    // ─────────────────────────────────────────────
    // 10. COOLDOWN SCHEDULER — har minute check karo
    // ─────────────────────────────────────────────
    @Scheduled(fixedRate = 60000)
    public void checkCooldowns() {
        List<Driver> resting = driverRepo.findByStatus("resting");
        for (Driver d : resting) {
            if (d.getCooldownUntil() != null
                    && LocalDateTime.now().isAfter(d.getCooldownUntil())) {
                d.setStatus("available");
                d.setCooldownUntil(null);
                driverRepo.save(d);
                // Queue mein koi wait kar raha ho toh assign karo
                RideRequest next = findNextUserForDriver(d.getRideType());
                if (next != null) assignDriverToRequest(d, next);
            }
        }
    }

    // Naya scheduler — har 5 sec mein waiting rides check karo
@Scheduled(fixedRate = 5000)
public synchronized void assignWaitingRides() {
    List<String> rideTypes = List.of("Car", "Bike", "Auto", "CarAC", "InterCity");
    for (String rideType : rideTypes) {
        Driver driver = findNextDriverRoundRobin(rideType);
        if (driver != null) {
            RideRequest nextUser = findNextUserForDriver(rideType);
            if (nextUser != null) {
                assignDriverToRequest(driver, nextUser);
            }
        }
    }
}

    // ─────────────────────────────────────────────
// 11. RIDE SIMULATION SCHEDULER — har 1 sec check
// ─────────────────────────────────────────────
@Scheduled(fixedRate = 1000)
public synchronized void simulateRideProgress() {
    // Stage 1: assigned → driver_arrived (estimatedWaitMinutes seconds)
    List<RideRequest> assigned = requestRepo.findByStatus("assigned");
    for (RideRequest req : assigned) {
        if (req.getSimulationStartedAt() == null) continue;
        long secsPassed = ChronoUnit.SECONDS.between(
            req.getSimulationStartedAt(), LocalDateTime.now()
        );
int pickupSecs = (int) Math.ceil(req.getEstimatedWaitMinutes() * 60); // minutes → seconds        if (pickupSecs < 1) pickupSecs = 1; // minimum 1 sec
        if (secsPassed >= 10) {
            req.setStatus("driver_arrived");
            requestRepo.save(req);
        }
    }

    // Stage 2: driver_arrived → completed (rideDurationMinutes seconds baad)
    List<RideRequest> arrived = requestRepo.findByStatus("driver_arrived");
    for (RideRequest req : arrived) {
        if (req.getSimulationStartedAt() == null) continue;
        long secsPassed = ChronoUnit.SECONDS.between(
            req.getSimulationStartedAt(), LocalDateTime.now()
        );
        if (secsPassed >= 20) {
            req.setStatus("ride_started");
            requestRepo.save(req);
        }
    }

    List<RideRequest> started = requestRepo.findByStatus("ride_started");
    for (RideRequest req : started) {
        if (req.getSimulationStartedAt() == null) continue;
        long secsPassed = ChronoUnit.SECONDS.between(
            req.getSimulationStartedAt(), LocalDateTime.now()
        );
        int rideSecs = Math.max(1, req.getRideDurationMinutes());
        if (secsPassed >= 20 + rideSecs) {
            completeRide(req.getId());
        }
    }
}



public List<Map<String, Object>> getRecentRides() {
    return requestRepo.findTop5ByOrderByRequestedAtDesc()
        .stream()
        .map(r -> {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("username",    r.getUsername());
            m.put("pickup",      r.getPickup());
            m.put("destination", r.getDestination());
            m.put("status",      r.getStatus());
            m.put("finalFare",   r.getFinalFare());
            m.put("requestedAt", r.getRequestedAt());
            return m;
        })
        .collect(java.util.stream.Collectors.toList());
}

public Map<String, Object> getUserStats(String username) {
    LocalDateTime startOfMonth = LocalDateTime.now()
        .withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
    
    List<RideRequest> monthRides = requestRepo
        .findByUsernameAndMonth(username, startOfMonth);
    
    double totalSaved = monthRides.stream()
        .mapToDouble(RideRequest::getSavedAmount)
        .sum();
    
    Map<String, Object> result = new java.util.HashMap<>();
    result.put("ridesThisMonth", monthRides.size());
    result.put("totalSaved",     totalSaved);
    return result;
}
    // ─────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────
    private QueueState getOrCreateQueueState(String rideType) {
        return queueRepo.findById(rideType)
            .orElseGet(() -> queueRepo.save(new QueueState(rideType)));
    }

    // Ride status fetch (frontend polling ke liye)
    public Map<String, Object> getRideStatus(String requestId) {
    RideRequest req = requestRepo.findById(requestId).orElse(null);
    if (req == null) {
        return Map.of("status", "not_found", "requestId", requestId);
    }

        double discount = calculateDiscount(req);
        double updatedFare = req.isPremium()
            ? Math.round(req.getPremiumFare() * (1 - discount/100) * 100.0) / 100.0
            : req.getFinalFare();

            

        // Driver location
        double driverLat = 0, driverLon = 0;
        String driverName = req.getAssignedDriverName();
        if (req.getAssignedDriverId() != null) {
            Driver d = driverRepo.findById(req.getAssignedDriverId()).orElse(null);
            if (d != null) {
                driverLat = d.getCurrentLat();
                driverLon = d.getCurrentLon();
            }
        }

      Map<String, Object> statusResult = new java.util.HashMap<>();
statusResult.put("requestId",       requestId);
statusResult.put("status",          req.getStatus());
statusResult.put("queuePosition",   req.getQueuePosition());
statusResult.put("estimatedWait",   calculateWaitEta(req.getRideType(), req.getQueuePosition()));
statusResult.put("discountPercent", discount);
statusResult.put("finalFare",       updatedFare);
statusResult.put("savedAmount",     req.isPremium() ? req.getPremiumFare() - updatedFare : 0);
statusResult.put("driverName",      driverName != null ? driverName : "");
statusResult.put("driverLat",       driverLat);
statusResult.put("driverLon",       driverLon);
return statusResult;
    }
}
