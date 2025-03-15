package org.example.tourplanner.ui.views;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.models.TourLog;
import org.example.tourplanner.ui.viewmodels.MainViewModel;
import org.example.tourplanner.ui.viewmodels.TourLogViewModel;
import org.example.tourplanner.ui.viewmodels.TourViewModel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainViewController {
    private static final Logger logger = LogManager.getLogger(MainViewController.class);
    private final MainViewModel viewModel = new MainViewModel();

    @FXML
    private TextField searchField;

    @FXML
    private ListView<TourViewModel> tourListView;

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
    private TableView<TourLogViewModel> tourLogTableView;

    @FXML
    private TableColumn<TourLogViewModel, LocalDateTime> dateColumn;

    @FXML
    private TableColumn<TourLogViewModel, Number> timeColumn;

    @FXML
    private TableColumn<TourLogViewModel, Number> distanceColumn;

    @FXML
    private TableColumn<TourLogViewModel, Number> difficultyColumn;

    @FXML
    private TableColumn<TourLogViewModel, Number> ratingColumn;

    @FXML
    private TableColumn<TourLogViewModel, String> commentColumn;

    @FXML
    private void initialize() {
        logger.info("Initializing main view controller");

        // Bind search field to view model
        searchField.textProperty().bindBidirectional(viewModel.searchTermProperty());

        // Setup tour list view
        tourListView.setItems(viewModel.getFilteredTours());
        tourListView.setCellFactory(createTourListCellFactory());

        // Bind selected tour
        tourListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> viewModel.selectedTourProperty().set(newValue)
        );

        // Listen for selection changes and update detail view
        viewModel.selectedTourProperty().addListener(
                (observable, oldValue, newValue) -> updateTourDetails(newValue)
        );

        // Setup tour log table view
        setupTourLogTableView();

        // Listen for selected tour changes to update tour logs
        viewModel.selectedTourProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        tourLogTableView.setItems(newValue.getTourLogs());
                    } else {
                        tourLogTableView.getItems().clear();
                    }
                }
        );

        // Initialize with first tour selected if available
        if (!viewModel.getFilteredTours().isEmpty()) {
            tourListView.getSelectionModel().select(0);
        }
    }

    private void setupTourLogTableView() {
        // Configure table columns
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        dateColumn.setCellFactory(column -> new TableCell<>() {
            private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            @Override
            protected void updateItem(LocalDateTime date, boolean empty) {
                super.updateItem(date, empty);

                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(formatter.format(date));
                }
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

                if (empty || distance == null) {
                    setText(null);
                } else {
                    setText(String.format("%.1f km", distance.doubleValue()));
                }
            }
        });

        difficultyColumn.setCellValueFactory(cellData -> cellData.getValue().difficultyProperty());
        ratingColumn.setCellValueFactory(cellData -> cellData.getValue().ratingProperty());
        commentColumn.setCellValueFactory(cellData -> cellData.getValue().commentProperty());

        // Set selection model
        tourLogTableView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> viewModel.selectedTourLogProperty().set(newValue)
        );
    }

    private Callback<ListView<TourViewModel>, ListCell<TourViewModel>> createTourListCellFactory() {
        return listView -> new ListCell<>() {
            @Override
            protected void updateItem(TourViewModel tour, boolean empty) {
                super.updateItem(tour, empty);

                if (empty || tour == null) {
                    setText(null);
                } else {
                    setText(tour.nameProperty().get());
                }
            }
        };
    }

    private void updateTourDetails(TourViewModel tour) {
        if (tour == null) {
            tourNameLabel.setText("[No tour selected]");
            tourFromLabel.setText("-");
            tourToLabel.setText("-");
            tourDistanceLabel.setText("-");
            tourTimeLabel.setText("-");
            tourTransportLabel.setText("-");
            tourDescriptionArea.setText("");
            return;
        }

        tourNameLabel.setText(tour.nameProperty().get());
        tourFromLabel.setText(tour.fromProperty().get());
        tourToLabel.setText(tour.toProperty().get());
        tourDistanceLabel.setText(String.format("%.1f km", tour.distanceProperty().get()));

        int minutes = tour.estimatedTimeProperty().get();
        tourTimeLabel.setText(String.format("%d:%02d hours", minutes / 60, minutes % 60));

        tourTransportLabel.setText(tour.transportTypeProperty().get());
        tourDescriptionArea.setText(tour.descriptionProperty().get());

        logger.info("Updated tour details for: {}", tour.nameProperty().get());
    }

    @FXML
    private void onImportAction() {
        logger.info("Import action triggered");
        // Implementation will be added later
    }

    @FXML
    private void onExportAction() {
        logger.info("Export action triggered");
        // Implementation will be added later
    }

    @FXML
    private void onExitAction() {
        logger.info("Exit action triggered");
        System.exit(0);
    }

    @FXML
    private void onPreferencesAction() {
        logger.info("Preferences action triggered");
        // Implementation will be added later
    }

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
    private void onNewTourAction() {
        logger.info("New tour action triggered");
        // Implementation will be added later with dialog for creating new tour
    }

    @FXML
    private void onEditTourAction() {
        logger.info("Edit tour action triggered");
        TourViewModel selectedTour = viewModel.selectedTourProperty().get();
        if (selectedTour != null) {
            // Implementation will be added later with dialog for editing
            logger.info("Editing tour: {}", selectedTour.nameProperty().get());
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
            confirmDialog.setContentText("Are you sure you want to delete the tour \""
                    + selectedTour.nameProperty().get() + "\"?");

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
            // Implementation will be added later with dialog for creating new log
            logger.info("Creating new log for tour: {}", selectedTour.nameProperty().get());
        } else {
            showNoTourSelectedWarning();
        }
    }

    @FXML
    private void onSearchAction() {
        // This is automatically handled through binding to the searchTerm property
    }

    @FXML
    private void onGenerateReportAction() {
        logger.info("Generate report action triggered");
        TourViewModel selectedTour = viewModel.selectedTourProperty().get();
        if (selectedTour != null) {
            // Implementation will be added later
            logger.info("Generating report for tour: {}", selectedTour.nameProperty().get());
        } else {
            showNoTourSelectedWarning();
        }
    }

    @FXML
    private void onGenerateSummaryAction() {
        logger.info("Generate summary action triggered");
        // Implementation will be added later
    }

    private void showNoTourSelectedWarning() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("No Tour Selected");
        alert.setHeaderText("No Tour Selected");
        alert.setContentText("Please select a tour from the list.");
        alert.showAndWait();
    }
}