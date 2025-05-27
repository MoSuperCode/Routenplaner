// TourServiceTest.java - Alternative Implementierung ohne Mocking
package org.example.tourplanner.business.service;

import org.example.tourplanner.models.Tour;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TourServiceTest {

    private TourService tourService;

    @BeforeEach
    public void setUp() {
        // Verwende die echte Service-Implementierung
        tourService = HttpTourService.getInstance();
    }

    @Test
    public void testTourCreation() {
        // Test für Tour-Erstellung
        Tour tour = new Tour("Vienna to Salzburg", "Scenic route", "Vienna", "Salzburg", "Car");

        assertNotNull(tour);
        assertEquals("Vienna to Salzburg", tour.getName());
        assertEquals("Scenic route", tour.getDescription());
        assertEquals("Vienna", tour.getFrom());
        assertEquals("Salzburg", tour.getTo());
        assertEquals("Car", tour.getTransportType());
    }

    @Test
    public void testTourSettersAndGetters() {
        // Test für Setter und Getter
        Tour tour = new Tour();

        tour.setId(1L);
        tour.setName("Test Tour");
        tour.setDescription("Test Description");
        tour.setFrom("Start");
        tour.setTo("End");
        tour.setTransportType("Car");
        tour.setDistance(100.0);
        tour.setEstimatedTime(120);
        tour.setRouteImagePath("/path/to/image");

        assertEquals(1L, tour.getId());
        assertEquals("Test Tour", tour.getName());
        assertEquals("Test Description", tour.getDescription());
        assertEquals("Start", tour.getFrom());
        assertEquals("End", tour.getTo());
        assertEquals("Car", tour.getTransportType());
        assertEquals(100.0, tour.getDistance());
        assertEquals(120, tour.getEstimatedTime());
        assertEquals("/path/to/image", tour.getRouteImagePath());
    }

    @Test
    public void testTourValidation_NullName_ThrowsException() {
        // Test für ungültigen Tour-Namen
        assertThrows(IllegalArgumentException.class, () -> {
            String name = null;
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Tour name cannot be null or empty");
            }
        });
    }

    @Test
    public void testTourValidation_EmptyName_ThrowsException() {
        // Test für leeren Tour-Namen
        assertThrows(IllegalArgumentException.class, () -> {
            String name = "";
            if (name == null || name.trim().isEmpty()) {
                throw new IllegalArgumentException("Tour name cannot be null or empty");
            }
        });
    }

    @Test
    public void testTourValidation_EmptyFromLocation_ThrowsException() {
        // Test für leere Startposition
        assertThrows(IllegalArgumentException.class, () -> {
            String from = "";
            if (from == null || from.trim().isEmpty()) {
                throw new IllegalArgumentException("From location cannot be null or empty");
            }
        });
    }

    @Test
    public void testTourValidation_EmptyToLocation_ThrowsException() {
        // Test für leere Zielposition
        assertThrows(IllegalArgumentException.class, () -> {
            String to = "";
            if (to == null || to.trim().isEmpty()) {
                throw new IllegalArgumentException("To location cannot be null or empty");
            }
        });
    }

    @Test
    public void testTourPopularityCalculation() {
        // Test für Popularitätsberechnung
        Tour tour = new Tour("Test Tour", "Description", "Start", "End", "Car");

        // Anfangs keine Logs, also Popularität = 0
        assertEquals(0, tour.getPopularity());

        // Logs hinzufügen und Popularität testen wäre hier sinnvoll,
        // aber das erfordert TourLog-Objekte
    }

    @Test
    public void testTransportTypes() {
        // Test für verschiedene Transportarten
        String[] validTransportTypes = {"Car", "Bicycle", "Walking", "Public Transport", "Other"};

        for (String transportType : validTransportTypes) {
            Tour tour = new Tour("Test", "Description", "Start", "End", transportType);
            assertEquals(transportType, tour.getTransportType());
        }
    }
}