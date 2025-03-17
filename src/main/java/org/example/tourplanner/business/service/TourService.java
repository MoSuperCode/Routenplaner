package org.example.tourplanner.business.service;

import org.example.tourplanner.models.Tour;
import java.util.List;

public interface TourService {
    /**
     * Retrieves all tours
     * @return List of all tours
     */
    List<Tour> getAllTours();

    /**
     * Retrieves a tour by its ID
     * @param id Tour ID
     * @return Tour object or null if not found
     */
    Tour getTourById(Long id);

    /**
     * Creates a new tour
     * @param tour Tour to create
     * @return Created tour with ID
     */
    Tour createTour(Tour tour);

    /**
     * Updates an existing tour
     * @param tour Tour to update
     * @return Updated tour or null if not found
     */
    Tour updateTour(Tour tour);

    /**
     * Deletes a tour by its ID
     * @param id ID of the tour to delete
     */
    void deleteTour(Long id);

    /**
     * Searches for tours based on search criteria
     * @param searchTerm Term to search for in tour data
     * @return List of matching tours
     */
    List<Tour> searchTours(String searchTerm);
}