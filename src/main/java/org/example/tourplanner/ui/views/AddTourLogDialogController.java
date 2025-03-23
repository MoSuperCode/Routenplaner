package org.example.tourplanner.ui.views;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.models.TourLog;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AddTourLogDialogController {
    private static final Logger logger = LogManager.getLogger(AddTourLogDialogController.class);

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField hoursField;

    @FXML
    private TextField minutesField;

    @FXML
    private TextField distanceField;

    @FXML
    private Slider difficultySlider;

    @FXML
    private ToggleGroup ratingGroup;

    @FXML
    private TextArea commentArea;

    private TourLog tourLog;
    private boolean saveClicked = false;
    private Stage dialogStage;

    @FXML
    private void initialize() {
        // Input validation for numeric fields
        hoursField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                hoursField.setText(oldValue);
            }
        });

        minutesField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                minutesField.setText(oldValue);
            } else if (!newValue.isEmpty()) {
                int mins = Integer.parseInt(newValue);
                if (mins > 59) {
                    minutesField.setText("59");
                }
            }
        });

        distanceField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*(\\.\\d*)?")) {
                distanceField.setText(oldValue);
            }
        });
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setTourLog(TourLog tourLog) {
        this.tourLog = tourLog;

        // Fill fields with tour log data if editing an existing log
        if (tourLog.getId() != null) {
            // Set date
            if (tourLog.getDate() != null) {
                datePicker.setValue(tourLog.getDate().toLocalDate());
                hoursField.setText(String.valueOf(tourLog.getDate().getHour()));
                minutesField.setText(String.valueOf(tourLog.getDate().getMinute()));
            } else {
                datePicker.setValue(LocalDate.now());
                hoursField.setText("0");
                minutesField.setText("0");
            }

            // Set other fields
            distanceField.setText(String.valueOf(tourLog.getTotalDistance()));
            difficultySlider.setValue(tourLog.getDifficulty());
            commentArea.setText(tourLog.getComment());

            // Set rating
            for (Toggle toggle : ratingGroup.getToggles()) {
                if (Integer.parseInt(toggle.getUserData().toString()) == tourLog.getRating()) {
                    toggle.setSelected(true);
                    break;
                }
            }
        } else {
            // Default values for new log
            datePicker.setValue(LocalDate.now());
            hoursField.setText("0");
            minutesField.setText("0");
            distanceField.setText("0.0");
            difficultySlider.setValue(5);

            // Default rating to 3
            for (Toggle toggle : ratingGroup.getToggles()) {
                if (Integer.parseInt(toggle.getUserData().toString()) == 3) {
                    toggle.setSelected(true);
                    break;
                }
            }
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    public TourLog getTourLog() {
        return tourLog;
    }

    @FXML
    private void onSave() {
        if (isInputValid()) {
            // Update tour log with form values
            LocalDateTime dateTime = LocalDateTime.of(
                    datePicker.getValue(),
                    LocalTime.of(
                            Integer.parseInt(hoursField.getText().isEmpty() ? "0" : hoursField.getText()),
                            Integer.parseInt(minutesField.getText().isEmpty() ? "0" : minutesField.getText())
                    )
            );
            tourLog.setDate(dateTime);

            // Get total time in minutes
            int totalMinutes = Integer.parseInt(hoursField.getText().isEmpty() ? "0" : hoursField.getText()) * 60 +
                    Integer.parseInt(minutesField.getText().isEmpty() ? "0" : minutesField.getText());
            tourLog.setTotalTime(totalMinutes);

            tourLog.setTotalDistance(Double.parseDouble(distanceField.getText()));
            tourLog.setDifficulty((int) difficultySlider.getValue());

            // Get selected rating
            int rating = 3; // Default
            if (ratingGroup.getSelectedToggle() != null) {
                rating = Integer.parseInt(ratingGroup.getSelectedToggle().getUserData().toString());
            }
            tourLog.setRating(rating);

            tourLog.setComment(commentArea.getText());

            saveClicked = true;
            dialogStage.close();
            logger.info("Tour log saved");
        }
    }

    @FXML
    private void onCancel() {
        dialogStage.close();
        logger.info("Tour log edit canceled");
    }

    private boolean isInputValid() {
        String errorMessage = "";

        if (datePicker.getValue() == null) {
            errorMessage += "No valid date provided!\n";
        }

        if (hoursField.getText().isEmpty() && minutesField.getText().isEmpty()) {
            errorMessage += "Please provide a time (hours/minutes)!\n";
        }

        if (distanceField.getText() == null || distanceField.getText().isEmpty()) {
            errorMessage += "No valid distance provided!\n";
        }

        if (ratingGroup.getSelectedToggle() == null) {
            errorMessage += "No rating selected!\n";
        }

        if (errorMessage.isEmpty()) {
            return true;
        } else {
            // Show the error message
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Invalid Fields");
            alert.setHeaderText("Please correct invalid fields");
            alert.setContentText(errorMessage);
            alert.showAndWait();
            return false;
        }
    }
}