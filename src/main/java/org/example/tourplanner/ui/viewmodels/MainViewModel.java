package org.example.tourplanner.ui.viewmodels;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.business.service.TourLogService;
import org.example.tourplanner.business.service.TourLogServiceImpl;
import org.example.tourplanner.business.service.TourService;
import org.example.tourplanner.business.service.TourServiceImpl;
import org.example.tourplanner.models.Tour;
import org.example.tourplanner.models.TourLog;

import java.time.LocalDateTime;
import java.util.List;

public class MainViewModel extends BaseViewModel {
    private static final Logger logger = LogManager.getLogger(MainViewModel.class);

    // Services
    private final TourService tourService;
    private final TourLogService tourLogService;

    // Properties for binding
    private final ObservableList<TourViewModel> tours = FXCollections.observableArrayList();
    private final ObjectProperty<TourViewModel> selectedTour = new SimpleObjectProperty<>();
    private final ObjectProperty<TourLogViewModel> selectedTourLog = new SimpleObjectProperty<>();
    private final StringProperty searchTerm = new SimpleStringProperty("");
    private final FilteredList<TourViewModel> filteredTours;

    // Timeout für verzögerte Suche (in Millisekunden)
    private static final int SEARCH_DELAY = 300;
    private java.util.Timer searchTimer;

