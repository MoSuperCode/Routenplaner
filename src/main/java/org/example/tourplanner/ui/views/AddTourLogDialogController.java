package org.example.tourplanner.ui.views;

import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
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

    // Properties for data binding
    private final ObjectProperty<LocalDate> dateProperty = new SimpleObjectProperty<>();
    private final IntegerProperty hoursProperty = new SimpleIntegerProperty(0);
    private final IntegerProperty minutesProperty = new SimpleIntegerProperty(0);
    private final DoubleProperty totalDistanceProperty = new SimpleDoubleProperty(0.0);
    private final IntegerProperty difficultyProperty = new SimpleIntegerProperty(5);
    private final IntegerProperty ratingProperty = new SimpleIntegerProperty(3);
    private final StringProperty commentProperty = new SimpleStringProperty("");

    private TourLog tourLog;
    private boolean saveClicked = false;
    private Stage dialogStage;

    @FXML
    private void initialize() {
        // Bind properties to UI elements
        datePicker.valueProperty().bindBidirectional(dateProperty);
        difficultySlider.valueProperty().bindBidirectional(difficultyProperty);
        commentArea.textProperty().bindBidirectional(commentProperty);

        // For numeric text fields, we need to handle conversion
        StringConverter<Number> intConverter = new StringConverter<>() {
            @Override
            public String toString(Number object) {
                return object == null ? "0" : object.toString();
            }

            @Override
            public Number fromString(String string) {
                if (string == null || string.isEmpty()) return 0;
                try {
                    return Integer.parseInt(string);
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        };

        StringConverter<Number> doubleConverter = new StringConverter<>() {
            @Override
            public String toString(Number object) {
                return object == null ? "0.0" : object.toString();
            }

            @Override
            public Number fromString(String string) {
                if (string == null || string.isEmpty()) return 0.0;
                try {
                    return Double.parseDouble(string);
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            }
        };

        // Bind text fields using the converters
        bindTextFieldToNumber(hoursField, hoursProperty, intConverter);
        bindTextFieldToNumber(minutesField, minutesProperty, intConverter);
        bindTextFieldToNumber(distanceField, totalDistanceProperty, doubleConverter);

        // Set up input validation for numeric fields
        addNumericValidation(hoursField, "\\d*");
        addNumericValidation(minutesField, "\\d*");
        addNumericValidation(distanceField, "\\d*(\\.\\d*)?");

        // Add extra validation for minutes (0-59)
        minutesField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) {
                try {
                    int mins = Integer.parseInt(newVal);
                    if (mins > 59) {
                        minutesField.setText("59");
                    }
                } catch (NumberFormatException e) {
                    // Already handled by regex validation
                }
            }
        });

        // Listen for rating toggle changes
        ratingGroup.selectedToggleProperty().addListener(this::onRatingChanged);
    }

    /**
     * Binds a text field to a numeric property using a converter
     */
    private <T extends Number> void bindTextFieldToNumber(TextField textField, Property<T> property, StringConverter<Number> converter) {
        // Update text field when property changes
        property.addListener((obs, oldVal, newVal) ->
                textField.setText(converter.toString(newVal)));

        // Update property when text field changes
        textField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.isEmpty()) {
                T value = (T) converter.fromString(newVal);
                property.setValue(value);
            }
        });

        // Set initial value
        textField.setText(converter.toString(property.getValue()));
    }

    /**
     * Adds validation to ensure text field only accepts input matching the pattern
     */
    private void addNumericValidation(TextField textField, String pattern) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches(pattern)) {
                textField.setText(oldValue);
            }
        });
    }

    /**
     * Updates the rating property when toggle selection changes
     */
    private void onRatingChanged(ObservableValue<? extends Toggle> obs, Toggle oldToggle, Toggle newToggle) {
        if (newToggle != null) {
            ratingProperty.set(Integer.parseInt(newToggle.getUserData().toString()));
        }
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setTourLog(TourLog tourLog) {
        this.tourLog = tourLog;

        // Fill properties with tour log data if editing an existing log
        if (tourLog.getId() != null) {
            // Set date and time properties
            if (tourLog.getDate() != null) {
                dateProperty.set(tourLog.getDate().toLocalDate());
                hoursProperty.set(tourLog.getDate().getHour());
                minutesProperty.set(tourLog.getDate().getMinute());
            } else {
                dateProperty.set(LocalDate.now());
                hoursProperty.set(0);
                minutesProperty.set(0);
            }

            // Set other properties
            totalDistanceProperty.set(tourLog.getTotalDistance());
            difficultyProperty.set(tourLog.getDifficulty());
            commentProperty.set(tourLog.getComment());
            ratingProperty.set(tourLog.getRating());

            // Select the appropriate rating toggle
            for (Toggle toggle : ratingGroup.getToggles()) {
                if (Integer.parseInt(toggle.getUserData().toString()) == tourLog.getRating()) {
                    toggle.setSelected(true);
                    break;
                }
            }
        } else {
            // Default values for new log
            dateProperty.set(LocalDate.now());
            hoursProperty.set(0);
            minutesProperty.set(0);
            totalDistanceProperty.set(0.0);
            difficultyProperty.set(5);
            ratingProperty.set(3);
            commentProperty.set("");

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
            // Create LocalDateTime from date and time properties
            LocalDateTime dateTime = LocalDateTime.of(
                    dateProperty.get(),
                    LocalTime.of(hoursProperty.get(), minutesProperty.get())
            );
            tourLog.setDate(dateTime);

            // Get total time in minutes
            int totalMinutes = hoursProperty.get() * 60 + minutesProperty.get();
            tourLog.setTotalTime(totalMinutes);

            // Set other fields from properties
            tourLog.setTotalDistance(totalDistanceProperty.get());
            tourLog.setDifficulty(difficultyProperty.get());
            tourLog.setRating(ratingProperty.get());
            tourLog.setComment(commentProperty.get());

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

        if (dateProperty.get() == null) {
            errorMessage += "No valid date provided!\n";
        }

        if (hoursProperty.get() == 0 && minutesProperty.get() == 0) {
            errorMessage += "Please provide a time (hours/minutes)!\n";
        }

        if (totalDistanceProperty.get() <= 0) {
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