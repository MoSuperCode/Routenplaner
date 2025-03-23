package org.example.tourplanner.ui.viewmodels;

import org.example.tourplanner.business.service.TourLogService;
import org.example.tourplanner.business.service.TourLogServiceImpl;
import org.example.tourplanner.business.service.TourService;
import org.example.tourplanner.business.service.TourServiceImpl;
import org.example.tourplanner.models.Tour;
import org.example.tourplanner.models.TourLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MainViewModelTest {

    private MainViewModel viewModel;
    private TourService tourService;
    private TourLogService tourLogService;

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

        // Create the main view model
        viewModel = new MainViewModel();

        // Clear the tours created by demo data in the view model
        List<TourViewModel> tours = viewModel.getTours();
        for (TourViewModel tour : new ArrayList<>(tours)) {
            viewModel.deleteTour(tour);
        }
    }

    @Test
    public void testAddTour() {
        // Verify initial state - should be empty after clearing
        assertTrue(viewModel.getTours().isEmpty(), "Tours list should be empty initially");

        // Create a new tour
        Tour newTour = new Tour("Test Tour", "Test Description", "Vienna", "Salzburg", "Car");

        // Add via view model
        viewModel.addTour(newTour);

        // Verify it's in the list
        assertFalse(viewModel.getTours().isEmpty(), "Tours list should not be empty");
        assertEquals(1, viewModel.getTours().size(), "Should have one tour");
        assertEquals("Test Tour", viewModel.getTours().get(0).nameProperty().get(), "Tour name should match");
    }

    @Test
    public void testUpdateTour() {
        // Verify initial state - should be empty after clearing
        assertTrue(viewModel.getTours().isEmpty(), "Tours list should be empty initially");

        // Create and add a tour
        Tour newTour = new Tour("Initial Tour", "Initial Description", "Vienna", "Salzburg", "Car");
        viewModel.addTour(newTour);

        // Get the view model
        TourViewModel tourViewModel = viewModel.getTours().get(0);

        // Update name
        tourViewModel.nameProperty().set("Updated Tour");
        viewModel.updateTour(tourViewModel);

        // Verify the update is reflected
        assertEquals("Updated Tour", viewModel.getTours().get(0).nameProperty().get(), "Tour name should be updated");

        // Verify the update is persisted in the service
        Tour updatedTour = tourService.getTourById(tourViewModel.idProperty().get());
        assertEquals("Updated Tour", updatedTour.getName(), "Tour name should be updated in service");
    }

    @Test
    public void testDeleteTour() {
        // Verify initial state - should be empty after clearing
        assertTrue(viewModel.getTours().isEmpty(), "Tours list should be empty initially");

        // Create and add a tour
        Tour newTour = new Tour("Test Tour", "Test Description", "Vienna", "Salzburg", "Car");
        viewModel.addTour(newTour);

        // Verify it's added
        assertEquals(1, viewModel.getTours().size(), "Should have one tour");

        // Get the view model
        TourViewModel tourViewModel = viewModel.getTours().get(0);

        // Delete it
        viewModel.deleteTour(tourViewModel);

        // Verify it's removed from the list
        assertTrue(viewModel.getTours().isEmpty(), "Tours list should be empty after deletion");
    }

    @Test
    public void testAddTourLog() {
        // Verify initial state - should be empty after clearing
        assertTrue(viewModel.getTours().isEmpty(), "Tours list should be empty initially");

        // Create and add a tour
        Tour newTour = new Tour("Test Tour", "Test Description", "Vienna", "Salzburg", "Car");
        viewModel.addTour(newTour);

        // Select the tour
        TourViewModel tourViewModel = viewModel.getTours().get(0);
        viewModel.selectedTourProperty().set(tourViewModel);

        // Verify no logs initially
        assertEquals(0, tourViewModel.getTourLogs().size(), "Should have no tour logs initially");

        // Create a tour log
        TourLog tourLog = new TourLog(
                LocalDateTime.now(),
                "Test comment",
                5,
                100.0,
                60,
                4
        );

        // Add the log
        viewModel.addTourLog(tourLog);

        // Verify it's added to the tour
        assertEquals(1, tourViewModel.getTourLogs().size(), "Should have one tour log");
        assertEquals("Test comment", tourViewModel.getTourLogs().get(0).commentProperty().get(), "Comment should match");
    }
}