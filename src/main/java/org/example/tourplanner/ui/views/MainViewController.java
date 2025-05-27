package org.example.tourplanner.ui.views;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.concurrent.Worker;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.models.TourLog;
import org.example.tourplanner.ui.viewmodels.MainViewModel;
import org.example.tourplanner.ui.viewmodels.TourLogViewModel;
import org.example.tourplanner.ui.viewmodels.TourViewModel;
import org.example.tourplanner.models.Tour;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.util.Locale;

public class MainViewController {
    private static final Logger logger = LogManager.getLogger(MainViewController.class);
    private final MainViewModel viewModel = new MainViewModel();

    // Map variables
    private WebView mapWebView;
    private WebEngine mapWebEngine;
    private boolean mapLoaded = false;
    private boolean mapInitialized = false;

    // FXML Controls
    @FXML private TextField searchField;
    @FXML private ListView<TourViewModel> tourListView;
    @FXML private Label tourNameLabel;
    @FXML private Label tourFromLabel;
    @FXML private Label tourToLabel;
    @FXML private Label tourDistanceLabel;
    @FXML private Label tourTimeLabel;
    @FXML private Label tourTransportLabel;
    @FXML private TextArea tourDescriptionArea;
    @FXML private Pane mapPane;
    @FXML private TableView<TourLogViewModel> tourLogTableView;
    @FXML private TableColumn<TourLogViewModel, LocalDateTime> dateColumn;
    @FXML private TableColumn<TourLogViewModel, Number> timeColumn;
    @FXML private TableColumn<TourLogViewModel, Number> distanceColumn;
    @FXML private TableColumn<TourLogViewModel, Number> difficultyColumn;
    @FXML private TableColumn<TourLogViewModel, Number> ratingColumn;
    @FXML private TableColumn<TourLogViewModel, String> commentColumn;

