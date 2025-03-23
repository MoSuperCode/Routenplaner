package org.example.tourplanner.business.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.models.Tour;
import org.example.tourplanner.models.TourLog;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TourLogServiceImpl implements TourLogService {
    private static final Logger logger = LogManager.getLogger(TourLogServiceImpl.class);

    private final TourService tourService;

    // Singleton-Pattern für den Übergang (später durch Spring-DI ersetzt)
    private static TourLogServiceImpl instance;

    private TourLogServiceImpl() {
        this.tourService = TourServiceImpl.getInstance();
    }

    public static synchronized TourLogServiceImpl getInstance() {
        if (instance == null) {
            instance = new TourLogServiceImpl();
        }
        return instance;
    }

    @Override
    public List<TourLog> getTourLogs(Long tourId) {
        logger.info("Fetching logs for tour ID: {}", tourId);
        Tour tour = tourService.getTourById(tourId);
        if (tour != null) {
            return new ArrayList<>(tour.getTourLogs());
        }
        logger.warn("Tour not found: {}", tourId);
        return new ArrayList<>();
    }

    @Override
    public TourLog getTourLogById(Long id) {
        logger.info("Fetching tour log with ID: {}", id);

        // Durchsuche alle Tours, um den TourLog zu finden
        for (Tour tour : tourService.getAllTours()) {
            for (TourLog log : tour.getTourLogs()) {
                if (log.getId().equals(id)) {
                    return log;
                }
            }
        }

        logger.warn("Tour log not found: {}", id);
        return null;
    }

    @Override
    public TourLog createTourLog(Long tourId, TourLog tourLog) {
        logger.info("Creating new log for tour ID: {}", tourId);
        Tour tour = tourService.getTourById(tourId);
        if (tour == null) {
            logger.warn("Tour not found: {}", tourId);
            return null;
        }

        // Setze eine ID, falls keine vorhanden ist
        if (tourLog.getId() == null) {
            // Finde die höchste ID und erhöhe sie um 1
            Long maxId = 0L;
            for (Tour t : tourService.getAllTours()) {
                for (TourLog log : t.getTourLogs()) {
                    if (log.getId() > maxId) {
                        maxId = log.getId();
                    }
                }
            }
            tourLog.setId(maxId + 1);
        }

        // Erstelle eine Kopie
        TourLog newLog = new TourLog(
                tourLog.getDate(),
                tourLog.getComment(),
                tourLog.getDifficulty(),
                tourLog.getTotalDistance(),
                tourLog.getTotalTime(),
                tourLog.getRating()
        );
        newLog.setId(tourLog.getId());
        newLog.setTour(tour);

        // Füge den TourLog zur Tour hinzu
        tour.addTourLog(newLog);

        logger.info("Tour log created with ID: {}", newLog.getId());
        return newLog;
    }

    @Override
    public TourLog updateTourLog(TourLog tourLog) {
        logger.info("Updating tour log with ID: {}", tourLog.getId());

        TourLog existingLog = getTourLogById(tourLog.getId());
        if (existingLog == null) {
            logger.warn("Tour log not found: {}", tourLog.getId());
            return null;
        }

        // Aktualisiere die Felder
        existingLog.setDate(tourLog.getDate());
        existingLog.setComment(tourLog.getComment());
        existingLog.setDifficulty(tourLog.getDifficulty());
        existingLog.setTotalDistance(tourLog.getTotalDistance());
        existingLog.setTotalTime(tourLog.getTotalTime());
        existingLog.setRating(tourLog.getRating());

        // Get parent tour and update it to ensure persistence
        Tour tour = existingLog.getTour();
        if (tour != null) {
            Tour updatedTour = tourService.updateTour(tour);
            if (updatedTour == null) {
                logger.warn("Failed to update parent tour");
            }
        }

        logger.info("Tour log updated successfully");
        return existingLog;
    }

    @Override
    public void deleteTourLog(Long id) {
        logger.info("Deleting tour log with ID: {}", id);

        TourLog log = getTourLogById(id);
        if (log == null) {
            logger.warn("Tour log not found: {}", id);
            return;
        }

        Tour tour = log.getTour();
        tour.removeTourLog(log);

        logger.info("Tour log deleted successfully");
    }
}