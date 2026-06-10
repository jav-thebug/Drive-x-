package com.drivex;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class CourierService {

    private final CourierRequestRepository requestRepo;
    private final CourierBatchRepository   batchRepo;
    private final CourierDriverRepository  driverRepo;

     private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ors.api.key}")
    private String orsApiKey;

    private static final int    MAX_BATCH_SIZE     = 3;
    private static final double PICKUP_RADIUS_KM   = 1.0;
    private static final double MIN_SAVING_KM      = 2.0;

    public CourierService(CourierRequestRepository requestRepo,
                          CourierBatchRepository batchRepo,
                          CourierDriverRepository driverRepo) {
        this.requestRepo = requestRepo;
        this.batchRepo   = batchRepo;
        this.driverRepo  = driverRepo;
    }
 // ─────────────────────────────────────────────
    // 1. NEW REQUEST — distanceKm & eta ORS se aata hai
    // ─────────────────────────────────────────────
    public synchronized Map<String, Object> newRequest(
            String username,
            String pickupAddress, String dropAddress,
            double pickupLat, double pickupLon,
            double dropLat,   double dropLon,
            double weightKg) {                          // ✅ distanceKm parameter hata diya

        // ✅ ORS se distance aur eta lo
        Map<String, Double> route = getRouteInfo(pickupLat, pickupLon, dropLat, dropLon);
        double distanceKm  = route.get("distanceKm");
        double etaMinutes  = route.get("etaMinutes");  // ✅ ab declared hai

        // Request banao
        CourierRequest req = new CourierRequest(
            username, pickupAddress, dropAddress,
            pickupLat, pickupLon, dropLat, dropLon,
            weightKg, distanceKm
        );

        // Zone detect karo
        req.setPickupZone(detectZone(pickupLat, pickupLon));
        req.setDropZone(detectZone(dropLat, dropLon));

        // Fare calculate karo
        double finalFare = req.getBaseFare() + (distanceKm * req.getPerKmRate());
        req.setFinalFare(Math.round(finalFare * 100.0) / 100.0);

        requestRepo.save(req);

        // Batching check karo
        String batchResult = tryBatch(req);

        Map<String, Object> result = new HashMap<>();
        result.put("success",        true);
        result.put("requestId",      req.getId());
        result.put("status",         req.getStatus());
        result.put("weightCategory", req.getWeightCategory());
        result.put("pickupZone",     req.getPickupZone());
        result.put("dropZone",       req.getDropZone());
        result.put("distanceKm",     distanceKm);       // ✅ sirf ek baar
        result.put("etaMinutes",     etaMinutes);       // ✅ properly declared variable
        result.put("finalFare",      req.getFinalFare());
        result.put("batched",        req.getBatchId() != null);
        result.put("batchId",        req.getBatchId());
        result.put("batchInfo",      batchResult);
        return result;
    }


    // ─────────────────────────────────────────────
    // 2. BATCHING LOGIC — 3 conditions check
    // ─────────────────────────────────────────────
    private String tryBatch(CourierRequest newReq) {
        // Same zone ki open batches dhundo
        List<CourierBatch> openBatches = batchRepo
    .findByStatusInAndPickupZoneAndDropZone(
        List.of("open", "assigned"),
        newReq.getPickupZone(), newReq.getDropZone()
    );
    // ✅ Yeh add karo
    System.out.println("=== BATCHING DEBUG ===");
    System.out.println("New request zone: pickup=" + newReq.getPickupZone() + " drop=" + newReq.getDropZone());
    System.out.println("Found batches: " + openBatches.size());


        for (CourierBatch batch : openBatches) {
            if (batch.isFull()) continue;

            // Existing requests fetch karo
            List<CourierRequest> batchRequests = new ArrayList<>();
            for (String rid : batch.getRequestIds()) {
                requestRepo.findById(rid).ifPresent(batchRequests::add);
            }

            // Condition 1: Pickup 1km radius check
            boolean pickupNear = batchRequests.stream().allMatch(r ->
                haversineKm(newReq.getPickupLat(), newReq.getPickupLon(),
                            r.getPickupLat(), r.getPickupLon()) <= PICKUP_RADIUS_KM
            );
            if (!pickupNear) continue;

            // Condition 2: Same drop zone — already guaranteed by query

            // Condition 3: Net saving > 2km
           double individualTotal = batchRequests.stream()
    .mapToDouble(CourierRequest::getDistanceKm).sum()
    + newReq.getDistanceKm();

double maxIndividual = Math.max(
    batchRequests.stream()
        .mapToDouble(CourierRequest::getDistanceKm)
        .max().orElse(0),
    newReq.getDistanceKm()
);

double saving = individualTotal - maxIndividual;

if (saving < MIN_SAVING_KM) continue;

            // Sab conditions pass — batch mein add karo
batch.addRequest(
    newReq.getId(),
    newReq.getDistanceKm(),
    newReq.getPickupLat(),
    newReq.getPickupLon()
);            batchRepo.save(batch);

            newReq.setBatchId(batch.getId());
            newReq.setStatus("batched");
            requestRepo.save(newReq);

            return "Batched! Saving: " + Math.round(saving * 10.0) / 10.0 + " km";
        }

        // Koi match nahi mila — naya batch banao
        CourierBatch newBatch = new CourierBatch(
            newReq.getPickupZone(), newReq.getDropZone()
        );
newBatch.addRequest(
    newReq.getId(),
    newReq.getDistanceKm(),
    newReq.getPickupLat(),
    newReq.getPickupLon()
);
        batchRepo.save(newBatch);

        newReq.setBatchId(newBatch.getId());
        newReq.setStatus("batched");
        requestRepo.save(newReq);

        return "New batch created";
    }

    // ─────────────────────────────────────────────
    // 3. SJF — NEXT BATCH ASSIGN KARO
    // ─────────────────────────────────────────────
    private void assignNextBatch() {
        // Sabse choti distance wali batch lo (SJF)
        List<CourierBatch> waiting = batchRepo
            .findByStatusOrderByTotalDistanceKmAsc("open");

        if (waiting.isEmpty()) return;

        // Round Robin — front of queue driver lo
        List<CourierDriver> available = driverRepo
            .findByStatusOrderByQueuePositionAsc("available");

        if (available.isEmpty()) return;

        CourierDriver driver = available.get(0);
        CourierBatch  batch  = waiting.get(0);

        // Assign karo
        batch.setStatus("assigned");
        batch.setAssignedDriverId(driver.getId());
        batch.setAssignedDriverName(driver.getName());
        batch.setAssignedAt(LocalDateTime.now());
        batch.setSimulationStartedAt(LocalDateTime.now());
        batchRepo.save(batch);

        // Driver busy karo
        driver.setStatus("busy");
        driver.setCurrentBatchId(batch.getId());
        driverRepo.save(driver);

        // Sab requests assigned mark karo
        for (String rid : batch.getRequestIds()) {
            requestRepo.findById(rid).ifPresent(r -> {
                r.setStatus("assigned");
                r.setAssignedDriverId(driver.getId());
                r.setAssignedDriverName(driver.getName());
                r.setAssignedAt(LocalDateTime.now());
                r.setSimulationStartedAt(LocalDateTime.now());
                requestRepo.save(r);
            });
        }
    }

    // ─────────────────────────────────────────────
    // 4. COMPLETE BATCH
    // ─────────────────────────────────────────────
    public synchronized Map<String, Object> completeBatch(String batchId) {
        CourierBatch batch = batchRepo.findById(batchId)
            .orElseThrow(() -> new RuntimeException("Batch not found"));

        if ("completed".equals(batch.getStatus())) {
            return Map.of("success", true, "message", "Already completed");
        }

        batch.setStatus("completed");
        batch.setCompletedAt(LocalDateTime.now());
        batchRepo.save(batch);

        // Sab requests complete karo
        for (String rid : batch.getRequestIds()) {
            requestRepo.findById(rid).ifPresent(r -> {
                r.setStatus("completed");
                r.setCompletedAt(LocalDateTime.now());
                requestRepo.save(r);
            });
        }

        // Driver free karo — queue ke END mein daalo
        if (batch.getAssignedDriverId() != null) {
            driverRepo.findById(batch.getAssignedDriverId()).ifPresent(d -> {
                // Sabse zyada queuePosition find karo
                List<CourierDriver> allDrivers = driverRepo
                    .findAllByOrderByQueuePositionAsc();
                int maxPos = allDrivers.stream()
                    .mapToInt(CourierDriver::getQueuePosition)
                    .max().orElse(0);

                d.setStatus("available");
                d.setCurrentBatchId(null);
                d.setQueuePosition(maxPos + 1);
                d.setLastReturnedAt(LocalDateTime.now());
                d.incrementDeliveries();
                driverRepo.save(d);
            });

            // Next batch assign karo
            assignNextBatch();
        }

        return Map.of("success", true, "message", "Batch completed", "batchId", batchId);
    }

   

 // ─────────────────────────────────────────────
    // 5. ORS ROUTE INFO
    // ─────────────────────────────────────────────
    private Map<String, Double> getRouteInfo(double pickupLat, double pickupLon,
                                              double dropLat,   double dropLon) {
        try {
            String url = "https://api.openrouteservice.org/v2/directions/driving-car"
                    + "?api_key=" + orsApiKey
                    + "&start=" + pickupLon + "," + pickupLat
                    + "&end="   + dropLon   + "," + dropLat;

            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            Map    features   = (Map)  ((List) response.getBody().get("features")).get(0);
            Map    properties = (Map)  features.get("properties");
            Map    segment    = (Map)  ((List) properties.get("segments")).get(0);

            double distanceKm = ((Number) segment.get("distance")).doubleValue() / 1000.0;
            double etaMinutes = ((Number) segment.get("duration")).doubleValue() / 60.0;

            return Map.of(
                "distanceKm", Math.round(distanceKm * 100.0) / 100.0,
                "etaMinutes", (double) Math.round(etaMinutes)
            );

        } catch (Exception e) {
            System.out.println("ORS failed: " + e.getMessage());
            // Fallback: haversine distance, 0 eta
            return Map.of(
                "distanceKm", haversineKm(pickupLat, pickupLon, dropLat, dropLon),
                "etaMinutes", 0.0
            );
        }
    }


    // ─────────────────────────────────────────────
    // 5. SIMULATION SCHEDULER — har 5 sec
    // ─────────────────────────────────────────────
    @Scheduled(fixedRate = 5000)
    public synchronized void simulateDelivery() {
        List<CourierBatch> assigned = batchRepo.findByStatus("assigned");
        for (CourierBatch batch : assigned) {
            if (batch.getSimulationStartedAt() == null) continue;
            long secsPassed = ChronoUnit.SECONDS.between(
                batch.getSimulationStartedAt(), LocalDateTime.now()
            );
            // 60 seconds mein complete
            if (secsPassed >= 60) {
                completeBatch(batch.getId());
            }
        }
    }

    // ─────────────────────────────────────────────
