package org.example.tourplanner.business.service;

import org.example.tourplanner.models.Tour;
import org.example.tourplanner.models.TourLog;

import java.util.List;

public interface TourLogService {
    /**
     * Retrieves all logs for a specific tour
     * @param tourId ID of the tour
     * @return List of tour logs
     */
    List<TourLog> getTourLogs(Long tourId);

    /**
     * Retrieves a specific tour log by ID
     * @param id Tour log ID
     * @return Tour log or null if not found
     */
    TourLog getTourLogById(Long id);

    /**
     * Creates a new tour log for a tour
     * @param tourId ID of the tour
     * @param tourLog Tour log to create
     * @return Created tour log with ID
     */
    TourLog createTourLog(Long tourId, TourLog tourLog);

    /**
     * Updates an existing tour log
     * @param tourLog Tour log to update
     * @return Updated tour log or null if not found
     */
    TourLog updateTourLog(TourLog tourLog);

    /**
     * Deletes a tour log by ID
     * @param id ID of the tour log to delete
     */
    void deleteTourLog(Long id);
}