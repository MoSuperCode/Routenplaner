package org.example.tourplanner.business.service.api;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.config.ConfigurationManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class OpenRouteServiceClient {
    private static final Logger logger = LogManager.getLogger(OpenRouteServiceClient.class);
    private final String apiKey;
    private final String apiUrl;
    private final ObjectMapper objectMapper;

    public OpenRouteServiceClient() {
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        this.apiKey = configManager.getProperty("ors.api.key");
        this.apiUrl = configManager.getProperty("ors.api.url");
        this.objectMapper = new ObjectMapper();

        logger.info("OpenRouteServiceClient initialized with URL: {}", apiUrl);
    }

    /**
     * Gets directions between two locations
     * @param start Starting location (e.g., "Vienna, Austria")
     * @param end Destination location (e.g., "Salzburg, Austria")
     * @param transportType Type of transport (driving-car, cycling-regular, foot-walking)
     * @return RouteInfo containing distance and duration
     */
    public RouteInfo getDirections(String start, String end, String transportType) throws IOException {
        logger.info("Getting directions from {} to {} using {}", start, end, transportType);



        logger.info("Starting route calculation with API key: {}", apiKey);
        // First, we need to geocode the locations to get coordinates
        double[] startCoords = geocode(start);
        double[] endCoords = geocode(end);

        if (startCoords == null) {
            logger.error("Could not geocode start location: {}", start);
        }
        if (endCoords == null) {
            logger.error("Could not geocode end location: {}", end);
        }

        if (startCoords == null || endCoords == null) {
            logger.error("Failed to geocode locations");
            return null;
        }

        // Map transport type to ORS profile
        String profile = mapTransportTypeToProfile(transportType);

        // Build URL for directions API
        String url = apiUrl + "/directions/" + profile + "?api_key=" + apiKey
                + "&start=" + startCoords[0] + "," + startCoords[1]
                + "&end=" + endCoords[0] + "," + endCoords[1];

        // Execute request
        String responseBody = executeRequest(url);
        logger.info("Executing API request to URL: {}", url);
        if (responseBody == null) {
            return null;
        }

        // Parse response
        return parseDirectionsResponse(responseBody);
    }

    /**
     * Geocodes a location string to coordinates
     * @param location Location name (e.g., "Vienna, Austria")
     * @return Array of [longitude, latitude] or null if geocoding failed
     */
    public double[] geocode(String location) throws IOException {
        logger.info("Geocoding location: {}", location);

        String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
        // Korrigierte URL für Geocoding - entfernen Sie "/v2" für Geocoding
        String url = "https://api.openrouteservice.org/geocode/search?text=" + encodedLocation;

        logger.info("Geocoding URL: {}", url);

        String responseBody = executeRequest(url);
        if (responseBody == null) {
            return null;
        }

        // Parse the geocoding response
        JsonNode rootNode = objectMapper.readTree(responseBody);
        if (!rootNode.has("features") || rootNode.get("features").size() == 0) {
            logger.error("No geocoding results found for: {}", location);
            return null;
        }

        JsonNode feature = rootNode.get("features").get(0);
        JsonNode geometry = feature.get("geometry");
        JsonNode coordinates = geometry.get("coordinates");

        double longitude = coordinates.get(0).asDouble();
        double latitude = coordinates.get(1).asDouble();

        logger.info("Geocoded {} to coordinates: [{}, {}]", location, longitude, latitude);
        return new double[] { longitude, latitude };
    }

    /**
     * Maps application transport types to OpenRouteService profiles
     */
    private String mapTransportTypeToProfile(String transportType) {
        switch (transportType.toLowerCase()) {
            case "car":
                return "driving-car";
            case "bicycle":
                return "cycling-regular";
            case "walking":
                return "foot-walking";
            case "public transport":
                return "driving-car"; // ORS doesn't support public transport directly
            default:
                return "driving-car"; // Default
        }
    }

    /**
     * Executes an HTTP GET request and returns the response body
     */
    private String executeRequest(String url) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);

            // Verwenden Sie den Authorization-Header
            request.setHeader("Authorization", apiKey);
            request.setHeader("Accept", "application/json");

            logger.info("Executing GET request to URL: {}", url);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getCode();
                HttpEntity entity = response.getEntity();

                if (statusCode >= 200 && statusCode < 300) {
                    String responseBody = EntityUtils.toString(entity);
                    logger.info("GET request successful, response length: {}", responseBody.length());
                    return responseBody;
                } else {
                    logger.error("GET request failed with status code: {}", statusCode);
                    if (entity != null) {
                        String errorResponse = EntityUtils.toString(entity);
                        logger.error("Error response: {}", errorResponse);
                    }
                    return null;
                }
            }
        } catch (Exception e) {
            logger.error("Error executing GET request", e);
            return null;
        }
    }

    /**
     * Parses the directions API response
     */
    private RouteInfo parseDirectionsResponse(String responseBody) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);

            if (!rootNode.has("routes") || rootNode.get("routes").size() == 0) {
                logger.error("No routes found in API response");
                return null;
            }

            JsonNode route = rootNode.get("routes").get(0);
            JsonNode summary = route.get("summary");

            double distance = summary.get("distance").asDouble() / 1000; // Convert to km
            int duration = (int) (summary.get("duration").asDouble() / 60); // Convert to minutes

            logger.info("Route info: distance={}km, duration={}min", distance, duration);
            return new RouteInfo(distance, duration);
        } catch (Exception e) {
            logger.error("Error parsing directions response", e);
            return null;
        }
    }

    /**
     * Inner class to hold route information
     */
    public static class RouteInfo {
        private final double distance; // in kilometers
        private final int duration; // in minutes

        public RouteInfo(double distance, int duration) {
            this.distance = distance;
            this.duration = duration;
        }

        public double getDistance() {
            return distance;
        }

        public int getDuration() {
            return duration;
        }
    }
}