package org.example.tourplanner.ui.viewmodels;

import org.example.tourplanner.models.Tour;
import org.example.tourplanner.models.TourLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class TourViewModelTest {

    private Tour tour;
    private TourViewModel viewModel;

    @BeforeEach
    public void setUp() {
        // Create a sample tour
        tour = new Tour("Test Tour", "Test Description", "Vienna", "Salzburg", "Car");
        tour.setId(1L);
        tour.setDistance(100.0);
        tour.setEstimatedTime(120);

        // Create the view model
        viewModel = new TourViewModel(tour);
    }

    @Test
    public void testInitialization() {
        // Check that the view model properties are correctly initialized from the tour
        assertEquals(1L, viewModel.idProperty().get(), "ID should be set correctly");
        assertEquals("Test Tour", viewModel.nameProperty().get(), "Name should be set correctly");
        assertEquals("Test Description", viewModel.descriptionProperty().get(), "Description should be set correctly");
        assertEquals("Vienna", viewModel.fromProperty().get(), "Starting point should be set correctly");
        assertEquals("Salzburg", viewModel.toProperty().get(), "Destination should be set correctly");
        assertEquals("Car", viewModel.transportTypeProperty().get(), "Transport type should be set correctly");
        assertEquals(100.0, viewModel.distanceProperty().get(), "Distance should be set correctly");
        assertEquals(120, viewModel.estimatedTimeProperty().get(), "Estimated time should be set correctly");
    }

    @Test
    public void testUpdateModel() {
        // Change view model properties
        viewModel.nameProperty().set("Updated Tour");
        viewModel.descriptionProperty().set("Updated Description");
        viewModel.fromProperty().set("Graz");
        viewModel.toProperty().set("Linz");
        viewModel.transportTypeProperty().set("Train");
        viewModel.distanceProperty().set(150.0);
        viewModel.estimatedTimeProperty().set(90);

        // Update the underlying model
        viewModel.updateModel();

        // Check that the model was updated
        assertEquals("Updated Tour", tour.getName(), "Name should be updated in model");
        assertEquals("Updated Description", tour.getDescription(), "Description should be updated in model");
        assertEquals("Graz", tour.getFrom(), "Starting point should be updated in model");
        assertEquals("Linz", tour.getTo(), "Destination should be updated in model");
        assertEquals("Train", tour.getTransportType(), "Transport type should be updated in model");
        assertEquals(150.0, tour.getDistance(), "Distance should be updated in model");
        assertEquals(90, tour.getEstimatedTime(), "Estimated time should be updated in model");
    }

    @Test
    public void testComputedProperties() {
        // Initially there are no logs, so popularity should be 0
        assertEquals(0, viewModel.popularityProperty().get(), "Initial popularity should be 0");

        // Add a tour log
        TourLog log1 = new TourLog(LocalDateTime.now(), "Good trip", 3, 100.0, 120, 4);
        log1.setId(1L);
        log1.setTour(tour);
        viewModel.addTourLog(log1);

        // Check that popularity was updated
        assertEquals(1, viewModel.popularityProperty().get(), "Popularity should be 1 after adding a log");

        // Child-friendliness is based on a formula with difficulty, distance and time
        // We can check that it's within the expected range (0-10)
        assertTrue(
                viewModel.childFriendlinessProperty().get() >= 0 &&
                        viewModel.childFriendlinessProperty().get() <= 10,
                "Child-friendliness should be between 0 and 10"
        );
    }
}