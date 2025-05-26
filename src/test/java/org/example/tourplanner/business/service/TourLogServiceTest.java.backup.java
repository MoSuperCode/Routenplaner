package org.example.tourplanner.business.service;

import org.example.tourplanner.models.Tour;
import org.example.tourplanner.models.TourLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TourLogServiceTest {

    private TourService tourService;
    private TourLogService tourLogService;
    private Tour testTour;

    @BeforeEach
    public void setUp() {
        // Get service instances
        tourService = TourServiceImpl.getInstance();
        tourLogService = TourLogServiceImpl.getInstance();

        // Clear existing tours
        List<Tour> allTours = tourService.getAllTours();
        for (Tour tour : allTours) {
            tourService.deleteTour(tour.getId());
        }

        // Create a test tour to use
        testTour = new Tour("Test Tour", "Test Description", "Vienna", "Salzburg", "Car");
        testTour = tourService.createTour(testTour);
    }

    @Test
    public void testCreateAndGetTourLog() {
        // Create a tour log
        LocalDateTime now = LocalDateTime.now();
        TourLog tourLog = new TourLog(now, "Test comment", 5, 100.0, 60, 4);

        // Save to service
        TourLog createdLog = tourLogService.createTourLog(testTour.getId(), tourLog);

        // Verify it has an ID after creation
        assertNotNull(createdLog.getId(), "Tour log ID should not be null after creation");

        // Retrieve the tour log
        TourLog retrievedLog = tourLogService.getTourLogById(createdLog.getId());

        // Verify fields
        assertNotNull(retrievedLog, "Retrieved tour log should not be null");
        assertEquals("Test comment", retrievedLog.getComment(), "Comment should match");
        assertEquals(5, retrievedLog.getDifficulty(), "Difficulty should match");
        assertEquals(100.0, retrievedLog.getTotalDistance(), "Distance should match");
        assertEquals(60, retrievedLog.getTotalTime(), "Time should match");
        assertEquals(4, retrievedLog.getRating(), "Rating should match");
        assertEquals(now, retrievedLog.getDate(), "Date should match");
    }

    @Test
    public void testUpdateTourLog() {
        // Create a tour log
        TourLog initialLog = new TourLog(
                LocalDateTime.now(),
                "Initial comment",
                3,
                50.0,
                30,
                3
        );

        TourLog createdLog = tourLogService.createTourLog(testTour.getId(), initialLog);

        // Update the tour log
        createdLog.setComment("Updated comment");
        createdLog.setDifficulty(4);

        TourLog updatedLog = tourLogService.updateTourLog(createdLog);

        // Verify the update
        assertNotNull(updatedLog, "Updated tour log should not be null");
        assertEquals("Updated comment", updatedLog.getComment(), "Comment should be updated");
        assertEquals(4, updatedLog.getDifficulty(), "Difficulty should be updated");

        // Verify the change is persistent
        TourLog retrievedLog = tourLogService.getTourLogById(createdLog.getId());
        assertEquals("Updated comment", retrievedLog.getComment(), "Comment should remain updated after retrieval");
    }
}