package org.example.tourplanner.business.service;

import org.example.tourplanner.models.Tour;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TourServiceTest {

    private TourService tourService;

    @BeforeEach
    public void setUp() {
        // Get the singleton instance
        tourService = TourServiceImpl.getInstance();

        // Clear any existing tours from previous tests
        List<Tour> allTours = tourService.getAllTours();
        for (Tour tour : allTours) {
            tourService.deleteTour(tour.getId());
        }
    }

    @Test
    public void testCreateAndGetTour() {
        // Create a new tour
        Tour newTour = new Tour("Test Tour", "Test Description", "Vienna", "Salzburg", "Car");
        newTour.setDistance(295.0);
        newTour.setEstimatedTime(180);

        // Save to service
        Tour createdTour = tourService.createTour(newTour);

        // Verify it has an ID after creation
        assertNotNull(createdTour.getId(), "Tour ID should not be null after creation");

        // Retrieve the tour and verify fields
        Tour retrievedTour = tourService.getTourById(createdTour.getId());

        assertNotNull(retrievedTour, "Retrieved tour should not be null");
        assertEquals("Test Tour", retrievedTour.getName(), "Tour name should match");
        assertEquals("Test Description", retrievedTour.getDescription(), "Tour description should match");
        assertEquals("Vienna", retrievedTour.getFrom(), "Starting point should match");
        assertEquals("Salzburg", retrievedTour.getTo(), "Destination should match");
        assertEquals("Car", retrievedTour.getTransportType(), "Transport type should match");
        assertEquals(295.0, retrievedTour.getDistance(), "Distance should match");
        assertEquals(180, retrievedTour.getEstimatedTime(), "Estimated time should match");
    }

    @Test
    public void testUpdateTour() {
        // Create a tour
        Tour initialTour = new Tour("Initial Tour", "Initial Description", "Vienna", "Salzburg", "Car");
        Tour createdTour = tourService.createTour(initialTour);

        // Update the tour
        createdTour.setName("Updated Tour");
        createdTour.setDescription("Updated Description");

        Tour updatedTour = tourService.updateTour(createdTour);

        // Verify the update
        assertNotNull(updatedTour, "Updated tour should not be null");
        assertEquals("Updated Tour", updatedTour.getName(), "Tour name should be updated");
        assertEquals("Updated Description", updatedTour.getDescription(), "Tour description should be updated");

        // Verify the change is persistent
        Tour retrievedTour = tourService.getTourById(createdTour.getId());
        assertEquals("Updated Tour", retrievedTour.getName(), "Tour name should remain updated after retrieval");
    }
}