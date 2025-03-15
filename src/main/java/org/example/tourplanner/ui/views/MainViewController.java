package org.example.tourplanner.ui.views;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainViewController {
    private static final Logger logger = LogManager.getLogger(MainViewController.class);

    @FXML
    private TextField searchField;

    @FXML
    private ListView<String> tourListView; // Später gegen ein richtiges Tour-Objekt austauschen

    @FXML
    private Label tourNameLabel;

    @FXML
    private Label tourFromLabel;

    @FXML
    private Label tourToLabel;

    @FXML
    private Label tourDistanceLabel;

    @FXML
    private Label tourTimeLabel;

    @FXML
    private Label tourTransportLabel;

    @FXML
    private TextArea tourDescriptionArea;

    @FXML
    private Pane mapPane;

    @FXML
    private TableView<String> tourLogTableView; // Später gegen ein richtiges TourLog-Objekt austauschen

    @FXML
    private TableColumn<String, String> dateColumn;

    @FXML
    private TableColumn<String, String> timeColumn;

    @FXML
    private TableColumn<String, String> distanceColumn;

    @FXML
    private TableColumn<String, String> difficultyColumn;

    @FXML
    private TableColumn<String, String> ratingColumn;

    @FXML
    private TableColumn<String, String> commentColumn;

    @FXML
    private void initialize() {
        logger.info("Initializing main view controller");
        // Demo-Daten für die ListView
        tourListView.getItems().addAll(
                "Sample Tour 1",
                "Sample Tour 2",
                "Sample Tour 3"
        );

        // Event-Handler für die Auswahl eines Tour-Elements
        tourListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> showTourDetails(newValue)
        );
    }

    private void showTourDetails(String tourName) {
        if (tourName == null) {
            return;
        }

        logger.info("Selected tour: {}", tourName);
        tourNameLabel.setText(tourName);

        // Demo-Werte setzen
        tourFromLabel.setText("Vienna");
        tourToLabel.setText("Salzburg");
        tourDistanceLabel.setText("295 km");
        tourTimeLabel.setText("3 hours");
        tourTransportLabel.setText("Car");
        tourDescriptionArea.setText("This is a sample tour from Vienna to Salzburg.");
    }

    @FXML
    private void onImportAction() {
        logger.info("Import action triggered");
    }

    @FXML
    private void onExportAction() {
        logger.info("Export action triggered");
    }

    @FXML
    private void onExitAction() {
        logger.info("Exit action triggered");
        System.exit(0);
    }

    @FXML
    private void onPreferencesAction() {
        logger.info("Preferences action triggered");
    }

    @FXML
    private void onAboutAction() {
        logger.info("About action triggered");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Tour Planner");
        alert.setHeaderText("Tour Planner Application");
        alert.setContentText("Version 1.0\nCreated by Your Name");
        alert.showAndWait();
    }

    @FXML
    private void onNewTourAction() {
        logger.info("New tour action triggered");
    }

    @FXML
    private void onEditTourAction() {
        logger.info("Edit tour action triggered");
    }

    @FXML
    private void onDeleteTourAction() {
        logger.info("Delete tour action triggered");
    }

    @FXML
    private void onNewLogAction() {
        logger.info("New log action triggered");
    }

    @FXML
    private void onSearchAction() {
        String searchTerm = searchField.getText().toLowerCase();
        logger.info("Search triggered with term: {}", searchTerm);
    }

    @FXML
    private void onGenerateReportAction() {
        logger.info("Generate report action triggered");
    }

    @FXML
    private void onGenerateSummaryAction() {
        logger.info("Generate summary action triggered");
    }
}