package org.example.tourplanner.ui.views;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.example.tourplanner.models.Tour;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class MapViewController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(MapViewController.class);

    @FXML
    private WebView mapWebView;

    private WebEngine webEngine;
    private ObjectMapper objectMapper;
    private boolean mapLoaded = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        objectMapper = new ObjectMapper();
        setupWebView();
    }

    private void setupWebView() {
        webEngine = mapWebView.getEngine();

        // Enable JavaScript
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                mapLoaded = true;
                logger.info("Map loaded successfully");

                // Make this controller accessible from JavaScript if needed
                JSObject window = (JSObject) webEngine.executeScript("window");
                window.setMember("javaController", this);
            }
        });

        // Load the map HTML file
        String mapHtmlPath = getClass().getResource("/map/interactive-map.html").toExternalForm();
        webEngine.load(mapHtmlPath);
    }

    /**
     * Display a route on the map
     */
    public void displayRoute(Tour tour, double[] fromCoords, double[] toCoords, Double distance, Integer time) {
        if (!mapLoaded) {
            logger.warn("Map not loaded yet, cannot display route");
            return;
        }

        try {
            // Create route data object
            Map<String, Object> routeData = new HashMap<>();
            routeData.put("fromLocation", tour.getFromLocation());
            routeData.put("toLocation", tour.getToLocation());
            routeData.put("fromCoords", fromCoords);
            routeData.put("toCoords", toCoords);
            routeData.put("distance", distance != null ? distance : tour.getDistance());
            routeData.put("time", time != null ? time : tour.getEstimatedTime());
            routeData.put("transportType", tour.getTransportType());
            routeData.put("routeGeometry", new double[][]{}); // Empty for now, can be enhanced later

            // Convert to JSON
            String routeJson = objectMapper.writeValueAsString(routeData);

            // Call JavaScript function
            String script = String.format("loadRoute(%s);", routeJson);
            webEngine.executeScript(script);

            logger.info("Route displayed on map: {} to {}", tour.getFromLocation(), tour.getToLocation());

        } catch (Exception e) {
            logger.error("Error displaying route on map", e);
            showError("Error displaying route: " + e.getMessage());
        }
    }

    /**
     * Display route with calculated data from backend
     */
    public void displayCalculatedRoute(String fromLocation, String toLocation, String transportType,
                                       double[] fromCoords, double[] toCoords, double distance, int time) {
        if (!mapLoaded) {
            logger.warn("Map not loaded yet, cannot display route");
            return;
        }

        try {
            Map<String, Object> routeData = new HashMap<>();
            routeData.put("fromLocation", fromLocation);
            routeData.put("toLocation", toLocation);
            routeData.put("fromCoords", fromCoords);
            routeData.put("toCoords", toCoords);
            routeData.put("distance", distance);
            routeData.put("time", time);
            routeData.put("transportType", transportType);
            routeData.put("routeGeometry", new double[][]{}); // Can be enhanced with actual route geometry

            String routeJson = objectMapper.writeValueAsString(routeData);
            String script = String.format("loadRoute(%s);", routeJson);
            webEngine.executeScript(script);

            logger.info("Calculated route displayed: {} to {} ({} km, {} min)",
                    fromLocation, toLocation, distance, time);

        } catch (Exception e) {
            logger.error("Error displaying calculated route", e);
            showError("Error displaying route: " + e.getMessage());
        }
    }

    /**
     * Center map on specific coordinates
     */
    public void centerMap(double latitude, double longitude, int zoom) {
        if (!mapLoaded) {
            return;
        }

        String script = String.format("centerMap(%f, %f, %d);", latitude, longitude, zoom);
        webEngine.executeScript(script);
    }

    /**
     * Show loading state on map
     */
    public void showLoading() {
        if (mapLoaded) {
            webEngine.executeScript("showLoading();");
        }
    }

    /**
     * Show error message on map
     */
    public void showError(String message) {
        if (mapLoaded) {
            String script = String.format("showError('%s');", message.replace("'", "\\'"));
            webEngine.executeScript(script);
        }
    }

    /**
     * Clear the map
     */
    public void clearMap() {
        if (mapLoaded) {
            webEngine.executeScript("clearMap();");
        }
    }

    /**
     * Reload the map
     */
    public void reloadMap() {
        mapLoaded = false;
        String mapHtmlPath = getClass().getResource("/map/interactive-map.html").toExternalForm();
        webEngine.load(mapHtmlPath);
    }

    /**
     * Check if map is loaded and ready
     */
    public boolean isMapLoaded() {
        return mapLoaded;
    }
}