    @FXML
    private void initialize() {
        logger.info("Initializing main view controller");

        // Bind search functionality
        searchField.textProperty().bindBidirectional(viewModel.searchTermProperty());

        // Setup tour list
        tourListView.setItems(viewModel.getFilteredTours());
        tourListView.setCellFactory(createTourListCellFactory());
        tourListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    viewModel.selectedTourProperty().set(newValue);
                    updateTourDetails(newValue);
                }
        );

        // Setup tour log table
        setupTourLogTableView();
        viewModel.selectedTourProperty().addListener((observable, oldValue, newValue) -> {
            tourLogTableView.setItems(newValue != null ? newValue.getTourLogs() : null);
        });

        // Initialize empty map message
        showEmptyMapMessage();

        // Select first tour if available
        if (!viewModel.getFilteredTours().isEmpty()) {
            tourListView.getSelectionModel().select(0);
        }
    }

    // ==================== MAP FUNCTIONALITY ====================

    private void showEmptyMapMessage() {
        mapPane.getChildren().clear();
        Label emptyLabel = new Label("Select a tour to view the route map");
        emptyLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 14px;");
        emptyLabel.layoutXProperty().bind(mapPane.widthProperty().subtract(emptyLabel.widthProperty()).divide(2));
        emptyLabel.layoutYProperty().bind(mapPane.heightProperty().subtract(emptyLabel.heightProperty()).divide(2));
        mapPane.getChildren().add(emptyLabel);
    }

    private void initializeMapWebView() {
        if (mapInitialized) return;

        logger.info("Initializing map WebView");

        mapWebView = new WebView();
        mapWebEngine = mapWebView.getEngine();

        // Add WebView to map pane
        mapPane.getChildren().clear();
        mapPane.getChildren().add(mapWebView);

        // Bind WebView size to pane
        mapWebView.prefWidthProperty().bind(mapPane.widthProperty());
        mapWebView.prefHeightProperty().bind(mapPane.heightProperty());

        // Setup load listener
        mapWebEngine.getLoadWorker().stateProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == Worker.State.SUCCEEDED) {
                mapLoaded = true;
                logger.info("Map loaded successfully");
            } else if (newValue == Worker.State.FAILED) {
                logger.error("Failed to load map: {}", mapWebEngine.getLoadWorker().getException());
            }
        });

        // Load map HTML
        loadMapHTML();
    }

    private void loadMapHTML() {
        try {
            URL mapUrl = getClass().getResource("/org/example/tourplanner/ui/views/map.html");
            if (mapUrl != null) {
                mapWebEngine.load(mapUrl.toExternalForm());
                mapInitialized = true;
                logger.info("Map HTML loaded from: {}", mapUrl.toExternalForm());
            } else {
                logger.error("map.html not found in resources");
                showMapError("Map file not found");
            }
        } catch (Exception e) {
            logger.error("Error loading map HTML", e);
            showMapError("Error loading map: " + e.getMessage());
        }
    }

    private void showMapError(String errorMessage) {
        mapPane.getChildren().clear();
        Label errorLabel = new Label(errorMessage);
        errorLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        errorLabel.layoutXProperty().bind(mapPane.widthProperty().subtract(errorLabel.widthProperty()).divide(2));
        errorLabel.layoutYProperty().bind(mapPane.heightProperty().subtract(errorLabel.heightProperty()).divide(2));
        mapPane.getChildren().add(errorLabel);
    }

    private void displayRouteOnMap(TourViewModel tour) {
        if (!mapLoaded) {
            logger.warn("Map not loaded yet, cannot display route");
            return;
        }

        double[] fromCoords = getCoordinatesForLocation(tour.fromProperty().get());
        double[] toCoords = getCoordinatesForLocation(tour.toProperty().get());

        try {
            String script = String.format(Locale.US,
                    "showSimpleRoute(%f, %f, %f, %f, '%s', '%s');",
                    fromCoords[0], fromCoords[1], toCoords[0], toCoords[1],
                    escapeJavaScript(tour.fromProperty().get()),
                    escapeJavaScript(tour.toProperty().get())
            );

            logger.info("Displaying route: {} -> {}", tour.fromProperty().get(), tour.toProperty().get());
            mapWebEngine.executeScript(script);

        } catch (Exception e) {
            logger.error("Error displaying route on map", e);
        }
    }

    private String escapeJavaScript(String input) {
        return input != null ? input.replace("'", "\\'").replace("\"", "\\\"") : "";
    }

    private double[] getCoordinatesForLocation(String location) {
        if (location == null) return new double[]{47.6965, 13.3457};

        return switch (location.toLowerCase().trim()) {
            case "vienna", "wien" -> new double[]{48.2082, 16.3738};
            case "salzburg" -> new double[]{47.8095, 13.0550};
            case "graz" -> new double[]{47.0707, 15.4395};
            case "linz" -> new double[]{48.3069, 14.2858};
            case "innsbruck" -> new double[]{47.2692, 11.4041};
            case "klagenfurt" -> new double[]{46.6250, 14.3050};
            case "bregenz" -> new double[]{47.5031, 9.7471};
            case "st. pölten", "st poelten" -> new double[]{48.2058, 15.6232};
            case "eisenstadt" -> new double[]{47.8450, 16.5200};
            case "villach" -> new double[]{46.6111, 13.8558};
            case "wels" -> new double[]{48.1597, 14.0264};
            case "dornbirn" -> new double[]{47.4124, 9.7436};
            case "berlin" -> new double[]{52.5200, 13.4050};
            case "munich", "münchen" -> new double[]{48.1351, 11.5820};
            case "prague", "prag" -> new double[]{50.0755, 14.4378};
            case "budapest" -> new double[]{47.4979, 19.0402};
            case "zurich", "zürich" -> new double[]{47.3769, 8.5417};
            case "amsterdam" -> new double[]{52.3676, 4.9041};
            case "netherlands", "holland" -> new double[]{52.1326, 5.2913};
            case "london" -> new double[]{51.5074, -0.1278};
            case "paris" -> new double[]{48.8566, 2.3522};
            case "brussels", "brüssel" -> new double[]{50.8503, 4.3517};
            case "madrid" -> new double[]{40.4168, -3.7038};
            case "barcelona" -> new double[]{41.3851, 2.1734};
            case "lisbon", "lissabon" -> new double[]{38.7223, -9.1393};
            case "rome", "rom" -> new double[]{41.9028, 12.4964};
            case "milan", "mailand" -> new double[]{45.4642, 9.1900};
            case "florence", "florenz" -> new double[]{43.7696, 11.2558};
            case "venice", "venedig" -> new double[]{45.4408, 12.3155};
            case "naples", "neapel" -> new double[]{40.8518, 14.2681};
            default -> new double[]{47.6965, 13.3457}; // Austria center
        };
    }

    // ==================== UI UPDATE METHODS ====================

    private void updateTourDetails(TourViewModel tour) {
        if (tour == null) {
            clearTourDetails();
            showEmptyMapMessage();
            return;
        }

        // Update tour detail labels
        tourNameLabel.setText(tour.nameProperty().get());
        tourFromLabel.setText(tour.fromProperty().get());
        tourToLabel.setText(tour.toProperty().get());
        tourDistanceLabel.setText(String.format("%.1f km", tour.distanceProperty().get()));

        int minutes = tour.estimatedTimeProperty().get();
        tourTimeLabel.setText(String.format("%d:%02d hours", minutes / 60, minutes % 60));

        tourTransportLabel.setText(tour.transportTypeProperty().get());
        tourDescriptionArea.setText(tour.descriptionProperty().get());

        // Initialize and display map
        initializeMapWebView();
        if (mapLoaded) {
            displayRouteOnMap(tour);
        } else {
            // Wait for map to load, then display route
            mapWebEngine.getLoadWorker().stateProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue == Worker.State.SUCCEEDED && mapLoaded) {
                    displayRouteOnMap(tour);
                }
            });
        }

        logger.info("Updated tour details for: {}", tour.nameProperty().get());
    }

    private void clearTourDetails() {
        tourNameLabel.setText("[No tour selected]");
        tourFromLabel.setText("-");
        tourToLabel.setText("-");
        tourDistanceLabel.setText("-");
        tourTimeLabel.setText("-");
        tourTransportLabel.setText("-");
        tourDescriptionArea.setText("");
    }

    // ==================== TABLE SETUP ====================

    private void setupTourLogTableView() {
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        dateColumn.setCellFactory(column -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);
                setText(empty || date == null ? null : formatter.format(date));
            }
        });

        timeColumn.setCellValueFactory(cellData -> cellData.getValue().totalTimeProperty());
        timeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number time, boolean empty) {
                super.updateItem(time, empty);
                if (empty || time == null) {
                    setText(null);
                } else {
                    int minutes = time.intValue();
                    setText(String.format("%d:%02d", minutes / 60, minutes % 60));
                }
            }
        });

        distanceColumn.setCellValueFactory(cellData -> cellData.getValue().totalDistanceProperty());
        distanceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number distance, boolean empty) {
                super.updateItem(distance, empty);
                setText(empty || distance == null ? null : String.format("%.1f km", distance.doubleValue()));
            }
        });

        difficultyColumn.setCellValueFactory(cellData -> cellData.getValue().difficultyProperty());
        ratingColumn.setCellValueFactory(cellData -> cellData.getValue().ratingProperty());
        commentColumn.setCellValueFactory(cellData -> cellData.getValue().commentProperty());

        tourLogTableView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> viewModel.selectedTourLogProperty().set(newValue)
        );
    }

    private Callback<ListView<TourViewModel>, ListCell<TourViewModel>> createTourListCellFactory() {
        return listView -> new ListCell<>() {
            @Override
            protected void updateItem(TourViewModel tour, boolean empty) {
                super.updateItem(tour, empty);
                setText(empty || tour == null ? null : tour.nameProperty().get());
            }
        };
    }

    // ==================== DIALOG HELPERS ====================

    private boolean showTourDialog(Tour tour, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/example/tourplanner/ui/views/tour-dialog.fxml"));
            Parent dialogContent = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(tourListView.getScene().getWindow());
            dialogStage.setScene(new Scene(dialogContent));

            AddTourDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setTour(tour);

            dialogStage.showAndWait();
            return controller.isSaveClicked();
        } catch (IOException e) {
            logger.error("Error showing tour dialog", e);
            showErrorDialog("Error", "Could not load the tour dialog: " + e.getMessage());
            return false;
        }
    }

    private boolean showTourLogDialog(TourLog tourLog, String title) {
        try {
            URL resource = getClass().getResource("/org/example/tourplanner/ui/views/tour-log-dialog.fxml");
            if (resource == null) {
                throw new IOException("Cannot find tour-log-dialog.fxml");
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent dialogContent = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(title);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(tourListView.getScene().getWindow());
            dialogStage.setScene(new Scene(dialogContent));

            AddTourLogDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setTourLog(tourLog);

            dialogStage.showAndWait();
            return controller.isSaveClicked();
        } catch (IOException e) {
            logger.error("Error showing tour log dialog: {}", e.getMessage(), e);
            showErrorDialog("Error", "Could not load the tour log dialog: " + e.getMessage());
            return false;
        }
    }

    private void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showNoTourSelectedWarning() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("No Tour Selected");
        alert.setHeaderText("No Tour Selected");
        alert.setContentText("Please select a tour from the list.");
        alert.showAndWait();
    }

    private void showNoLogSelectedWarning() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("No Tour Log Selected");
        alert.setHeaderText("No Tour Log Selected");
        alert.setContentText("Please select a tour log from the table.");
        alert.showAndWait();
    }

    // ==================== ACTION HANDLERS ====================

    @FXML private void onSearchAction() {
        // Automatically handled through binding
    }

    @FXML
    private void onNewTourAction() {
        logger.info("New tour action triggered");
        try {
            Tour newTour = new Tour();
            if (showTourDialog(newTour, "New Tour")) {
                viewModel.addTour(newTour);
                logger.info("New tour created: {}", newTour.getName());
            }
        } catch (Exception e) {
            logger.error("Error creating new tour", e);
            showErrorDialog("Error creating new tour", e.getMessage());
        }
    }

    @FXML
    private void onEditTourAction() {
        logger.info("Edit tour action triggered");
        TourViewModel selectedTour = viewModel.selectedTourProperty().get();

        if (selectedTour != null) {
            try {
                Tour tour = selectedTour.getTour();
                if (showTourDialog(tour, "Edit Tour")) {
                    selectedTour.updateFromModel();
                    viewModel.updateTour(selectedTour);
                    updateTourDetails(selectedTour);
                    logger.info("Tour updated: {}", tour.getName());
                }
            } catch (Exception e) {
                logger.error("Error editing tour", e);
                showErrorDialog("Error editing tour", e.getMessage());
            }
        } else {
            showNoTourSelectedWarning();
        }
    }

    @FXML
    private void onDeleteTourAction() {
        logger.info("Delete tour action triggered");
        TourViewModel selectedTour = viewModel.selectedTourProperty().get();
        if (selectedTour != null) {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Delete Tour");
            confirmDialog.setHeaderText("Delete Tour");
            confirmDialog.setContentText("Are you sure you want to delete the tour \"" + selectedTour.nameProperty().get() + "\"?");

            confirmDialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    viewModel.deleteTour(selectedTour);
                }
            });
        } else {
            showNoTourSelectedWarning();
        }
    }

    @FXML
    private void onNewLogAction() {
        logger.info("New log action triggered");
        TourViewModel selectedTour = viewModel.selectedTourProperty().get();

        if (selectedTour != null) {
            try {
                TourLog newTourLog = new TourLog();
                newTourLog.setDate(LocalDateTime.now());

                if (showTourLogDialog(newTourLog, "New Tour Log")) {
                    viewModel.addTourLog(newTourLog);
                    logger.info("New tour log created for tour: {}", selectedTour.nameProperty().get());
                }
            } catch (Exception e) {
                logger.error("Error creating new tour log", e);
                showErrorDialog("Error creating new tour log", e.getMessage());
            }
        } else {
            showNoTourSelectedWarning();
        }
    }

    @FXML
    private void onEditTourLogAction() {
        logger.info("Edit tour log action triggered");
        TourLogViewModel selectedLog = viewModel.selectedTourLogProperty().get();

        if (selectedLog != null) {
            try {
                TourLog tourLog = selectedLog.getTourLog();
                if (showTourLogDialog(tourLog, "Edit Tour Log")) {
                    selectedLog.refreshFromModel();
                    viewModel.updateTourLog(selectedLog);
                    tourLogTableView.refresh();
                    logger.info("Tour log updated");
                }
            } catch (Exception e) {
                logger.error("Error editing tour log", e);
                showErrorDialog("Error editing tour log", e.getMessage());
            }
        } else {
            showNoLogSelectedWarning();
        }
    }

    @FXML
    private void onDeleteTourLogAction() {
        logger.info("Delete tour log action triggered");
        TourLogViewModel selectedLog = viewModel.selectedTourLogProperty().get();

        if (selectedLog != null) {
            Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
            confirmDialog.setTitle("Delete Tour Log");
            confirmDialog.setHeaderText("Delete Tour Log");
            confirmDialog.setContentText("Are you sure you want to delete this tour log?");

            confirmDialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    viewModel.deleteTourLog(selectedLog);
                    logger.info("Tour log deleted");
                }
            });
        } else {
            showNoLogSelectedWarning();
        }
    }

    @FXML
    private void onGenerateReportAction() {
        logger.info("Generate report action triggered");
        TourViewModel selectedTour = viewModel.selectedTourProperty().get();
        if (selectedTour != null) {
            logger.info("Generating report for tour: {}", selectedTour.nameProperty().get());
            // TODO: Implement report generation
        } else {
            showNoTourSelectedWarning();
        }
    }

    @FXML
    private void onGenerateSummaryAction() {
        logger.info("Generate summary action triggered");
        // TODO: Implement summary generation
    }

    // Menu actions
    @FXML private void onImportAction() { logger.info("Import action triggered"); }
    @FXML private void onExportAction() { logger.info("Export action triggered"); }
    @FXML private void onExitAction() { logger.info("Exit action triggered"); System.exit(0); }
    @FXML private void onPreferencesAction() { logger.info("Preferences action triggered"); }

    @FXML
    private void onAboutAction() {
        logger.info("About action triggered");
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Tour Planner");
        alert.setHeaderText("Tour Planner Application");
        alert.setContentText("Version 1.0\nA JavaFX MVVM application for planning tours");
        alert.showAndWait();
    }

    // Debug method for testing map
    @FXML
    private void onTestMapAction() {
        logger.info("Testing map functionality...");
        if (!mapLoaded) {
            logger.warn("Map not loaded yet");
            return;
        }

        try {
            String script = "showSimpleRoute(48.2082, 16.3738, 47.8095, 13.0550, 'Wien', 'Salzburg');";
            mapWebEngine.executeScript(script);
            logger.info("Test route displayed");
        } catch (Exception e) {
            logger.error("Error testing map", e);
        }
    }


@FXML
private void onDebugMapAction() {
    if (mapWebEngine != null) {
        String currentLocation = mapWebEngine.getLocation();
        logger.info("Current WebEngine location: {}", currentLocation);

        // JavaScript Console prüfen
        try {
            Object result = mapWebEngine.executeScript("document.title");
            logger.info("Document title: {}", result);

            Object mapExists = mapWebEngine.executeScript("typeof map !== 'undefined'");
            logger.info("Map object exists: {}", mapExists);

        } catch (Exception e) {
            logger.error("Error checking map state", e);
        }
    } else {
        logger.warn("MapWebEngine is null");
    }
}
}