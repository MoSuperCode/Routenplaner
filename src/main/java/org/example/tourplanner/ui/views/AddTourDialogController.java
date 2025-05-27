package org.example.tourplanner.ui.views;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.business.service.BackendRouteService;
import org.example.tourplanner.models.Tour;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.scene.control.ProgressBar;
import java.util.Locale;


public class AddTourDialogController {
    private static final Logger logger = LogManager.getLogger(AddTourDialogController.class);
    private boolean skipValidation = false;

    @FXML
    private TextField nameField;

    @FXML
    private TextField fromField;

    @FXML
    private TextField toField;

    @FXML
    private ComboBox<String> transportTypeComboBox;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private TextField distanceField;

    @FXML
    private TextField timeField;

    @FXML
    private Button calculateButton;

    @FXML
    private ProgressBar progressBar;

    private Tour tour;
    private boolean saveClicked = false;
    private Stage dialogStage;

    @FXML
    private void initialize() {
        // Initialize transport type options
        transportTypeComboBox.setItems(FXCollections.observableArrayList(
                "Car", "Bicycle", "Walking", "Public Transport", "Other"
        ));

        // FIXED Input validation - mit skipValidation Flag
        distanceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!skipValidation && newValue != null && !newValue.isEmpty()) {
                // Bessere Regex die auch Dezimalzahlen wie "194.19" erlaubt
                if (!newValue.matches("\\d*(\\.\\d*)?")) {
                    logger.debug("Distance validation blocked: '{}' -> reverting to '{}'", newValue, oldValue);
                    distanceField.setText(oldValue);
                } else {
                    logger.debug("Distance validation passed: '{}'", newValue);
                }
            }
        });

        timeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!skipValidation && newValue != null && !newValue.isEmpty()) {
                if (!newValue.matches("\\d*")) {
                    logger.debug("Time validation blocked: '{}' -> reverting to '{}'", newValue, oldValue);
                    timeField.setText(oldValue);
                } else {
                    logger.debug("Time validation passed: '{}'", newValue);
                }
            }
        });
    }

    // Hilfsmethode um Werte ohne Validation zu setzen
    private void setFieldWithoutValidation(TextField field, String value) {
        logger.info("Setting field without validation: '{}'", value);
        skipValidation = true;
        field.setText(value);
        skipValidation = false;
        logger.info("Field value after setting: '{}'", field.getText());
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setTour(Tour tour) {
        this.tour = tour;

        // Fill fields with tour data if editing an existing tour
        if (tour.getId() != null) {
            nameField.setText(tour.getName());
            fromField.setText(tour.getFrom());
            toField.setText(tour.getTo());
            transportTypeComboBox.setValue(tour.getTransportType());
            descriptionArea.setText(tour.getDescription());

            // Debug: Logge die aktuellen Werte des Tour-Objekts
            logger.info("Setting tour in dialog - Tour distance: {}, Tour time: {}",
                    tour.getDistance(), tour.getEstimatedTime());

            // WICHTIG: Verwende die neue Methode ohne Validation
            setFieldWithoutValidation(distanceField, String.format(Locale.US, "%.1f", tour.getDistance()));
            setFieldWithoutValidation(timeField, Integer.toString(tour.getEstimatedTime()));

            // Debug: Logge was tatsächlich in die Felder gesetzt wurde
            logger.info("After setting fields - Distance field: '{}', Time field: '{}'",
                    distanceField.getText(), timeField.getText());
        } else {
            // Set defaults for new tour
            transportTypeComboBox.setValue("Car");
            setFieldWithoutValidation(distanceField, "0.0");
            setFieldWithoutValidation(timeField, "0");

            logger.info("Setting defaults for new tour");
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public Tour getTour() {
        return tour;
    }

    @FXML
    private void onSave() {
        if (isInputValid()) {
            // Update tour with form values - WICHTIG: Lies die aktuellen Werte aus den Feldern
            tour.setName(nameField.getText());
            tour.setFrom(fromField.getText());
            tour.setTo(toField.getText());
            tour.setTransportType(transportTypeComboBox.getValue());
            tour.setDescription(descriptionArea.getText());

            // Parse die Werte aus den Feldern (nicht aus den alten Objektwerten)
            try {
                double distance = Double.parseDouble(distanceField.getText().replace(',', '.'));
                int estimatedTime = Integer.parseInt(timeField.getText());

                tour.setDistance(distance);
                tour.setEstimatedTime(estimatedTime);

                logger.info("Saving tour with - Distance: {}km, Time: {}min", distance, estimatedTime);
            } catch (NumberFormatException e) {
                logger.error("Error parsing distance or time values", e);
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Invalid Values");
                alert.setHeaderText("Invalid distance or time values");
                alert.setContentText("Please ensure distance and time contain valid numbers.");
                alert.showAndWait();
                return;
            }

            saveClicked = true;
            dialogStage.close();
            logger.info("Tour saved: {} (Distance: {}km, Time: {}min)",
                    tour.getName(), tour.getDistance(), tour.getEstimatedTime());
        }
    }

    @FXML
    private void onCancel() {
        dialogStage.close();
        logger.info("Tour edit canceled");
    }

    @FXML
    private void onCalculateRoute() {
        if (fromField.getText().isEmpty() || toField.getText().isEmpty() || transportTypeComboBox.getValue() == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Missing Information");
            alert.setHeaderText("Please enter From, To locations and select a Transport Type");
            alert.setContentText("All fields must be filled to calculate a route.");
            alert.showAndWait();
            return;
        }

        // Zeige Wartecursor an
        dialogStage.getScene().setCursor(Cursor.WAIT);

        // Optional: ProgressBar anzeigen, falls vorhanden
        if (progressBar != null) {
            progressBar.setVisible(true);
        }

        // Erstelle eine Hintergrundaufgabe, um die UI nicht zu blockieren
        Task<BackendRouteService.RouteCalculationResult> calculateRouteTask = new Task<>() {
            @Override
            protected BackendRouteService.RouteCalculationResult call() {
                try {
                    BackendRouteService routeService = BackendRouteService.getInstance();
                    return routeService.calculateRoute(
                            fromField.getText(),
                            toField.getText(),
                            transportTypeComboBox.getValue()
                    );
                } catch (Exception e) {
                    logger.error("Error calculating route", e);
                    return null;
                }
            }
        };

        // Handle Aufgabenabschluss
        calculateRouteTask.setOnSucceeded(event -> {
            // Cursor zurücksetzen
            dialogStage.getScene().setCursor(Cursor.DEFAULT);

            // Optional: ProgressBar ausblenden
            if (progressBar != null) {
                progressBar.setVisible(false);
            }

            BackendRouteService.RouteCalculationResult result = calculateRouteTask.getValue();
            if (result != null && result.isSuccess()) {
                Platform.runLater(() -> {
                    try {
                        // Debug: Aktuelle Feldwerte vor Update
                        logger.info("Before update - Distance field: '{}', Time field: '{}'",
                                distanceField.getText(), timeField.getText());
                        logger.info("Result values - Distance: {}, Time: {}",
                                result.getDistance(), result.getEstimatedTime());

                        // 1. Tour-Objekt aktualisieren
                        tour.setDistance(result.getDistance());
                        tour.setEstimatedTime(result.getEstimatedTime());
                        if (result.getRouteImagePath() != null && !result.getRouteImagePath().isEmpty()) {
                            tour.setRouteImagePath(result.getRouteImagePath());
                        }

                        // 2. UI-Felder mit der neuen Methode ohne Validation setzen
                        String formattedDistance = String.format(Locale.US, "%.2f", result.getDistance());
                        setFieldWithoutValidation(distanceField, formattedDistance);
                        setFieldWithoutValidation(timeField, String.valueOf(result.getEstimatedTime()));

                        // Debug: Werte nach Update
                        logger.info("After update - Distance field: '{}', Time field: '{}'",
                                distanceField.getText(), timeField.getText());
                        logger.info("Tour object - Distance: {}, Time: {}",
                                tour.getDistance(), tour.getEstimatedTime());

                    } catch (Exception e) {
                        logger.error("Error updating UI fields", e);
                    }
                });

                // Erfolgsmeldung anzeigen
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Route Calculation");
                    alert.setHeaderText("Route Calculated Successfully");
                    alert.setContentText(String.format(
                            "Distance: %.2f km\nEstimated Time: %d minutes",
                            result.getDistance(),
                            result.getEstimatedTime()
                    ));
                    alert.showAndWait();
                });
            } else {
                // Fehlermeldung anzeigen
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Route Calculation Failed");
                    alert.setHeaderText("Could not calculate route");
                    alert.setContentText(result != null ? result.getMessage() :
                            "Please check your internet connection and the validity of the locations.");
                    alert.showAndWait();
                });
            }
        });

        calculateRouteTask.setOnFailed(event -> {
            // Cursor zurücksetzen
            dialogStage.getScene().setCursor(Cursor.DEFAULT);

            // Optional: ProgressBar ausblenden
            if (progressBar != null) {
                progressBar.setVisible(false);
            }

            // Fehlermeldung anzeigen
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Route Calculation Failed");
                alert.setHeaderText("Error During Route Calculation");
                alert.setContentText("An error occurred: " + calculateRouteTask.getException().getMessage());
                alert.showAndWait();
            });
        });

        // Starten der Aufgabe in einem Hintergrundthread
        Thread thread = new Thread(calculateRouteTask);
        thread.setDaemon(true);
        thread.start();

        logger.info("Route calculation requested for {} to {}", fromField.getText(), toField.getText());
    }

    // Input-Validation
    private boolean isInputValid() {
        String errorMessage = "";

        if (nameField.getText() == null || nameField.getText().isEmpty()) {
            errorMessage += "No valid name provided!\n";
        }

        if (fromField.getText() == null || fromField.getText().isEmpty()) {
            errorMessage += "No valid starting location provided!\n";
        }

        if (toField.getText() == null || toField.getText().isEmpty()) {
            errorMessage += "No valid destination provided!\n";
        }

        if (transportTypeComboBox.getValue() == null) {
            errorMessage += "No transport type selected!\n";
        }

        if (distanceField.getText() == null || distanceField.getText().isEmpty()) {
            errorMessage += "No valid distance provided!\n";
        }

        if (timeField.getText() == null || timeField.getText().isEmpty()) {
            errorMessage += "No valid time provided!\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Fields");
            alert.setHeaderText("Please correct invalid fields");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }
}