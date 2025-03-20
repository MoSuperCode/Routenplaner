package org.example.tourplanner.ui.viewmodels;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.example.tourplanner.models.Tour;
import org.example.tourplanner.models.TourLog;

public class TourViewModel extends BaseViewModel {
    private final Tour tour;

    // Properties for binding to UI elements
    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final StringProperty from = new SimpleStringProperty();
    private final StringProperty to = new SimpleStringProperty();
    private final StringProperty transportType = new SimpleStringProperty();
    private final DoubleProperty distance = new SimpleDoubleProperty();
    private final IntegerProperty estimatedTime = new SimpleIntegerProperty();
    private final StringProperty routeImagePath = new SimpleStringProperty();
    private final ObservableList<TourLogViewModel> tourLogs = FXCollections.observableArrayList();

    // Computed properties
    private final IntegerProperty popularity = new SimpleIntegerProperty();
    private final DoubleProperty childFriendliness = new SimpleDoubleProperty();

    public TourViewModel(Tour tour) {
        this.tour = tour;
        initializeProperties();
        registerAllProperties();
    }

    private void initializeProperties() {
        id.set(tour.getId() != null ? tour.getId() : 0);
        name.set(tour.getName());
        description.set(tour.getDescription());
        from.set(tour.getFrom());
        to.set(tour.getTo());
        transportType.set(tour.getTransportType());
        distance.set(tour.getDistance());
        estimatedTime.set(tour.getEstimatedTime());
        routeImagePath.set(tour.getRouteImagePath());
        popularity.set(tour.getPopularity());
        childFriendliness.set(tour.getChildFriendliness());

        // Convert tour logs to view models
        updateTourLogs();
    }

    private void updateTourLogs() {
        tourLogs.clear();
        if (tour.getTourLogs() != null) {
            for (TourLog log : tour.getTourLogs()) {
                tourLogs.add(new TourLogViewModel(log));
            }
        }
    }

    private void registerAllProperties() {
        registerProperty(id);
        registerProperty(name);
        registerProperty(description);
        registerProperty(from);
        registerProperty(to);
        registerProperty(transportType);
        registerProperty(distance);
        registerProperty(estimatedTime);
        registerProperty(routeImagePath);
        registerProperty(popularity);
        registerProperty(childFriendliness);
    }

    // Method to update the model from the viewmodel
    public void updateModel() {
        tour.setName(name.get());
        tour.setDescription(description.get());
        tour.setFrom(from.get());
        tour.setTo(to.get());
        tour.setTransportType(transportType.get());
        tour.setDistance(distance.get());
        tour.setEstimatedTime(estimatedTime.get());
        tour.setRouteImagePath(routeImagePath.get());
    }

    public void updateFromModel() {
        name.set(tour.getName());
        description.set(tour.getDescription());
        from.set(tour.getFrom());
        to.set(tour.getTo());
        transportType.set(tour.getTransportType());
        distance.set(tour.getDistance());
        estimatedTime.set(tour.getEstimatedTime());
        routeImagePath.set(tour.getRouteImagePath());
        popularity.set(tour.getPopularity());
        childFriendliness.set(tour.getChildFriendliness());
    }

    // Getters for properties to bind to UI
    public LongProperty idProperty() {
        return id;
    }

    public StringProperty nameProperty() {
        return name;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public StringProperty fromProperty() {
        return from;
    }

    public StringProperty toProperty() {
        return to;
    }

    public StringProperty transportTypeProperty() {
        return transportType;
    }

    public DoubleProperty distanceProperty() {
        return distance;
    }

    public IntegerProperty estimatedTimeProperty() {
        return estimatedTime;
    }

    public StringProperty routeImagePathProperty() {
        return routeImagePath;
    }

    public ObservableList<TourLogViewModel> getTourLogs() {
        return tourLogs;
    }

    public IntegerProperty popularityProperty() {
        return popularity;
    }

    public DoubleProperty childFriendlinessProperty() {
        return childFriendliness;
    }

    // Utility methods
    public Tour getTour() {
        return tour;
    }

    public void addTourLog(TourLog tourLog) {
        tour.addTourLog(tourLog);
        tourLogs.add(new TourLogViewModel(tourLog));

        // Update computed properties
        popularity.set(tour.getPopularity());
        childFriendliness.set(tour.getChildFriendliness());
    }

    public void removeTourLog(TourLogViewModel tourLogViewModel) {
        tour.removeTourLog(tourLogViewModel.getTourLog());
        tourLogs.remove(tourLogViewModel);

        // Update computed properties
        popularity.set(tour.getPopularity());
        childFriendliness.set(tour.getChildFriendliness());
    }
}