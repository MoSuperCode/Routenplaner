package org.example.tourplanner;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

public class TourPlannerApplication extends Application {
    private static final Logger logger = LogManager.getLogger(TourPlannerApplication.class);

    @Override
    public void start(Stage stage) throws IOException {
        logger.info("Starting Tour Planner Application");
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(TourPlannerApplication.class.getResource("ui/views/main-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 1024, 768);
            stage.setTitle("Tour Planner");
            stage.setScene(scene);
            stage.show();
            logger.info("Application UI initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize application", e);
            throw e;
        }
    }

    public static void main(String[] args) {
        launch();
    }
}