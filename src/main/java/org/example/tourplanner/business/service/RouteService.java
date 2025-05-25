package org.example.tourplanner.business.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.business.service.api.OpenRouteServiceClient;
import org.example.tourplanner.business.service.api.OpenRouteServiceClient.RouteInfo;
import org.example.tourplanner.business.service.api.OpenStreetMapClient;
import org.example.tourplanner.config.ConfigurationManager;
import org.example.tourplanner.models.Tour;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class RouteService {
    private static final Logger logger = LogManager.getLogger(RouteService.class);

    private final OpenRouteServiceClient openRouteServiceClient;
    private final OpenStreetMapClient openStreetMapClient;
    private final String basePath;

    // Singleton pattern
    private static RouteService instance;

    private RouteService() {
        this.openRouteServiceClient = new OpenRouteServiceClient();
        this.openStreetMapClient = new OpenStreetMapClient();

        ConfigurationManager configManager = ConfigurationManager.getInstance();
        this.basePath = configManager.getProperty("file.basePath", "./resources/images");

        // Ensure the base directory exists
        ensureImageDirectoryExists(basePath);
    }

    public static synchronized RouteService getInstance() {
        if (instance == null) {
            instance = new RouteService();
        }
        return instance;
    }

    /**
     * Calculates route information between two locations and updates the tour object
     * @param tour Tour object to update with route information
     * @return Updated tour object or null if calculation failed
     */
    public Tour calculateRoute(Tour tour) {
        try {
            logger.info("Calculating route for tour: {}", tour.getName());

            // Get route information from OpenRouteService
            RouteInfo routeInfo = openRouteServiceClient.getDirections(
                    tour.getFrom(),
                    tour.getTo(),
                    tour.getTransportType()
            );

            if (routeInfo == null) {
                logger.error("Failed to get route information");
                return null;
            }

            // Update tour with calculated information
            tour.setDistance(routeInfo.getDistance());
            tour.setEstimatedTime(routeInfo.getDuration());

            // Generate and save route map image
            String imagePath = generateRouteImage(tour);
            if (imagePath != null) {
                tour.setRouteImagePath(imagePath);
            }

            logger.info("Route calculation successful: distance={}km, time={}min",
                    routeInfo.getDistance(), routeInfo.getDuration());

            return tour;

        } catch (IOException e) {
            logger.error("Error calculating route", e);
            return null;
        }
    }

    /**
     * Generates a route image using OpenStreetMap
     */
    private String generateRouteImage(Tour tour) {
        try {
            // Geocode start and end points
            double[] startCoords = openRouteServiceClient.geocode(tour.getFrom());
            double[] endCoords = openRouteServiceClient.geocode(tour.getTo());

            if (startCoords == null || endCoords == null) {
                logger.error("Failed to geocode locations for map image");
                return null;
            }

            // Generate a unique file name
            String fileName = getRouteImageFilePath(tour);

            // Generate and save the map image
            int zoom = calculateZoomLevel(startCoords, endCoords);
            String fullPath = openStreetMapClient.generateStaticMap(
                    startCoords[0], startCoords[1],
                    endCoords[0], endCoords[1],
                    zoom, fileName
            );

            logger.info("Generated route image: {}", fullPath);
            return fileName;

        } catch (Exception e) {
            logger.error("Error generating route image", e);
            return null;
        }
    }

    /**
     * Calculates an appropriate zoom level based on the distance between points
     */
    private int calculateZoomLevel(double[] startCoords, double[] endCoords) {
        // Calculate rough distance in degrees
        double deltaLon = Math.abs(startCoords[0] - endCoords[0]);
        double deltaLat = Math.abs(startCoords[1] - endCoords[1]);
        double maxDelta = Math.max(deltaLon, deltaLat);

        // Simple zoom calculation - lower means more zoomed out
        if (maxDelta > 10) return 5;      // Very far
        if (maxDelta > 5) return 7;       // Far
        if (maxDelta > 1) return 9;       // Medium
        if (maxDelta > 0.5) return 11;    // Close
        if (maxDelta > 0.1) return 13;    // Very close
        return 15;                         // Extremely close
    }

    /**
     * Gets the file path for a route image
     * @param tour Tour to get image path for
     * @return File path as string
     */
    public String getRouteImageFilePath(Tour tour) {
        // If the tour already has an image path, return it
        if (tour.getRouteImagePath() != null && !tour.getRouteImagePath().isEmpty()) {
            return tour.getRouteImagePath();
        }

        // Generate a unique image file name
        String fileName = "route_" + UUID.randomUUID().toString() + ".png";
        return fileName;
    }

    /**
     * Ensures the image directory exists
     * @param basePath Base path for images
     * @return true if directory exists or was created successfully
     */
    private boolean ensureImageDirectoryExists(String basePath) {
        try {
            Path path = Paths.get(basePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                logger.info("Created image directory: {}", basePath);
            }
            return true;
        } catch (IOException e) {
            logger.error("Failed to create image directory", e);
            return false;
        }
    }
}