// SCHEDULED ASSIGN — har 10 sec mein SJF
// ─────────────────────────────────────────────
@Scheduled(fixedRate = 10000)
public synchronized void scheduledAssign() {
    assignNextBatch();
}

    // ─────────────────────────────────────────────
    // 6. STATUS CHECK
    // ─────────────────────────────────────────────
    public Map<String, Object> getRequestStatus(String requestId) {
        CourierRequest req = requestRepo.findById(requestId).orElse(null);
        if (req == null) return Map.of("status", "not_found");

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("requestId",      requestId);
        result.put("status",         req.getStatus());
        result.put("batchId",        req.getBatchId());
        result.put("driverName",     req.getAssignedDriverName() != null ? req.getAssignedDriverName() : "");
        result.put("weightCategory", req.getWeightCategory());
        result.put("finalFare",      req.getFinalFare());
        result.put("pickupZone",     req.getPickupZone());
        result.put("dropZone",       req.getDropZone());
        return result;
    }

    // ─────────────────────────────────────────────
    // 7. ZONE DETECTION
    // ─────────────────────────────────────────────
    public int detectZone(double lat, double lon) {
        if (lat >= 24.78 && lat <= 24.83 && lon >= 67.02 && lon <= 67.08) return 1; // DHA, Clifton
        if (lat >= 24.90 && lat <= 24.95 && lon >= 67.08 && lon <= 67.14) return 2; // Gulshan, Johar
        if (lat >= 24.85 && lat <= 24.90 && lon >= 67.01 && lon <= 67.07) return 3; // Saddar, PECHS
        if (lat >= 24.83 && lat <= 24.88 && lon >= 67.14 && lon <= 67.22) return 4; // Malir, Korangi
        if (lat >= 24.97 && lat <= 25.03 && lon >= 67.02 && lon <= 67.10) return 5; // North Karachi
        return 0; // Unknown zone
    }

    
    public Map<String, Object> getCourierStats(String username) {
    List<CourierRequest> allRequests = requestRepo
        .findByUsernameOrderByRequestedAtDesc(username);
    
    long delivered = allRequests.stream()
        .filter(r -> "completed".equals(r.getStatus()))
        .count();
    
    long batched = allRequests.stream()
        .filter(r -> r.getBatchId() != null && "completed".equals(r.getStatus()))
        .count();
    
    Map<String, Object> result = new java.util.HashMap<>();
    result.put("delivered", delivered);
    result.put("batched",   batched);
    return result;
}
      // ─────────────────────────────────────────────
    // 9. HAVERSINE
    // ─────────────────────────────────────────────
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        // ✅ duplicate line hata di
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;  // ✅ sirf ek return
    }

    // Recent deliveries
    public List<Map<String, Object>> getRecentDeliveries() {
        return requestRepo.findTop5ByOrderByRequestedAtDesc()
            .stream()
            .map(r -> {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("username",       r.getUsername());
                m.put("pickupAddress",  r.getPickupAddress());
                m.put("dropAddress",    r.getDropAddress());
                m.put("status",         r.getStatus());
                m.put("finalFare",      r.getFinalFare());
                m.put("weightCategory", r.getWeightCategory());
                m.put("requestedAt",    r.getRequestedAt());
                return m;
            })
            .collect(java.util.stream.Collectors.toList());
    }
}