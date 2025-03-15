package org.example.tourplanner.ui.viewmodels;

import javafx.beans.property.*;
import org.example.tourplanner.models.TourLog;

import java.time.LocalDateTime;

public class TourLogViewModel extends BaseViewModel {
    private final TourLog tourLog;

    // Properties for binding to UI elements
    private final LongProperty id = new SimpleLongProperty();
    private final ObjectProperty<LocalDateTime> date = new SimpleObjectProperty<>();
    private final StringProperty comment = new SimpleStringProperty();
    private final IntegerProperty difficulty = new SimpleIntegerProperty();
    private final DoubleProperty totalDistance = new SimpleDoubleProperty();
    private final IntegerProperty totalTime = new SimpleIntegerProperty();
    private final IntegerProperty rating = new SimpleIntegerProperty();

    public TourLogViewModel(TourLog tourLog) {
        this.tourLog = tourLog;
        initializeProperties();
        registerAllProperties();
    }

    private void initializeProperties() {
        id.set(tourLog.getId() != null ? tourLog.getId() : 0);
        date.set(tourLog.getDate());
        comment.set(tourLog.getComment());
        difficulty.set(tourLog.getDifficulty());
        totalDistance.set(tourLog.getTotalDistance());
        totalTime.set(tourLog.getTotalTime());
        rating.set(tourLog.getRating());
    }

    private void registerAllProperties() {
        registerProperty(id);
        registerProperty(date);
        registerProperty(comment);
        registerProperty(difficulty);
        registerProperty(totalDistance);
        registerProperty(totalTime);
        registerProperty(rating);
    }

    // Method to update the model from the viewmodel
    public void updateModel() {
        tourLog.setDate(date.get());
        tourLog.setComment(comment.get());
        tourLog.setDifficulty(difficulty.get());
        tourLog.setTotalDistance(totalDistance.get());
        tourLog.setTotalTime(totalTime.get());
        tourLog.setRating(rating.get());
    }

    // Getters for properties to bind to UI
    public LongProperty idProperty() {
        return id;
    }

    public ObjectProperty<LocalDateTime> dateProperty() {
        return date;
    }

    public StringProperty commentProperty() {
        return comment;
    }

    public IntegerProperty difficultyProperty() {
        return difficulty;
    }

    public DoubleProperty totalDistanceProperty() {
        return totalDistance;
    }

    public IntegerProperty totalTimeProperty() {
        return totalTime;
    }

    public IntegerProperty ratingProperty() {
        return rating;
    }

    // Utility methods
    public TourLog getTourLog() {
        return tourLog;
    }
}