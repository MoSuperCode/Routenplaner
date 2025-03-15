package org.example.tourplanner.ui.viewmodels;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.models.Tour;
import org.example.tourplanner.models.TourLog;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MainViewModel extends BaseViewModel {
    private static final Logger logger = LogManager.getLogger(MainViewModel.class);

    // Properties for binding
    private final ObservableList<TourViewModel> tours = FXCollections.observableArrayList();
    private final ObjectProperty<TourViewModel> selectedTour = new SimpleObjectProperty<>();
    private final ObjectProperty<TourLogViewModel> selectedTourLog = new SimpleObjectProperty<>();
    private final StringProperty searchTerm = new SimpleStringProperty("");
    private final FilteredList<TourViewModel> filteredTours;

    public MainViewModel() {
        // Initialize with some demo data for now
        createDemoData();

        // Setup filtered list
        filteredTours = new FilteredList<>(tours, p -> true);

        // Add listener to searchTerm property to update the filter
        searchTerm.addListener((observable, oldValue, newValue) ->
                updateFilter(newValue)
        );

        logger.info("MainViewModel initialized with {} tours", tours.size());
    }

    private void updateFilter(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            filteredTours.setPredicate(p -> true);
        } else {
            String lowerCaseFilter = searchText.toLowerCase();

            filteredTours.setPredicate(tour -> {
                // Check if tour name contains filter
                if (tour.nameProperty().get().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                // Check if description contains filter
                if (tour.descriptionProperty().get().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                // Check if from/to contains filter
                if (tour.fromProperty().get().toLowerCase().contains(lowerCaseFilter) ||
                        tour.toProperty().get().toLowerCase().contains(lowerCaseFilter)) {
                    return true;
                }
                // Check in tour logs
                for (TourLogViewModel logVM : tour.getTourLogs()) {
                    if (logVM.commentProperty().get().toLowerCase().contains(lowerCaseFilter)) {
                        return true;
                    }
                }
                return false;
            });
        }

        logger.info("Filter updated with term '{}', filtered tours count: {}",
                searchText, filteredTours.size());
    }

    private void createDemoData() {
        // Create some demo tours and logs
        Tour tour1 = new Tour("Vienna to Salzburg", "A beautiful trip through Austria",
                "Vienna", "Salzburg", "Car");
        tour1.setId(1L);
        tour1.setDistance(295.0);
        tour1.setEstimatedTime(180); // 3 hours

        TourLog log1 = new TourLog(LocalDateTime.now().minusDays(5),
                "Great weather, enjoyed the trip", 3, 295.0, 185, 4);
        log1.setId(1L);
        log1.setTour(tour1);

        TourLog log2 = new TourLog(LocalDateTime.now().minusDays(2),
                "Some traffic, but still good", 4, 298.0, 200, 3);
        log2.setId(2L);
        log2.setTour(tour1);

        tour1.addTourLog(log1);
        tour1.addTourLog(log2);

        Tour tour2 = new Tour("Vienna to Graz", "Southern route through Styria",
                "Vienna", "Graz", "Train");
        tour2.setId(2L);
        tour2.setDistance(200.0);
        tour2.setEstimatedTime(150); // 2.5 hours

        TourLog log3 = new TourLog(LocalDateTime.now().minusWeeks(1),
                "Relaxing train ride", 2, 200.0, 145, 5);
        log3.setId(3L);
        log3.setTour(tour2);
        tour2.addTourLog(log3);

        // Add to observable list
        tours.add(new TourViewModel(tour1));
        tours.add(new TourViewModel(tour2));
    }

    // Tour Management
    public void addTour(Tour tour) {
        TourViewModel viewModel = new TourViewModel(tour);
        tours.add(viewModel);
        logger.info("Added new tour: {}", tour.getName());
    }

    public void updateTour(TourViewModel viewModel) {
        viewModel.updateModel();
        // In a real application, you would save to database here
        logger.info("Updated tour: {}", viewModel.nameProperty().get());
    }

    public void deleteTour(TourViewModel viewModel) {
        tours.remove(viewModel);
        if (selectedTour.get() == viewModel) {
            selectedTour.set(null);
        }
        logger.info("Deleted tour: {}", viewModel.nameProperty().get());
    }

    // TourLog Management
    public void addTourLog(TourLog tourLog) {
        if (selectedTour.get() != null) {
            selectedTour.get().addTourLog(tourLog);
            logger.info("Added new tour log to tour: {}", selectedTour.get().nameProperty().get());
        }
    }

    public void updateTourLog(TourLogViewModel viewModel) {
        viewModel.updateModel();
        // In a real application, you would save to database here
        logger.info("Updated tour log");
    }

    public void deleteTourLog(TourLogViewModel viewModel) {
        if (selectedTour.get() != null) {
            selectedTour.get().removeTourLog(viewModel);
            if (selectedTourLog.get() == viewModel) {
                selectedTourLog.set(null);
            }
            logger.info("Deleted tour log");
        }
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