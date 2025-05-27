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
import org.example.tourplanner.business.service.HttpImportExportService;
import org.example.tourplanner.business.service.HttpReportService;
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
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.stage.FileChooser;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;


public class MainViewController {
    private static final Logger logger = LogManager.getLogger(MainViewController.class);
    private final MainViewModel viewModel = new MainViewModel();
    // Report Service hinzufügen
    private final HttpReportService reportService = HttpReportService.getInstance();
    // Import/Export Service hinzugefügt
    private final HttpImportExportService importExportService = HttpImportExportService.getInstance();



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


    // Menu actions
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





    @FXML
    private void onGenerateReportAction() {
        logger.info("Generate report action triggered");
        TourViewModel selectedTour = viewModel.selectedTourProperty().get();

        if (selectedTour == null) {
            showNoTourSelectedWarning();
            return;
        }

        // File chooser for save location
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Tour Report");
        fileChooser.setInitialFileName("tour-report-" + selectedTour.nameProperty().get().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        // Set initial directory to user's home/TourPlanner/Reports
        String defaultPath = reportService.getReportPath("");
        File defaultDir = new File(defaultPath).getParentFile();
        if (defaultDir.exists()) {
            fileChooser.setInitialDirectory(defaultDir);
        }

        File file = fileChooser.showSaveDialog(tourListView.getScene().getWindow());
        if (file != null) {
            generateTourReportAsync(selectedTour.getTour().getId(), file.getAbsolutePath());
        }
    }
    @FXML
    private void onGenerateSummaryAction() {
        logger.info("Generate summary action triggered");

        if (viewModel.getTours().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Tours Available");
            alert.setHeaderText("No Tours to Summarize");
            alert.setContentText("Please create some tours first before generating a summary report.");
            alert.showAndWait();
            return;
        }

        // File chooser for save location
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Summary Report");
        fileChooser.setInitialFileName("tour-summary-report-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );

        // Set initial directory
        String defaultPath = reportService.getReportPath("");
        File defaultDir = new File(defaultPath).getParentFile();
        if (defaultDir.exists()) {
            fileChooser.setInitialDirectory(defaultDir);
        }

        File file = fileChooser.showSaveDialog(tourListView.getScene().getWindow());
        if (file != null) {
            generateSummaryReportAsync(file.getAbsolutePath());
        }
    }

    /**
     * Generate tour report asynchronously to avoid blocking the UI
     */
    private void generateTourReportAsync(Long tourId, String outputPath) {
        // Show progress cursor
        tourListView.getScene().setCursor(Cursor.WAIT);

        Task<Boolean> reportTask = new Task<>() {
            @Override
            protected Boolean call() {
                return reportService.generateTourReport(tourId, outputPath);
            }
        };

        reportTask.setOnSucceeded(event -> {
            // Reset cursor
            tourListView.getScene().setCursor(Cursor.DEFAULT);

            boolean success = reportTask.getValue();
            if (success) {
                // Show success dialog with option to open file
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Report Generated");
                alert.setHeaderText("Tour Report Generated Successfully");
                alert.setContentText("The report has been saved to:\n" + outputPath +
                        "\n\nWould you like to open the file?");

                ButtonType openButton = new ButtonType("Open Report");
                ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(openButton, closeButton);

                alert.showAndWait().ifPresent(response -> {
                    if (response == openButton) {
                        openReportFile(outputPath);
                    }
                });
            } else {
                showErrorDialog("Report Generation Failed",
                        "Could not generate the tour report. Please check the logs for details.");
            }
        });

        reportTask.setOnFailed(event -> {
            // Reset cursor
            tourListView.getScene().setCursor(Cursor.DEFAULT);

            logger.error("Report generation failed", reportTask.getException());
            showErrorDialog("Report Generation Failed",
                    "An error occurred while generating the report: " + reportTask.getException().getMessage());
        });

        // Run in background thread
        Thread reportThread = new Thread(reportTask);
        reportThread.setDaemon(true);
        reportThread.start();
    }

    /**
     * Generate summary report asynchronously
     */
    private void generateSummaryReportAsync(String outputPath) {
        // Show progress cursor
        tourListView.getScene().setCursor(Cursor.WAIT);

        Task<Boolean> reportTask = new Task<>() {
            @Override
            protected Boolean call() {
                return reportService.generateSummaryReport(outputPath);
            }
        };

        reportTask.setOnSucceeded(event -> {
            // Reset cursor
            tourListView.getScene().setCursor(Cursor.DEFAULT);

            boolean success = reportTask.getValue();
            if (success) {
                // Show success dialog with option to open file
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Summary Report Generated");
                alert.setHeaderText("Summary Report Generated Successfully");
                alert.setContentText("The summary report has been saved to:\n" + outputPath +
                        "\n\nWould you like to open the file?");

                ButtonType openButton = new ButtonType("Open Report");
                ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(openButton, closeButton);

                alert.showAndWait().ifPresent(response -> {
                    if (response == openButton) {
                        openReportFile(outputPath);
                    }
                });
            } else {
                showErrorDialog("Report Generation Failed",
                        "Could not generate the summary report. Please check the logs for details.");
            }
        });

        reportTask.setOnFailed(event -> {
            // Reset cursor
            tourListView.getScene().setCursor(Cursor.DEFAULT);

            logger.error("Summary report generation failed", reportTask.getException());
            showErrorDialog("Report Generation Failed",
                    "An error occurred while generating the report: " + reportTask.getException().getMessage());
        });

        // Run in background thread
        Thread reportThread = new Thread(reportTask);
        reportThread.setDaemon(true);
        reportThread.start();
    }

    /**
     * Opens the generated report file with the default system application
     */
    private void openReportFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                // Use Desktop API to open with default application
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(file);
                } else {
                    // Fallback for systems without Desktop support
                    String os = System.getProperty("os.name").toLowerCase();
                    if (os.contains("win")) {
                        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + filePath);
                    } else if (os.contains("mac")) {
                        Runtime.getRuntime().exec("open " + filePath);
                    } else if (os.contains("nix") || os.contains("nux")) {
                        Runtime.getRuntime().exec("xdg-open " + filePath);
                    }
                }
            } else {
                showErrorDialog("File Not Found", "The report file could not be found at: " + filePath);
            }
        } catch (Exception e) {
            logger.error("Error opening report file", e);
            showErrorDialog("Cannot Open File",
                    "Could not open the report file. Please navigate to the file manually:\n" + filePath);
        }
    }

    @FXML
    private void onImportAction() {
        logger.info("Import action triggered");

        // File chooser for import file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Tours from JSON");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("JSON Files", "*.json"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        // Set initial directory
        String defaultPath = importExportService.getExportDirectory();
        File defaultDir = new File(defaultPath);
        if (defaultDir.exists()) {
            fileChooser.setInitialDirectory(defaultDir);
        }

        File file = fileChooser.showOpenDialog(tourListView.getScene().getWindow());
        if (file != null) {
            importToursAsync(file.getAbsolutePath());
        }
    }



    /**
     * Import tours asynchronously
     */
    private void importToursAsync(String filePath) {
        // Show progress cursor
        tourListView.getScene().setCursor(Cursor.WAIT);

        Task<HttpImportExportService.ImportResult> importTask = new Task<>() {
            @Override
            protected HttpImportExportService.ImportResult call() {
                return importExportService.importToursFromJson(filePath);
            }
        };

        importTask.setOnSucceeded(event -> {
            // Reset cursor
            tourListView.getScene().setCursor(Cursor.DEFAULT);

            HttpImportExportService.ImportResult result = importTask.getValue();
            if (result.isSuccess()) {
                // Refresh tours list
                viewModel.refreshTours();

                // Show success dialog
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Import Successful");
                alert.setHeaderText("Tours Imported Successfully");
                alert.setContentText(String.format(
                        "Successfully imported %d tours from:\n%s",
                        result.getImportedCount(),
                        filePath
                ));
                alert.showAndWait();

                logger.info("Import completed: {} tours imported", result.getImportedCount());
            } else {
                showErrorDialog("Import Failed", result.getMessage());
            }
        });

        importTask.setOnFailed(event -> {
            // Reset cursor
            tourListView.getScene().setCursor(Cursor.DEFAULT);

            logger.error("Import failed", importTask.getException());
            showErrorDialog("Import Failed",
                    "An error occurred while importing tours: " + importTask.getException().getMessage());
        });

        // Run in background thread
        Thread importThread = new Thread(importTask);
        importThread.setDaemon(true);
        importThread.start();
    }

    /**
     * Export all tours to JSON
     */
    private void exportAllToursToJson() {
        if (viewModel.getTours().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Tours to Export");
            alert.setHeaderText("No Tours Available");
            alert.setContentText("Please create some tours first before exporting.");
            alert.showAndWait();
            return;
        }

        // File chooser for save location
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export All Tours to JSON");
        fileChooser.setInitialFileName("tours-export-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")) + ".json");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        // Set initial directory
        String defaultPath = importExportService.getExportDirectory();
        File defaultDir = new File(defaultPath);
        if (defaultDir.exists()) {
            fileChooser.setInitialDirectory(defaultDir);
        }

        File file = fileChooser.showSaveDialog(tourListView.getScene().getWindow());
        if (file != null) {
            exportToursAsync(file.getAbsolutePath(), "JSON", () ->
                    importExportService.exportToursToJson(file.getAbsolutePath()));
        }
    }

    /**
     * Export all tours to CSV
     */
    private void exportAllToursToCsv() {
        if (viewModel.getTours().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Tours to Export");
            alert.setHeaderText("No Tours Available");
            alert.setContentText("Please create some tours first before exporting.");
            alert.showAndWait();
            return;
        }

        // File chooser for save location
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export All Tours to CSV");
        fileChooser.setInitialFileName("tours-export-" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm")) + ".csv");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        // Set initial directory
        String defaultPath = importExportService.getExportDirectory();
        File defaultDir = new File(defaultPath);
        if (defaultDir.exists()) {
            fileChooser.setInitialDirectory(defaultDir);
        }

        File file = fileChooser.showSaveDialog(tourListView.getScene().getWindow());
        if (file != null) {
            exportToursAsync(file.getAbsolutePath(), "CSV", () ->
                    importExportService.exportToursToCsv(file.getAbsolutePath()));
        }
    }

    /**
     * Export selected tour to JSON
     */
    private void exportSelectedTourToJson() {
        TourViewModel selectedTour = viewModel.selectedTourProperty().get();
        if (selectedTour == null) {
            showNoTourSelectedWarning();
            return;
        }

        // File chooser for save location
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Selected Tour to JSON");
        fileChooser.setInitialFileName("tour-" +
                selectedTour.nameProperty().get().replaceAll("[^a-zA-Z0-9]", "_") +
                "-export.json");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        // Set initial directory
        String defaultPath = importExportService.getExportDirectory();
        File defaultDir = new File(defaultPath);
        if (defaultDir.exists()) {
            fileChooser.setInitialDirectory(defaultDir);
        }

        File file = fileChooser.showSaveDialog(tourListView.getScene().getWindow());
        if (file != null) {
            Long tourId = selectedTour.getTour().getId();
            exportToursAsync(file.getAbsolutePath(), "JSON", () ->
                    importExportService.exportTourToJson(tourId, file.getAbsolutePath()));
        }
    }

    /**
     * Generic async export method
     */
    private void exportToursAsync(String outputPath, String format, java.util.concurrent.Callable<Boolean> exportOperation) {
        // Show progress cursor
        tourListView.getScene().setCursor(Cursor.WAIT);

        Task<Boolean> exportTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return exportOperation.call();
            }
        };

        exportTask.setOnSucceeded(event -> {
            // Reset cursor
            tourListView.getScene().setCursor(Cursor.DEFAULT);

            boolean success = exportTask.getValue();
            if (success) {
                // Show success dialog with option to open file location
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Successful");
                alert.setHeaderText("Tours Exported Successfully");
                alert.setContentText("The tours have been exported to " + format + " format:\n" + outputPath +
                        "\n\nWould you like to open the file location?");

                ButtonType openFolderButton = new ButtonType("Open Folder");
                ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(openFolderButton, closeButton);

                alert.showAndWait().ifPresent(response -> {
                    if (response == openFolderButton) {
                        openFileLocation(outputPath);
                    }
                });

                logger.info("Export completed successfully to: {}", outputPath);
            } else {
                showErrorDialog("Export Failed", "Could not export tours to " + format + ". Please check the logs for details.");
            }
        });

        exportTask.setOnFailed(event -> {
            // Reset cursor
            tourListView.getScene().setCursor(Cursor.DEFAULT);

            logger.error("Export failed", exportTask.getException());
            showErrorDialog("Export Failed",
                    "An error occurred while exporting tours: " + exportTask.getException().getMessage());
        });

        // Run in background thread
        Thread exportThread = new Thread(exportTask);
        exportThread.setDaemon(true);
        exportThread.start();
    }

    /**
     * Opens the file location in the system file manager
     */
    private void openFileLocation(String filePath) {
        try {
            File file = new File(filePath);
            File directory = file.getParentFile();

            if (directory != null && directory.exists()) {
                // Use Desktop API to open folder
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(directory);
                } else {
                    // Fallback for systems without Desktop support
                    String os = System.getProperty("os.name").toLowerCase();
                    if (os.contains("win")) {
                        Runtime.getRuntime().exec("explorer.exe /select," + filePath);
                    } else if (os.contains("mac")) {
                        Runtime.getRuntime().exec("open -R " + filePath);
                    } else if (os.contains("nix") || os.contains("nux")) {
                        Runtime.getRuntime().exec("xdg-open " + directory.getAbsolutePath());
                    }
                }
            } else {
                showErrorDialog("Folder Not Found", "The export folder could not be found at: " + directory);
            }
        } catch (Exception e) {
            logger.error("Error opening file location", e);
            showErrorDialog("Cannot Open Folder",
                    "Could not open the export folder. Please navigate to the file manually:\n" + filePath);
        }
    }
    // Add these separate action methods to your MainViewController.java class:

    /**
     * Action method for exporting all tours to JSON from menu
     */
    @FXML
    private void onExportAllToursJsonAction() {
        logger.info("Export all tours to JSON action triggered");
        exportAllToursToJson();
    }

    /**
     * Action method for exporting all tours to CSV from menu
     */
    @FXML
    private void onExportAllToursCsvAction() {
        logger.info("Export all tours to CSV action triggered");
        exportAllToursToCsv();
    }

    /**
     * Action method for exporting selected tour to JSON from menu
     */
    @FXML
    private void onExportSelectedTourJsonAction() {
        logger.info("Export selected tour to JSON action triggered");
        exportSelectedTourToJson();
    }

