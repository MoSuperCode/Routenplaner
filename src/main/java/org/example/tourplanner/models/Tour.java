package org.example.tourplanner.models;

import java.util.ArrayList;
import java.util.List;

public class Tour {
    private Long id;
    private String name;
    private String description;
    private String from;
    private String to;
    private String transportType;
    private double distance; // in kilometers
    private int estimatedTime; // in minutes
    private String routeImagePath;
    private List<TourLog> tourLogs;

    public Tour() {
        this.tourLogs = new ArrayList<>();
    }

    public Tour(String name, String description, String from, String to, String transportType) {
        this();
        this.name = name;
        this.description = description;
        this.from = from;
        this.to = to;
        this.transportType = transportType;
    }

    // Computed properties based on requirements
    public int getPopularity() {
        return tourLogs.size();
    }

    public double getChildFriendliness() {
        if (tourLogs.isEmpty()) {
            return 0;
        }

        // Calculate based on difficulty, time and distance
        double avgDifficulty = tourLogs.stream()
                .mapToInt(TourLog::getDifficulty)
                .average()
                .orElse(0);

        double avgTime = tourLogs.stream()
                .mapToInt(TourLog::getTotalTime)
                .average()
                .orElse(0);

        // Higher values mean less child-friendly
        double rawScore = (avgDifficulty * 0.5) + (distance * 0.3) + (avgTime * 0.2);

        // Convert to a 0-10 scale where 10 is most child-friendly
        return Math.max(0, 10 - rawScore);
    }

    // Getter and Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(int estimatedTime) {
        this.estimatedTime = estimatedTime;
    }

    public String getRouteImagePath() {
        return routeImagePath;
    }

    public void setRouteImagePath(String routeImagePath) {
        this.routeImagePath = routeImagePath;
    }

    public List<TourLog> getTourLogs() {
        return tourLogs;
    }

    public void setTourLogs(List<TourLog> tourLogs) {
        this.tourLogs = tourLogs;
    }

    public void addTourLog(TourLog tourLog) {
        this.tourLogs.add(tourLog);
    }

    public void removeTourLog(TourLog tourLog) {
        this.tourLogs.remove(tourLog);
    }
}