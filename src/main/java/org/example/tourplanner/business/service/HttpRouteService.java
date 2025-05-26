package org.example.tourplanner.business.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.config.HttpClientConfig;
import org.example.tourplanner.models.Tour;

public class HttpRouteService {
    private static final Logger logger = LogManager.getLogger(HttpRouteService.class);
    private final ObjectMapper objectMapper;
    private static HttpRouteService instance;

    private HttpRouteService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public static synchronized HttpRouteService getInstance() {
        if (instance == null) {
            instance = new HttpRouteService();
        }
        return instance;
    }

    /**
     * Calculate route using backend service
     */
    public RouteCalculationResult calculateRoute(String from, String to, String transportType) {
        try {
            // Create request DTO
            RouteCalculationRequest request = new RouteCalculationRequest();
            request.setFromLocation(from);
            request.setToLocation(to);
            request.setTransportType(transportType);

            String json = objectMapper.writeValueAsString(request);
            String url = HttpClientConfig.getBaseUrl() + "/routes/calculate";

            try (CloseableHttpClient httpClient = HttpClientConfig.getHttpClient()) {
                HttpPost httpPost = new HttpPost(url);
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

                try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                    String responseBody = EntityUtils.toString(response.getEntity());

                    if (response.getCode() == 200) {
                        RouteCalculationResponse responseDto = objectMapper.readValue(
                                responseBody, RouteCalculationResponse.class);

                        return new RouteCalculationResult(
                                responseDto.isSuccess(),
                                responseDto.getDistance(),
                                responseDto.getEstimatedTime(),
                                responseDto.getRouteImagePath(),
                                responseDto.getMessage()
                        );
                    } else {
                        logger.error("Route calculation failed: HTTP {}", response.getCode());
                        return new RouteCalculationResult(false, 0.0, 0, null,
                                "Route calculation failed");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error calculating route", e);
            return new RouteCalculationResult(false, 0.0, 0, null,
                    "Error: " + e.getMessage());
        }
    }

    // DTO Classes
    public static class RouteCalculationRequest {
        private String fromLocation;
        private String toLocation;
        private String transportType;

        // Getters and setters
        public String getFromLocation() { return fromLocation; }
        public void setFromLocation(String fromLocation) { this.fromLocation = fromLocation; }
        public String getToLocation() { return toLocation; }
        public void setToLocation(String toLocation) { this.toLocation = toLocation; }
        public String getTransportType() { return transportType; }
        public void setTransportType(String transportType) { this.transportType = transportType; }
    }

    public static class RouteCalculationResponse {
        private boolean success;
        private Double distance;
        private Integer estimatedTime;
        private String routeImagePath;
        private String message;

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public Double getDistance() { return distance; }
        public void setDistance(Double distance) { this.distance = distance; }
        public Integer getEstimatedTime() { return estimatedTime; }
        public void setEstimatedTime(Integer estimatedTime) { this.estimatedTime = estimatedTime; }
        public String getRouteImagePath() { return routeImagePath; }
        public void setRouteImagePath(String routeImagePath) { this.routeImagePath = routeImagePath; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class RouteCalculationResult {
        private final boolean success;
        private final Double distance;
        private final Integer estimatedTime;
        private final String routeImagePath;
        private final String message;

        public RouteCalculationResult(boolean success, Double distance, Integer estimatedTime,
                                      String routeImagePath, String message) {
            this.success = success;
            this.distance = distance;
            this.estimatedTime = estimatedTime;
            this.routeImagePath = routeImagePath;
            this.message = message;
        }

        // Getters
        public boolean isSuccess() { return success; }
        public Double getDistance() { return distance; }
        public Integer getEstimatedTime() { return estimatedTime; }
        public String getRouteImagePath() { return routeImagePath; }
        public String getMessage() { return message; }
    }
}