// Also update the existing onExportAction method to show a selection dialog:

    @FXML
    private void onExportAction() {
        logger.info("Export action triggered - showing selection dialog");

        // Create a choice dialog for export options
        ChoiceDialog<String> dialog = new ChoiceDialog<>("All Tours (JSON)",
                "All Tours (JSON)", "All Tours (CSV)", "Selected Tour (JSON)");
        dialog.setTitle("Export Tours");
        dialog.setHeaderText("Choose Export Option");
        dialog.setContentText("What would you like to export?");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(choice -> {
            switch (choice) {
                case "All Tours (JSON)" -> exportAllToursToJson();
                case "All Tours (CSV)" -> exportAllToursToCsv();
                case "Selected Tour (JSON)" -> exportSelectedTourToJson();
            }
        });
    }

    /**
     * Enhanced method to show export progress with better user feedback
     */
    private void exportToursAsyncWithProgress(String outputPath, String format,
                                              String description,
                                              java.util.concurrent.Callable<Boolean> exportOperation) {

        // Create progress dialog
        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.setTitle("Exporting Tours");
        progressAlert.setHeaderText("Exporting " + description);
        progressAlert.setContentText("Please wait while the export is being processed...");

        // Remove OK button to prevent user from closing
        progressAlert.getButtonTypes().clear();
        progressAlert.show();

        // Show progress cursor
        tourListView.getScene().setCursor(Cursor.WAIT);

        Task<Boolean> exportTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return exportOperation.call();
            }
        };

        exportTask.setOnSucceeded(event -> {
            // Close progress dialog
            progressAlert.close();

            // Reset cursor
            tourListView.getScene().setCursor(Cursor.DEFAULT);

            boolean success = exportTask.getValue();
            if (success) {
                // Show success dialog with file info
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Export Successful");
                successAlert.setHeaderText("Tours Exported Successfully");

                // Get file size for user info
                String fileSizeInfo = getFileSizeInfo(outputPath);
                successAlert.setContentText(String.format(
                        "Successfully exported %s to %s format:\n%s\n\n%s\n\nWould you like to open the file location?",
                        description.toLowerCase(), format, outputPath, fileSizeInfo
                ));

                ButtonType openFolderButton = new ButtonType("Open Folder");
                ButtonType openFileButton = new ButtonType("Open File");
                ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
                successAlert.getButtonTypes().setAll(openFolderButton, openFileButton, closeButton);

                successAlert.showAndWait().ifPresent(response -> {
                    if (response == openFolderButton) {
                        openFileLocation(outputPath);
                    } else if (response == openFileButton) {
                        openFile(outputPath);
                    }
                });

                logger.info("Export completed successfully: {} to {}", description, outputPath);
            } else {
                showErrorDialog("Export Failed",
                        "Could not export " + description.toLowerCase() + " to " + format +
                                ". Please check the logs for details.");
            }
        });

        exportTask.setOnFailed(event -> {
            // Close progress dialog
            progressAlert.close();

            // Reset cursor
            tourListView.getScene().setCursor(Cursor.DEFAULT);

            Throwable exception = exportTask.getException();
            logger.error("Export failed for {}", description, exception);

            showErrorDialog("Export Failed",
                    "An error occurred while exporting " + description.toLowerCase() + ":\n" +
                            (exception != null ? exception.getMessage() : "Unknown error"));
        });

        // Run in background thread
        Thread exportThread = new Thread(exportTask);
        exportThread.setDaemon(true);
        exportThread.start();
    }

    /**
     * Get file size information for user feedback
     */
    private String getFileSizeInfo(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                long fileSize = file.length();
                String sizeText;
                if (fileSize < 1024) {
                    sizeText = fileSize + " bytes";
                } else if (fileSize < 1024 * 1024) {
                    sizeText = String.format("%.1f KB", fileSize / 1024.0);
                } else {
                    sizeText = String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
                }
                return "File size: " + sizeText;
            }
        } catch (Exception e) {
            logger.debug("Could not get file size for {}", filePath);
        }
        return "";
    }

    /**
     * Open file with default system application
     */
    private void openFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(file);
                } else {
                    // Fallback for systems without Desktop support
                    String os = System.getProperty("os.name").toLowerCase();
                    if (os.contains("win")) {
                        Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + filePath);
                    } else if (os.contains("mac")) {
                        Runtime.getRuntime().exec("open " + filePath);
                    } else if (os.contains("nix") || os.contains("nux")) {
                        Runtime.getRuntime().exec("xdg-open " + filePath);
                    }
                }
            } else {
                showErrorDialog("File Not Found", "The exported file could not be found at: " + filePath);
            }
        } catch (Exception e) {
            logger.error("Error opening file", e);
            showErrorDialog("Cannot Open File",
                    "Could not open the exported file. Please navigate to it manually:\n" + filePath);
        }
    }

    /**
     * Update the existing export methods to use the enhanced progress method
     */
    private void exportAllToursToJsonEnhanced() {
        if (viewModel.getTours().isEmpty()) {
            showNoToursWarning();
            return;
        }

        FileChooser fileChooser = createJsonFileChooser("Export All Tours to JSON",
                "tours-export-" + getCurrentTimestamp() + ".json");

        File file = fileChooser.showSaveDialog(tourListView.getScene().getWindow());
        if (file != null) {
            exportToursAsyncWithProgress(
                    file.getAbsolutePath(),
                    "JSON",
                    "All Tours (" + viewModel.getTours().size() + " tours)",
                    () -> importExportService.exportToursToJson(file.getAbsolutePath())
            );
        }
    }

    private void exportAllToursToCsvEnhanced() {
        if (viewModel.getTours().isEmpty()) {
            showNoToursWarning();
            return;
        }

        FileChooser fileChooser = createCsvFileChooser("Export All Tours to CSV",
                "tours-export-" + getCurrentTimestamp() + ".csv");

        File file = fileChooser.showSaveDialog(tourListView.getScene().getWindow());
        if (file != null) {
            exportToursAsyncWithProgress(
                    file.getAbsolutePath(),
                    "CSV",
                    "All Tours (" + viewModel.getTours().size() + " tours)",
                    () -> importExportService.exportToursToCsv(file.getAbsolutePath())
            );
        }
    }

    private void exportSelectedTourToJsonEnhanced() {
        TourViewModel selectedTour = viewModel.selectedTourProperty().get();
        if (selectedTour == null) {
            showNoTourSelectedWarning();
            return;
        }

        FileChooser fileChooser = createJsonFileChooser("Export Selected Tour to JSON",
                "tour-" + sanitizeFileName(selectedTour.nameProperty().get()) + "-export.json");

        File file = fileChooser.showSaveDialog(tourListView.getScene().getWindow());
        if (file != null) {
            Long tourId = selectedTour.getTour().getId();
            exportToursAsyncWithProgress(
                    file.getAbsolutePath(),
                    "JSON",
                    "Tour: " + selectedTour.nameProperty().get(),
                    () -> importExportService.exportTourToJson(tourId, file.getAbsolutePath())
            );
        }
    }

// Helper methods

    private FileChooser createJsonFileChooser(String title, String initialFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialFileName(initialFileName);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("JSON Files", "*.json")
        );

        String defaultPath = importExportService.getExportDirectory();
        File defaultDir = new File(defaultPath);
        if (defaultDir.exists()) {
            fileChooser.setInitialDirectory(defaultDir);
        }

        return fileChooser;
    }

    private FileChooser createCsvFileChooser(String title, String initialFileName) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialFileName(initialFileName);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        String defaultPath = importExportService.getExportDirectory();
        File defaultDir = new File(defaultPath);
        if (defaultDir.exists()) {
            fileChooser.setInitialDirectory(defaultDir);
        }

        return fileChooser;
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm"));
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9]", "_");
    }

    private void showNoToursWarning() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("No Tours to Export");
        alert.setHeaderText("No Tours Available");
        alert.setContentText("Please create some tours first before exporting.");
        alert.showAndWait();
    }

}