package org.example.tourplanner.business.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.models.Tour;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HttpTourService implements TourService {
    private static final Logger logger = LogManager.getLogger(HttpTourService.class);
    private static final String BASE_URL = "http://localhost:8080/api/tours";

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static HttpTourService instance;

    private HttpTourService() {
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public static synchronized HttpTourService getInstance() {
        if (instance == null) {
            instance = new HttpTourService();
        }
        return instance;
    }

    @Override
    public List<Tour> getAllTours() {
        try {
            HttpGet request = new HttpGet(BASE_URL);
            request.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());

                if (response.getCode() == 200) {
                    // The backend returns TourSummaryDto[], we need to convert to Tour[]
                    TourSummaryDto[] summaryDtos = objectMapper.readValue(responseBody, TourSummaryDto[].class);
                    return Arrays.stream(summaryDtos)
                            .map(this::convertSummaryToTour)
                            .toList();
                } else {
                    logger.error("Failed to get tours: HTTP {}", response.getCode());
                    return Collections.emptyList();
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching tours", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Tour getTourById(Long id) {
        try {
            HttpGet request = new HttpGet(BASE_URL + "/" + id);
            request.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    TourResponseDto dto = objectMapper.readValue(responseBody, TourResponseDto.class);
                    return convertResponseToTour(dto);
                } else {
                    logger.error("Failed to get tour {}: HTTP {}", id, response.getCode());
                    return null;
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching tour with id {}", id, e);
            return null;
        }
    }

    @Override
    public Tour createTour(Tour tour) {
        try {
            TourRequestDto requestDto = convertTourToRequest(tour);
            String json = objectMapper.writeValueAsString(requestDto);

            HttpPost request = new HttpPost(BASE_URL);
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 201) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    TourResponseDto dto = objectMapper.readValue(responseBody, TourResponseDto.class);
                    return convertResponseToTour(dto);
                } else {
                    logger.error("Failed to create tour: HTTP {}", response.getCode());
                    return null;
                }
            }
        } catch (Exception e) {
            logger.error("Error creating tour", e);
            return null;
        }
    }

    @Override
    public Tour updateTour(Tour tour) {
        try {
            TourRequestDto requestDto = convertTourToRequest(tour);
            String json = objectMapper.writeValueAsString(requestDto);

            HttpPut request = new HttpPut(BASE_URL + "/" + tour.getId());
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    TourResponseDto dto = objectMapper.readValue(responseBody, TourResponseDto.class);
                    return convertResponseToTour(dto);
                } else {
                    logger.error("Failed to update tour: HTTP {}", response.getCode());
                    return null;
                }
            }
        } catch (Exception e) {
            logger.error("Error updating tour", e);
            return null;
        }
    }

    @Override
    public void deleteTour(Long id) {
        try {
            HttpDelete request = new HttpDelete(BASE_URL + "/" + id);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() != 204) {
                    logger.error("Failed to delete tour: HTTP {}", response.getCode());
                }
            }
        } catch (Exception e) {
            logger.error("Error deleting tour with id {}", id, e);
        }
    }

    @Override
    public List<Tour> searchTours(String searchTerm) {
        try {
            HttpGet request = new HttpGet(BASE_URL + "/search?q=" + searchTerm);
            request.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    TourSummaryDto[] summaryDtos = objectMapper.readValue(responseBody, TourSummaryDto[].class);
                    return Arrays.stream(summaryDtos)
                            .map(this::convertSummaryToTour)
                            .toList();
                } else {
                    logger.error("Failed to search tours: HTTP {}", response.getCode());
                    return Collections.emptyList();
                }
            }
        } catch (Exception e) {
            logger.error("Error searching tours", e);
            return Collections.emptyList();
        }
    }

    // Helper methods to convert between DTOs and Models
    private Tour convertSummaryToTour(TourSummaryDto dto) {
        Tour tour = new Tour();
        tour.setId(dto.getId());
        tour.setName(dto.getName());
        tour.setFrom(dto.getFromLocation());
        tour.setTo(dto.getToLocation());
        tour.setTransportType(dto.getTransportType());
        tour.setDistance(dto.getDistance() != null ? dto.getDistance() : 0.0);
        tour.setEstimatedTime(dto.getEstimatedTime() != null ? dto.getEstimatedTime() : 0);
        return tour;
    }

    private Tour convertResponseToTour(TourResponseDto dto) {
        Tour tour = new Tour();
        tour.setId(dto.getId());
        tour.setName(dto.getName());
        tour.setDescription(dto.getDescription());
        tour.setFrom(dto.getFromLocation());
        tour.setTo(dto.getToLocation());
        tour.setTransportType(dto.getTransportType());
        tour.setDistance(dto.getDistance() != null ? dto.getDistance() : 0.0);
        tour.setEstimatedTime(dto.getEstimatedTime() != null ? dto.getEstimatedTime() : 0);
        tour.setRouteImagePath(dto.getRouteImagePath());
        return tour;
    }

    private TourRequestDto convertTourToRequest(Tour tour) {
        TourRequestDto dto = new TourRequestDto();
        dto.setName(tour.getName());
        dto.setDescription(tour.getDescription());
        dto.setFromLocation(tour.getFrom());
        dto.setToLocation(tour.getTo());
        dto.setTransportType(tour.getTransportType());
        dto.setDistance(tour.getDistance());
        dto.setEstimatedTime(tour.getEstimatedTime());
        dto.setRouteImagePath(tour.getRouteImagePath());
        return dto;
    }

    // DTO classes (you need to create these to match your backend DTOs)
    public static class TourSummaryDto {
        private Long id;
        private String name;
        private String fromLocation;
        private String toLocation;
        private String transportType;
        private Double distance;
        private Integer estimatedTime;
        private Integer popularity;
        private Double childFriendliness;

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getFromLocation() { return fromLocation; }
        public void setFromLocation(String fromLocation) { this.fromLocation = fromLocation; }
        public String getToLocation() { return toLocation; }
        public void setToLocation(String toLocation) { this.toLocation = toLocation; }
        public String getTransportType() { return transportType; }
        public void setTransportType(String transportType) { this.transportType = transportType; }
        public Double getDistance() { return distance; }
        public void setDistance(Double distance) { this.distance = distance; }
        public Integer getEstimatedTime() { return estimatedTime; }
        public void setEstimatedTime(Integer estimatedTime) { this.estimatedTime = estimatedTime; }
        public Integer getPopularity() { return popularity; }
        public void setPopularity(Integer popularity) { this.popularity = popularity; }
        public Double getChildFriendliness() { return childFriendliness; }
        public void setChildFriendliness(Double childFriendliness) { this.childFriendliness = childFriendliness; }
    }

    public static class TourResponseDto extends TourSummaryDto {
        private String description;
        private String routeImagePath;
        private Integer tourLogsCount;

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getRouteImagePath() { return routeImagePath; }
        public void setRouteImagePath(String routeImagePath) { this.routeImagePath = routeImagePath; }
        public Integer getTourLogsCount() { return tourLogsCount; }
        public void setTourLogsCount(Integer tourLogsCount) { this.tourLogsCount = tourLogsCount; }
    }

    public static class TourRequestDto {
        private String name;
        private String description;
        private String fromLocation;
        private String toLocation;
        private String transportType;
        private Double distance;
        private Integer estimatedTime;
        private String routeImagePath;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getFromLocation() { return fromLocation; }
        public void setFromLocation(String fromLocation) { this.fromLocation = fromLocation; }
        public String getToLocation() { return toLocation; }
        public void setToLocation(String toLocation) { this.toLocation = toLocation; }
        public String getTransportType() { return transportType; }
        public void setTransportType(String transportType) { this.transportType = transportType; }
        public Double getDistance() { return distance; }
        public void setDistance(Double distance) { this.distance = distance; }
        public Integer getEstimatedTime() { return estimatedTime; }
        public void setEstimatedTime(Integer estimatedTime) { this.estimatedTime = estimatedTime; }
        public String getRouteImagePath() { return routeImagePath; }
        public void setRouteImagePath(String routeImagePath) { this.routeImagePath = routeImagePath; }
    }
}