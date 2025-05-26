package org.example.tourplanner.ui.views;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.models.Tour;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import org.example.tourplanner.business.service.RouteService;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import org.example.tourplanner.business.service.RouteService;
import javafx.scene.control.ProgressBar;

public class AddTourDialogController {
    private static final Logger logger = LogManager.getLogger(AddTourDialogController.class);

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

    private Tour tour;
    private boolean saveClicked = false;
    private Stage dialogStage;

    @FXML
    private void initialize() {
        // Initialize transport type options
        transportTypeComboBox.setItems(FXCollections.observableArrayList(
                "Car", "Bicycle", "Walking", "Public Transport", "Other"
        ));

        // Input validation for numeric fields
        distanceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                distanceField.setText(oldValue);
            }
        });

        timeField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                timeField.setText(oldValue);
            }
        });
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
            distanceField.setText(Double.toString(tour.getDistance()));
            timeField.setText(Integer.toString(tour.getEstimatedTime()));
        } else {
            // Set defaults for new tour
            transportTypeComboBox.setValue("Car");
            distanceField.setText("0.0");
            timeField.setText("0");
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
            // Update tour with form values
            tour.setName(nameField.getText());
            tour.setFrom(fromField.getText());
            tour.setTo(toField.getText());
            tour.setTransportType(transportTypeComboBox.getValue());
            tour.setDescription(descriptionArea.getText());
            tour.setDistance(Double.parseDouble(distanceField.getText()));
            tour.setEstimatedTime(Integer.parseInt(timeField.getText()));

            saveClicked = true;
            dialogStage.close();
            logger.info("Tour saved: {}", tour.getName());
        }
    }

    @FXML
    private void onCancel() {
        dialogStage.close();
        logger.info("Tour edit canceled");
    }




    @FXML
    private ProgressBar progressBar; // ProgressBar zur tour-dialog.fxml hinzufügen

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
        Task<Tour> calculateRouteTask = new Task<>() {
            @Override
            protected Tour call() {
                try {
                    // Erstelle temporäre Tour für die Berechnung
                    Tour tempTour = new Tour(
                            nameField.getText(),
                            descriptionArea.getText(),
                            fromField.getText(),
                            toField.getText(),
                            transportTypeComboBox.getValue()
                    );

                    // Berechne Route mithilfe von RouteService
                    RouteService routeService = RouteService.getInstance();
                    Tour calculatedTour = routeService.calculateRoute(tempTour);
                    return calculatedTour;
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

            Tour calculatedTour = calculateRouteTask.getValue();
            if (calculatedTour != null) {
                // UI-Felder mit berechneten Werten aktualisieren - HIER WAR DAS PROBLEM!
                Platform.runLater(() -> {
                    distanceField.setText(String.format("%.2f", calculatedTour.getDistance()));
                    timeField.setText(String.valueOf(calculatedTour.getEstimatedTime()));

                    // WICHTIG: Übertrage die berechneten Werte auch in das tour Objekt
                    tour.setDistance(calculatedTour.getDistance());
                    tour.setEstimatedTime(calculatedTour.getEstimatedTime());

                    // Kopiere die routeImagePath, wenn sie generiert wurde
                    if (calculatedTour.getRouteImagePath() != null && !calculatedTour.getRouteImagePath().isEmpty()) {
                        tour.setRouteImagePath(calculatedTour.getRouteImagePath());
                    }
                });

                // Erfolgsmeldung anzeigen
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Route Calculation");
                    alert.setHeaderText("Route Calculated Successfully");
                    alert.setContentText(String.format(
                            "Distance: %.2f km\nEstimated Time: %d minutes",
                            calculatedTour.getDistance(),
                            calculatedTour.getEstimatedTime()
                    ));
                    alert.showAndWait();
                });
            } else {
                // Fehlermeldung anzeigen
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Route Calculation Failed");
                    alert.setHeaderText("Could not calculate route");
                    alert.setContentText("Please check your internet connection and the validity of the locations.");
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