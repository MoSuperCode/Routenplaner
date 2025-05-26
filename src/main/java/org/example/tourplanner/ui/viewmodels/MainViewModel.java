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
import org.example.tourplanner.business.service.HttpTourLogService;
import org.example.tourplanner.business.service.HttpTourService;
import org.example.tourplanner.business.service.TourLogService;
import org.example.tourplanner.business.service.TourService;
import org.example.tourplanner.models.Tour;
import org.example.tourplanner.models.TourLog;

import java.util.List;

public class MainViewModel extends BaseViewModel {
    private static final Logger logger = LogManager.getLogger(MainViewModel.class);

    // Services - now using HTTP services
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
        // Services initialisieren - now using HTTP services
        tourService = HttpTourService.getInstance();
        tourLogService = HttpTourLogService.getInstance();

        // Load data from backend
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

        // Load tours in background thread to avoid blocking UI
        Task<List<Tour>> loadTask = new Task<>() {
            @Override
            protected List<Tour> call() {
                return tourService.getAllTours();
            }
        };

        loadTask.setOnSucceeded(event -> {
            List<Tour> allTours = loadTask.getValue();

            // Load tour logs for each tour
            for (Tour tour : allTours) {
                // Load tour logs from backend
                List<TourLog> tourLogs = tourLogService.getTourLogs(tour.getId());
                tour.getTourLogs().clear();
                tour.getTourLogs().addAll(tourLogs);

                tours.add(new TourViewModel(tour));
            }
            logger.info("Loaded {} tours from backend", tours.size());
        });

        loadTask.setOnFailed(event -> {
            logger.error("Failed to load tours from backend", loadTask.getException());
        });

        Thread loadThread = new Thread(loadTask);
        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void updateFilter(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredTours.setPredicate(p -> true);
            logger.info("Empty search term, showing all {} tours", tours.size());
            return;
        }

        // Search via backend service
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

    // Tour Management
    public void addTour(Tour tour) {
        Task<Tour> createTask = new Task<>() {
            @Override
            protected Tour call() {
                return tourService.createTour(tour);
            }
        };

        createTask.setOnSucceeded(event -> {
            Tour createdTour = createTask.getValue();
            if (createdTour != null) {
                TourViewModel viewModel = new TourViewModel(createdTour);
                tours.add(viewModel);
                logger.info("Added new tour: {}", createdTour.getName());
            }
        });

        createTask.setOnFailed(event -> {
            logger.error("Failed to create tour", createTask.getException());
        });

        Thread createThread = new Thread(createTask);
        createThread.setDaemon(true);
        createThread.start();
    }

    public void updateTour(TourViewModel viewModel) {
        viewModel.updateModel();

        Task<Tour> updateTask = new Task<>() {
            @Override
            protected Tour call() {
                return tourService.updateTour(viewModel.getTour());
            }
        };

        updateTask.setOnSucceeded(event -> {
            Tour updatedTour = updateTask.getValue();
            if (updatedTour != null) {
                viewModel.updateFromModel();
                logger.info("Updated tour: {}", updatedTour.getName());
            } else {
                logger.warn("Failed to update tour: {}", viewModel.nameProperty().get());
            }
        });

        updateTask.setOnFailed(event -> {
            logger.error("Failed to update tour", updateTask.getException());
        });

        Thread updateThread = new Thread(updateTask);
        updateThread.setDaemon(true);
        updateThread.start();
    }

    public void deleteTour(TourViewModel viewModel) {
        Long tourId = viewModel.getTour().getId();

        Task<Void> deleteTask = new Task<>() {
            @Override
            protected Void call() {
                tourService.deleteTour(tourId);
                return null;
            }
        };

        deleteTask.setOnSucceeded(event -> {
            tours.remove(viewModel);
            if (selectedTour.get() == viewModel) {
                selectedTour.set(null);
            }
            logger.info("Deleted tour: {}", viewModel.nameProperty().get());
        });

        deleteTask.setOnFailed(event -> {
            logger.error("Failed to delete tour", deleteTask.getException());
        });

        Thread deleteThread = new Thread(deleteTask);
        deleteThread.setDaemon(true);
        deleteThread.start();
    }

    // TourLog Management
    public void addTourLog(TourLog tourLog) {
        TourViewModel selectedTourViewModel = selectedTourProperty().get();

        if (selectedTourViewModel != null) {
            Long tourId = selectedTourViewModel.getTour().getId();

            Task<TourLog> createLogTask = new Task<>() {
                @Override
                protected TourLog call() {
                    return tourLogService.createTourLog(tourId, tourLog);
                }
            };

            createLogTask.setOnSucceeded(event -> {
                TourLog createdLog = createLogTask.getValue();
                if (createdLog != null) {
                    // Update view model
                    selectedTourViewModel.addTourLog(createdLog);
                    logger.info("Added new tour log to tour: {}", selectedTourViewModel.nameProperty().get());

                    // Refresh tour logs table view
                    selectedTourLogProperty().set(null);
                } else {
                    logger.warn("Failed to create tour log");
                }
            });

            createLogTask.setOnFailed(event -> {
                logger.error("Failed to create tour log", createLogTask.getException());
            });

            Thread createLogThread = new Thread(createLogTask);
            createLogThread.setDaemon(true);
            createLogThread.start();
        }
    }

    public void updateTourLog(TourLogViewModel viewModel) {
        viewModel.updateModel();  // updates model from view model

        Task<TourLog> updateLogTask = new Task<>() {
            @Override
            protected TourLog call() {
                return tourLogService.updateTourLog(viewModel.getTourLog());
            }
        };

        updateLogTask.setOnSucceeded(event -> {
            TourLog updatedLog = updateLogTask.getValue();
            if (updatedLog != null) {
                // refresh view model properties from updated model
                viewModel.refreshFromModel();
                logger.info("Tour log updated successfully");
            } else {
                logger.warn("Failed to update tour log");
            }
        });

        updateLogTask.setOnFailed(event -> {
            logger.error("Failed to update tour log", updateLogTask.getException());
        });

        Thread updateLogThread = new Thread(updateLogTask);
        updateLogThread.setDaemon(true);
        updateLogThread.start();
    }

    public void deleteTourLog(TourLogViewModel viewModel) {
        if (selectedTour.get() != null) {
            Long logId = viewModel.getTourLog().getId();

            Task<Void> deleteLogTask = new Task<>() {
                @Override
                protected Void call() {
                    tourLogService.deleteTourLog(logId);
                    return null;
                }
            };

            deleteLogTask.setOnSucceeded(event -> {
                selectedTour.get().removeTourLog(viewModel);
                if (selectedTourLog.get() == viewModel) {
                    selectedTourLog.set(null);
                }
                logger.info("Tour log deleted");
            });

            deleteLogTask.setOnFailed(event -> {
                logger.error("Failed to delete tour log", deleteLogTask.getException());
            });

            Thread deleteLogThread = new Thread(deleteLogTask);
            deleteLogThread.setDaemon(true);
            deleteLogThread.start();
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