    public MainViewModel() {
        // Services initialisieren
        tourService = TourServiceImpl.getInstance();
        tourLogService = TourLogServiceImpl.getInstance();

        // Laden der Demo-Daten über Service, wenn keine Daten vorhanden sind
        if (tourService.getAllTours().isEmpty()) {
            createDemoData();
        }

        // Laden der Daten vom Service
        loadToursFromService();

        // Setup filtered list
        filteredTours = new FilteredList<>(tours, p -> true);

        // Add listener to searchTerm property to update the filter with delay
        searchTerm.addListener((observable, oldValue, newValue) -> {
            // Abbrechen des vorherigen Timers, falls vorhanden
            if (searchTimer != null) {
                searchTimer.cancel();
            }

            // Erstellen eines neuen Timers
            searchTimer = new java.util.Timer();
            searchTimer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    // Da dies in einem Timer-Thread läuft, müssen wir zurück zum JavaFX-Thread
                    javafx.application.Platform.runLater(() -> updateFilter(newValue));
                }
            }, SEARCH_DELAY);
        });

        logger.info("MainViewModel initialized with {} tours", tours.size());
    }

    // Methode zum Laden der Touren vom Service
    private void loadToursFromService() {
        tours.clear();
        List<Tour> allTours = tourService.getAllTours();
        for (Tour tour : allTours) {
            tours.add(new TourViewModel(tour));
        }
        logger.info("Loaded {} tours from service", tours.size());
    }

    private void updateFilter(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredTours.setPredicate(p -> true);
            logger.info("Empty search term, showing all {} tours", tours.size());
            return;
        }

        // Direkte Suche über den Service implementieren
        Task<List<Tour>> searchTask = new Task<>() {
            @Override
            protected List<Tour> call() {
                return tourService.searchTours(searchText);
            }
        };

        searchTask.setOnSucceeded(event -> {
            List<Tour> searchResults = searchTask.getValue();
            // Filter aktualisieren, um nur übereinstimmende Touren anzuzeigen
            filteredTours.setPredicate(tourViewModel ->
                    searchResults.stream().anyMatch(tour ->
                            tour.getId().equals(tourViewModel.getTour().getId())
                    )
            );
            logger.info("Filter updated, filtered tours count: {}", filteredTours.size());
        });

        // Bei Fehlern alle anzeigen
        searchTask.setOnFailed(event -> {
            logger.error("Search failed", searchTask.getException());
            filteredTours.setPredicate(p -> true);
        });

        // Suche im Hintergrund ausführen
        Thread searchThread = new Thread(searchTask);
        searchThread.setDaemon(true);
        searchThread.start();
    }

    private void createDemoData() {
        // Create some demo tours and logs
        Tour tour1 = new Tour("Vienna to Salzburg", "A beautiful trip through Austria",
                "Vienna", "Salzburg", "Car");
        tour1.setDistance(295.0);
        tour1.setEstimatedTime(180); // 3 hours

        // Tour im Service speichern
        tour1 = tourService.createTour(tour1);

        // TourLogs erstellen
        TourLog log1 = new TourLog(LocalDateTime.now().minusDays(5),
                "Great weather, enjoyed the trip", 3, 295.0, 185, 4);
        tourLogService.createTourLog(tour1.getId(), log1);

        TourLog log2 = new TourLog(LocalDateTime.now().minusDays(2),
                "Some traffic, but still good", 4, 298.0, 200, 3);
        tourLogService.createTourLog(tour1.getId(), log2);

        Tour tour2 = new Tour("Vienna to Graz", "Southern route through Styria",
                "Vienna", "Graz", "Train");
        tour2.setDistance(200.0);
        tour2.setEstimatedTime(150); // 2.5 hours

        // Tour im Service speichern
        tour2 = tourService.createTour(tour2);

        // TourLog erstellen
        TourLog log3 = new TourLog(LocalDateTime.now().minusWeeks(1),
                "Relaxing train ride", 2, 200.0, 145, 5);
        tourLogService.createTourLog(tour2.getId(), log3);

        logger.info("Demo data created");
    }

    // Tour Management
    public void addTour(Tour tour) {
        Tour createdTour = tourService.createTour(tour);
        TourViewModel viewModel = new TourViewModel(createdTour);
        tours.add(viewModel);
        logger.info("Added new tour: {}", createdTour.getName());
    }

    public void updateTour(TourViewModel viewModel) {
        viewModel.updateModel();
        Tour updatedTour = tourService.updateTour(viewModel.getTour());
        // Falls nötig, das ViewModel aktualisieren
        if (updatedTour != null) {
            // Keine direkten Änderungen notwendig, da das ViewModel bereits aktualisiert wurde
            logger.info("Updated tour: {}", updatedTour.getName());
        } else {
            logger.warn("Failed to update tour: {}", viewModel.nameProperty().get());
        }
    }

    public void deleteTour(TourViewModel viewModel) {
        Long tourId = viewModel.getTour().getId();
        tourService.deleteTour(tourId);
        tours.remove(viewModel);
        if (selectedTour.get() == viewModel) {
            selectedTour.set(null);
        }
        logger.info("Deleted tour: {}", viewModel.nameProperty().get());
    }

    // TourLog Management
    public void addTourLog(TourLog tourLog) {
        TourViewModel selectedTourViewModel = selectedTourProperty().get();

        if (selectedTourViewModel != null) {
            Long tourId = selectedTourViewModel.getTour().getId();
            TourLog createdLog = tourLogService.createTourLog(tourId, tourLog);

            if (createdLog != null) {
                // Update view model
                selectedTourViewModel.addTourLog(createdLog);
                logger.info("Added new tour log to tour: {}", selectedTourViewModel.nameProperty().get());

                // Refresh tour logs table view
                selectedTourLogProperty().set(null);
            } else {
                logger.warn("Failed to create tour log");
            }
        }
    }

    public void updateTourLog(TourLogViewModel viewModel) {
        viewModel.updateModel();  // updates model from view model
        TourLog updatedLog = tourLogService.updateTourLog(viewModel.getTourLog());

        if (updatedLog != null) {
            // refresh view model properties from updated model
            viewModel.refreshFromModel();
            logger.info("Tour log updated successfully");
        } else {
            logger.warn("Failed to update tour log");
        }
    }

    public void deleteTourLog(TourLogViewModel viewModel) {
        if (selectedTour.get() != null) {
            Long logId = viewModel.getTourLog().getId();
            tourLogService.deleteTourLog(logId);
            selectedTour.get().removeTourLog(viewModel);

            if (selectedTourLog.get() == viewModel) {
                selectedTourLog.set(null);
            }
            logger.info("Tour log deleted");
        }
    }

    // Methode zum Aktualisieren der Touren (kann bei einer Aktualisierung aus externen Quellen verwendet werden)
    public void refreshTours() {
        loadToursFromService();
    }

    // Properties
    public ObservableList<TourViewModel> getTours() {
        return tours;
    }

    public FilteredList<TourViewModel> getFilteredTours() {
        return filteredTours;
    }

    public ObjectProperty<TourViewModel> selectedTourProperty() {
        return selectedTour;
    }

    public ObjectProperty<TourLogViewModel> selectedTourLogProperty() {
        return selectedTourLog;
    }

    public StringProperty searchTermProperty() {
        return searchTerm;
    }
}