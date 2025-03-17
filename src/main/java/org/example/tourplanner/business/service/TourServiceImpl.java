package org.example.tourplanner.business.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.models.Tour;
import org.example.tourplanner.models.TourLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class TourServiceImpl implements TourService {
    private static final Logger logger = LogManager.getLogger(TourServiceImpl.class);

    // Temporäre Liste, bis wir eine echte Datenbank haben
    private final List<Tour> tours = new CopyOnWriteArrayList<>();

    // Singleton-Pattern für den Übergang (später durch Spring-DI ersetzt)
    private static TourServiceImpl instance;

    private TourServiceImpl() {
        // Privater Konstruktor für Singleton
    }

    public static synchronized TourServiceImpl getInstance() {
        if (instance == null) {
            instance = new TourServiceImpl();
        }
        return instance;
    }

    @Override
    public List<Tour> getAllTours() {
        logger.info("Fetching all tours, count: {}", tours.size());
        return new ArrayList<>(tours);
    }

    @Override
    public Tour getTourById(Long id) {
        logger.info("Fetching tour with ID: {}", id);
        return tours.stream()
                .filter(tour -> tour.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Tour createTour(Tour tour) {
        logger.info("Creating new tour: {}", tour.getName());

        // Setze eine ID, falls keine vorhanden ist
        if (tour.getId() == null) {
            // Finde die höchste ID und erhöhe sie um 1
            Long maxId = tours.stream()
                    .map(Tour::getId)
                    .filter(id -> id != null)
                    .max(Long::compare)
                    .orElse(0L);
            tour.setId(maxId + 1);
        }

        // Erstelle eine Kopie um sicherzustellen, dass wir nicht die Referenz modifizieren
        Tour newTour = new Tour(
                tour.getName(),
                tour.getDescription(),
                tour.getFrom(),
                tour.getTo(),
                tour.getTransportType()
        );
        newTour.setId(tour.getId());
        newTour.setDistance(tour.getDistance());
        newTour.setEstimatedTime(tour.getEstimatedTime());
        newTour.setRouteImagePath(tour.getRouteImagePath());

        // Füge TourLogs hinzu, falls vorhanden
        if (tour.getTourLogs() != null) {
            for (TourLog log : tour.getTourLogs()) {
                TourLog newLog = new TourLog(
                        log.getDate(),
                        log.getComment(),
                        log.getDifficulty(),
                        log.getTotalDistance(),
                        log.getTotalTime(),
                        log.getRating()
                );
                newLog.setId(log.getId());
                newLog.setTour(newTour);
                newTour.addTourLog(newLog);
            }
        }

        tours.add(newTour);
        logger.info("Tour created with ID: {}", newTour.getId());
        return newTour;
    }

    @Override
    public Tour updateTour(Tour tour) {
        logger.info("Updating tour with ID: {}", tour.getId());

        // Finde den existierenden Tour
        int index = -1;
        for (int i = 0; i < tours.size(); i++) {
            if (tours.get(i).getId().equals(tour.getId())) {
                index = i;
                break;
            }
        }

        if (index >= 0) {
            Tour existing = tours.get(index);
            // Aktualisiere die Felder
            existing.setName(tour.getName());
            existing.setDescription(tour.getDescription());
            existing.setFrom(tour.getFrom());
            existing.setTo(tour.getTo());
            existing.setTransportType(tour.getTransportType());
            existing.setDistance(tour.getDistance());
            existing.setEstimatedTime(tour.getEstimatedTime());
            existing.setRouteImagePath(tour.getRouteImagePath());

            // Hier könnten wir auch TourLogs aktualisieren, aber für jetzt lassen wir sie unverändert

            logger.info("Tour updated successfully");
            return existing;
        } else {
            logger.warn("Tour not found for update: {}", tour.getId());
            return null;
        }
    }

    @Override
    public void deleteTour(Long id) {
        logger.info("Deleting tour with ID: {}", id);
        boolean removed = tours.removeIf(tour -> tour.getId().equals(id));
        if (removed) {
            logger.info("Tour deleted successfully");
        } else {
            logger.warn("Tour not found for deletion: {}", id);
        }
    }

    @Override
    public List<Tour> searchTours(String searchTerm) {
        logger.info("Searching tours with term: '{}'", searchTerm);

        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllTours();
        }

        String lowerCaseFilter = searchTerm.toLowerCase();

        List<Tour> result = tours.stream()
                .filter(tour -> {
                    // Check if tour name contains filter
                    if (tour.getName().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    }

                    // Check if description contains filter
                    if (tour.getDescription().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    }

                    // Check if from/to contains filter
                    if (tour.getFrom().toLowerCase().contains(lowerCaseFilter) ||
                            tour.getTo().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    }

                    // Check if transport type contains filter
                    if (tour.getTransportType().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    }

                    // Check in tour logs
                    for (TourLog log : tour.getTourLogs()) {
                        if (log.getComment().toLowerCase().contains(lowerCaseFilter)) {
                            return true;
                        }
                    }

                    // Check computed attributes
                    if (String.valueOf(tour.getPopularity()).contains(lowerCaseFilter)) {
                        return true;
                    }

                    if (String.valueOf(tour.getChildFriendliness()).contains(lowerCaseFilter)) {
                        return true;
                    }

                    return false;
                })
                .collect(Collectors.toList());

        logger.info("Found {} matching tours", result.size());
        return result;
    }

    // Hilfsmethode, um Demo-Daten hinzuzufügen (wird später entfernt)
    public void addTour(Tour tour) {
        tours.add(tour);
    }
}