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
import java.util.Locale;

public class OpenRouteServiceClient {
    private static final Logger logger = LogManager.getLogger(OpenRouteServiceClient.class);
    private final String apiKey;
    private final ObjectMapper objectMapper;

    public OpenRouteServiceClient() {
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        this.apiKey = configManager.getProperty("ors.api.key");
        this.objectMapper = new ObjectMapper();

        logger.info("OpenRouteServiceClient initialized with API key: {}***",
                apiKey != null ? apiKey.substring(0, Math.min(8, apiKey.length())) : "null");
    }

    /**
     * Gets directions between two locations
     */
    public RouteInfo getDirections(String start, String end, String transportType) throws IOException {
        logger.info("Getting directions from {} to {} using {}", start, end, transportType);

        // First, geocode the locations
        double[] startCoords = geocode(start);
        double[] endCoords = geocode(end);

        if (startCoords == null || endCoords == null) {
            logger.error("Failed to geocode locations");
            return null;
        }

        // Map transport type to ORS profile
        String profile = mapTransportTypeToProfile(transportType);

        // Build URL for directions API - KORRIGIERT
        String url = String.format(Locale.US,
                "https://api.openrouteservice.org/v2/directions/%s?api_key=%s&start=%.6f,%.6f&end=%.6f,%.6f",
                profile, apiKey, startCoords[0], startCoords[1], endCoords[0], endCoords[1]
        );

        logger.info("Directions API URL: {}", url);

        // Execute request
        String responseBody = executeRequest(url);
        if (responseBody == null) {
            return null;
        }

        // Parse response
        return parseDirectionsResponse(responseBody);
    }

    /**
     * Geocodes a location string to coordinates
     */
    public double[] geocode(String location) throws IOException {
        logger.info("Geocoding location: {}", location);

        String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);

        // KORRIGIERTE Geocoding URL
        String url = String.format(
                "https://api.openrouteservice.org/geocode/search?api_key=%s&text=%s",
                apiKey, encodedLocation
        );

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
        return switch (transportType.toLowerCase()) {
            case "car" -> "driving-car";
            case "bicycle" -> "cycling-regular";
            case "walking" -> "foot-walking";
            case "public transport" -> "driving-car"; // ORS doesn't support public transport directly
            default -> "driving-car"; // Default
        };
    }

    /**
     * Executes an HTTP GET request and returns the response body
     */
    private String executeRequest(String url) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);

            // KORRIGIERT: Richtiger Accept-Header für Directions API
            request.setHeader("Authorization", "Bearer " + apiKey);
            request.setHeader("Accept", "application/geo+json");

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
     * Parses the directions API response (GeoJSON format)
     */
    private RouteInfo parseDirectionsResponse(String responseBody) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);

            if (!rootNode.has("features") || rootNode.get("features").size() == 0) {
                logger.error("No routes found in API response");
                return null;
            }

            JsonNode route = rootNode.get("features").get(0);
            JsonNode properties = route.get("properties");

            // GeoJSON structure for ORS directions
            if (properties.has("summary")) {
                JsonNode summary = properties.get("summary");
                double distance = summary.get("distance").asDouble() / 1000; // Convert to km
                int duration = (int) (summary.get("duration").asDouble() / 60); // Convert to minutes

                logger.info("Route info: distance={}km, duration={}min", distance, duration);
                return new RouteInfo(distance, duration);
            } else if (properties.has("segments")) {
                // Alternative structure
                JsonNode segments = properties.get("segments");
                if (segments.isArray() && segments.size() > 0) {
                    JsonNode firstSegment = segments.get(0);
                    double distance = firstSegment.get("distance").asDouble() / 1000;
                    int duration = (int) (firstSegment.get("duration").asDouble() / 60);

                    logger.info("Route info: distance={}km, duration={}min", distance, duration);
                    return new RouteInfo(distance, duration);
                }
            }

            logger.error("Could not parse route summary from response");
            return null;

        } catch (Exception e) {
            logger.error("Error parsing directions response", e);
            logger.error("Response body: {}", responseBody.substring(0, Math.min(500, responseBody.length())));
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