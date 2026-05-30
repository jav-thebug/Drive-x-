package com.drivex;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/locations")
public class LocationController {

    private final SavedLocationRepository locationRepository;

    // application.properties se API key lo
    @Value("${ors.api.key}")
    private String orsApiKey;

    public LocationController(SavedLocationRepository repo) {
        this.locationRepository = repo;
    }

    // OpenRouteService se distance aur ETA lo
    private Map<String, Double> getRouteInfo(double pickupLat, double pickupLon,
                                              double destLat, double destLon) {
        try {
            // API URL banao
            // Note: ORS mein pehle longitude, phir latitude aata hai
            String url = "https://api.openrouteservice.org/v2/directions/driving-car"
                + "?api_key=" + orsApiKey
                + "&start=" + pickupLon + "," + pickupLat
                + "&end="   + destLon   + "," + destLat;

            // HTTP call karo
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);

            // Response se distance aur duration nikalo
            Map features = (Map) ((java.util.List) response.getBody()
                .get("features")).get(0);
            Map properties = (Map) features.get("properties");
            Map summary = (Map) ((java.util.List) properties
                .get("segments")).get(0);

            // Meters → Kilometers
            double distanceKm = Math.round(
                ((Number) summary.get("distance")).doubleValue() / 10.0) / 100.0;

            // Seconds → Minutes
            double etaMinutes = Math.round(
                ((Number) summary.get("duration")).doubleValue() / 60.0);

            return Map.of(
                "distanceKm", distanceKm,
                "etaMinutes", etaMinutes
            );

        } catch (Exception e) {
            // API fail ho toh Haversine fallback use karo
            System.out.println("ORS API failed: " + e.getMessage());
            return Map.of(
                "distanceKm", haversineDistance(pickupLat, pickupLon, destLat, destLon),
                "etaMinutes", 0.0
            );
        }
    }

    // Backup formula agar API fail ho
    private double haversineDistance(double lat1, double lon1,
                                      double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1))
                 * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return Math.round(R * c * 100.0) / 100.0;
    }

    @PostMapping("/save")
    public Map<String, Object> saveTrip(@RequestBody Map<String, String> body) {
        String username       = body.get("username");
        String pickup         = body.get("pickup");
        String destination    = body.get("destination");
        String rideType       = body.get("rideType");
        int    fare           = Integer.parseInt(body.getOrDefault("fare", "0"));
        double pickupLat      = Double.parseDouble(body.getOrDefault("pickupLat", "0"));
        double pickupLon      = Double.parseDouble(body.getOrDefault("pickupLon", "0"));
        double destinationLat = Double.parseDouble(body.getOrDefault("destinationLat", "0"));
        double destinationLon = Double.parseDouble(body.getOrDefault("destinationLon", "0"));

        if (username == null || pickup == null || destination == null) {
            return Map.of("error", "Missing fields");
        }

        // ORS se real distance + ETA lo
        Map<String, Double> routeInfo = getRouteInfo(
            pickupLat, pickupLon, destinationLat, destinationLon
        );

        double distanceKm = routeInfo.get("distanceKm");
        double etaMinutes = routeInfo.get("etaMinutes");

        // MongoDB mein save karo
        locationRepository.save(new SavedLocation(
            username, pickup, destination, rideType, fare,
            pickupLat, pickupLon, destinationLat, destinationLon,
            distanceKm
        ));

        // Peak hours check karo
int currentHour = java.time.LocalTime.now().getHour();
boolean isPeakHour = (currentHour >= 8 && currentHour < 10)   // morning
                  || (currentHour >= 17 && currentHour < 20);  // evening

double peakMultiplier = isPeakHour ? 1.5 : 1.0;

// Fare calculate karo
double baseFare;
double perKmRate;

switch (rideType) {
    case "Bike"   -> { baseFare = 30;  perKmRate = 15; }
    case "Auto"   -> { baseFare = 50;  perKmRate = 25; }
    case "Car AC" -> { baseFare = 100; perKmRate = 45; }
    default       -> { baseFare = 80;  perKmRate = 35; } // Car
}

// Final fare
double calculatedFare = Math.round((baseFare + (perKmRate * distanceKm)) * peakMultiplier);


        // ETA adjust karo ride type ke hisaab se
double etaMultiplier;
switch (rideType) {
    case "Bike"   -> etaMultiplier = 0.70;
    case "Auto"   -> etaMultiplier = 0.85;
    case "Car AC" -> etaMultiplier = 1.10;
    default       -> etaMultiplier = 1.00; // Car
}

double adjustedEta = Math.round(etaMinutes * etaMultiplier);

        // Frontend ko sab kuch bhejo
        return Map.of(
            "message",     "Trip saved",
            "distanceKm",  distanceKm,
            "etaMinutes",  adjustedEta,
                "isPeakHour",   isPeakHour,     // frontend ko batao
    "fare",        calculatedFare,  

            "pickup",      pickup,
            "destination", destination
        );
    }

    @PostMapping("/route")
public Map<String, Object> getRoute(@RequestBody Map<String, String> body) {
    try {
        double pickupLat = Double.parseDouble(body.get("pickupLat"));
        double pickupLon = Double.parseDouble(body.get("pickupLon"));
        double destLat   = Double.parseDouble(body.get("destLat"));
        double destLon   = Double.parseDouble(body.get("destLon"));

        String url = "https://api.openrouteservice.org/v2/directions/driving-car/geojson";

        // Request body banao
        Map<String, Object> requestBody = Map.of(
            "coordinates", List.of(
                List.of(pickupLon, pickupLat),
                List.of(destLon, destLat)
            )
        );

        // ORS ko call karo
        RestTemplate restTemplate = new RestTemplate();
        org.springframework.http.HttpHeaders headers = 
            new org.springframework.http.HttpHeaders();
        headers.set("Authorization", orsApiKey);
        headers.set("Content-Type", "application/json");

        org.springframework.http.HttpEntity<Map<String, Object>> entity = 
            new org.springframework.http.HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        // GeoJSON wapas bhejo frontend ko
        return response.getBody();

    } catch (Exception e) {
        System.out.println("Route fetch failed: " + e.getMessage());
        return Map.of("error", "Route fetch failed");
    }
}

    @GetMapping("/recent/{username}")
    public List<SavedLocation> getRecent(@PathVariable String username) {
        return locationRepository
            .findTop5ByUsernameOrderBySavedAtDesc(username);
    }
}