package org.example.tourplanner.business.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.models.TourLog;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HttpTourLogService implements TourLogService {
    private static final Logger logger = LogManager.getLogger(HttpTourLogService.class);
    private static final String BASE_URL = "http://localhost:8080/api";

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static HttpTourLogService instance;

    private HttpTourLogService() {
        this.httpClient = HttpClients.createDefault();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public static synchronized HttpTourLogService getInstance() {
        if (instance == null) {
            instance = new HttpTourLogService();
        }
        return instance;
    }

    @Override
    public List<TourLog> getTourLogs(Long tourId) {
        try {
            HttpGet request = new HttpGet(BASE_URL + "/tours/" + tourId + "/logs");
            request.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    TourLogResponseDto[] dtos = objectMapper.readValue(responseBody, TourLogResponseDto[].class);
                    return Arrays.stream(dtos)
                            .map(this::convertResponseToTourLog)
                            .toList();
                } else {
                    logger.error("Failed to get tour logs: HTTP {}", response.getCode());
                    return Collections.emptyList();
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching tour logs for tour {}", tourId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public TourLog getTourLogById(Long id) {
        try {
            HttpGet request = new HttpGet(BASE_URL + "/logs/" + id);
            request.setHeader("Accept", "application/json");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    TourLogResponseDto dto = objectMapper.readValue(responseBody, TourLogResponseDto.class);
                    return convertResponseToTourLog(dto);
                } else {
                    logger.error("Failed to get tour log {}: HTTP {}", id, response.getCode());
                    return null;
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching tour log with id {}", id, e);
            return null;
        }
    }

    @Override
    public TourLog createTourLog(Long tourId, TourLog tourLog) {
        try {
            TourLogRequestDto requestDto = convertTourLogToRequest(tourLog);
            String json = objectMapper.writeValueAsString(requestDto);

            HttpPost request = new HttpPost(BASE_URL + "/tours/" + tourId + "/logs");
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 201) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    TourLogResponseDto dto = objectMapper.readValue(responseBody, TourLogResponseDto.class);
                    return convertResponseToTourLog(dto);
                } else {
                    logger.error("Failed to create tour log: HTTP {}", response.getCode());
                    return null;
                }
            }
        } catch (Exception e) {
            logger.error("Error creating tour log", e);
            return null;
        }
    }

    @Override
    public TourLog updateTourLog(TourLog tourLog) {
        try {
            TourLogRequestDto requestDto = convertTourLogToRequest(tourLog);
            String json = objectMapper.writeValueAsString(requestDto);

            HttpPut request = new HttpPut(BASE_URL + "/logs/" + tourLog.getId());
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    TourLogResponseDto dto = objectMapper.readValue(responseBody, TourLogResponseDto.class);
                    return convertResponseToTourLog(dto);
                } else {
                    logger.error("Failed to update tour log: HTTP {}", response.getCode());
                    return null;
                }
            }
        } catch (Exception e) {
            logger.error("Error updating tour log", e);
            return null;
        }
    }

    @Override
    public void deleteTourLog(Long id) {
        try {
            HttpDelete request = new HttpDelete(BASE_URL + "/logs/" + id);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() != 204) {
                    logger.error("Failed to delete tour log: HTTP {}", response.getCode());
                }
            }
        } catch (Exception e) {
            logger.error("Error deleting tour log with id {}", id, e);
        }
    }

    // Helper methods
    private TourLog convertResponseToTourLog(TourLogResponseDto dto) {
        TourLog tourLog = new TourLog();
        tourLog.setId(dto.getId());
        tourLog.setDate(dto.getDate());
        tourLog.setComment(dto.getComment() != null ? dto.getComment() : "");
        tourLog.setDifficulty(dto.getDifficulty());
        tourLog.setTotalDistance(dto.getTotalDistance());
        tourLog.setTotalTime(dto.getTotalTime());
        tourLog.setRating(dto.getRating());
        return tourLog;
    }

    private TourLogRequestDto convertTourLogToRequest(TourLog tourLog) {
        TourLogRequestDto dto = new TourLogRequestDto();
        dto.setDate(tourLog.getDate());
        dto.setComment(tourLog.getComment());
        dto.setDifficulty(tourLog.getDifficulty());
        dto.setTotalDistance(tourLog.getTotalDistance());
        dto.setTotalTime(tourLog.getTotalTime());
        dto.setRating(tourLog.getRating());
        return dto;
    }

    // DTO classes
    public static class TourLogResponseDto {
        private Long id;
        private LocalDateTime date;
        private String comment;
        private Integer difficulty;
        private Double totalDistance;
        private Integer totalTime;
        private Integer rating;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long tourId;
        private String tourName;

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public LocalDateTime getDate() { return date; }
        public void setDate(LocalDateTime date) { this.date = date; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public Integer getDifficulty() { return difficulty; }
        public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
        public Double getTotalDistance() { return totalDistance; }
        public void setTotalDistance(Double totalDistance) { this.totalDistance = totalDistance; }
        public Integer getTotalTime() { return totalTime; }
        public void setTotalTime(Integer totalTime) { this.totalTime = totalTime; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        public Long getTourId() { return tourId; }
        public void setTourId(Long tourId) { this.tourId = tourId; }
        public String getTourName() { return tourName; }
        public void setTourName(String tourName) { this.tourName = tourName; }
    }

    public static class TourLogRequestDto {
        private LocalDateTime date;
        private String comment;
        private Integer difficulty;
        private Double totalDistance;
        private Integer totalTime;
        private Integer rating;

        // Getters and setters
        public LocalDateTime getDate() { return date; }
        public void setDate(LocalDateTime date) { this.date = date; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public Integer getDifficulty() { return difficulty; }
        public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }
        public Double getTotalDistance() { return totalDistance; }
        public void setTotalDistance(Double totalDistance) { this.totalDistance = totalDistance; }
        public Integer getTotalTime() { return totalTime; }
        public void setTotalTime(Integer totalTime) { this.totalTime = totalTime; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
    }
}