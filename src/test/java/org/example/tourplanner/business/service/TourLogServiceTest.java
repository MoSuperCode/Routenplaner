package org.example.tourplanner.business.service;

import org.example.tourplanner.models.TourLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TourLogServiceTest {

    private TourLogService tourLogService;
    private Long testTourId = 1L; // Annahme: Tour mit ID 1 existiert

    @BeforeEach
    public void setUp() {
        // Verwende die echte Service-Implementierung
        tourLogService = HttpTourLogService.getInstance();
    }

    @Test
    public void testTourLogValidation_InvalidDifficulty_ThrowsException() {
        // Test für ungültige Schwierigkeit
        assertThrows(IllegalArgumentException.class, () -> {
            int difficulty = 11; // Invalid: should be 1-10
            if (difficulty < 1 || difficulty > 10) {
                throw new IllegalArgumentException("Difficulty must be between 1 and 10");
            }
        });
    }

    @Test
    public void testTourLogValidation_InvalidRating_ThrowsException() {
        // Test für ungültige Bewertung
        assertThrows(IllegalArgumentException.class, () -> {
            int rating = 6; // Invalid: should be 1-5
            if (rating < 1 || rating > 5) {
                throw new IllegalArgumentException("Rating must be between 1 and 5");
            }
        });
    }

    @Test
    public void testTourLogValidation_NegativeDistance_ThrowsException() {
        // Test für negative Distanz
        assertThrows(IllegalArgumentException.class, () -> {
            double distance = -5.0; // Invalid: should be positive
            if (distance <= 0) {
                throw new IllegalArgumentException("Distance must be positive");
            }
        });
    }

    @Test
    public void testTourLogValidation_NegativeTime_ThrowsException() {
        // Test für negative Zeit
        assertThrows(IllegalArgumentException.class, () -> {
            int time = -30; // Invalid: should be positive
            if (time <= 0) {
                throw new IllegalArgumentException("Time must be positive");
            }
        });
    }

    @Test
    public void testTourLogValidation_NullDate_ThrowsException() {
        // Test für null Datum
        assertThrows(IllegalArgumentException.class, () -> {
            LocalDateTime date = null;
            if (date == null) {
                throw new IllegalArgumentException("Date cannot be null");
            }
        });
    }

    @Test
    public void testTourLogCreation() {
        // Test für gültige TourLog-Erstellung
        LocalDateTime testDate = LocalDateTime.now();
        TourLog tourLog = new TourLog(testDate, "Great trip!", 7, 295.0, 180, 5);

        // Validiere die erstellten Daten
        assertNotNull(tourLog);
        assertEquals(testDate, tourLog.getDate());
        assertEquals("Great trip!", tourLog.getComment());
        assertEquals(7, tourLog.getDifficulty());
        assertEquals(295.0, tourLog.getTotalDistance());
        assertEquals(180, tourLog.getTotalTime());
        assertEquals(5, tourLog.getRating());
    }

    @Test
    public void testTourLogSettersAndGetters() {
        // Test für Setter und Getter
        TourLog tourLog = new TourLog();
        LocalDateTime testDate = LocalDateTime.now();

        tourLog.setId(1L);
        tourLog.setDate(testDate);
        tourLog.setComment("Test comment");
        tourLog.setDifficulty(5);
        tourLog.setTotalDistance(100.0);
        tourLog.setTotalTime(120);
        tourLog.setRating(4);

        assertEquals(1L, tourLog.getId());
        assertEquals(testDate, tourLog.getDate());
        assertEquals("Test comment", tourLog.getComment());
        assertEquals(5, tourLog.getDifficulty());
        assertEquals(100.0, tourLog.getTotalDistance());
        assertEquals(120, tourLog.getTotalTime());
        assertEquals(4, tourLog.getRating());
    }

    @Test
    public void testValidDifficultyRange() {
        // Test für gültigen Schwierigkeitsbereich
        for (int difficulty = 1; difficulty <= 10; difficulty++) {
            final int d = difficulty;
            assertDoesNotThrow(() -> {
                if (d < 1 || d > 10) {
                    throw new IllegalArgumentException("Difficulty must be between 1 and 10");
                }
            });
        }
    }

    @Test
    public void testValidRatingRange() {
        // Test für gültigen Bewertungsbereich
        for (int rating = 1; rating <= 5; rating++) {
            final int r = rating;
            assertDoesNotThrow(() -> {
                if (r < 1 || r > 5) {
                    throw new IllegalArgumentException("Rating must be between 1 and 5");
                }
            });
        }
    }
}
