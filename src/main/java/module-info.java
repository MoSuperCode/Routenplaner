module org.example.tourplanner {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.data.jpa;
    requires jakarta.persistence;
    requires com.fasterxml.jackson.databind;
    requires org.apache.httpcomponents.client5.httpclient5;

    opens org.example.tourplanner to javafx.fxml;
    opens org.example.tourplanner.ui.views to javafx.fxml;
    exports org.example.tourplanner;
    exports org.example.tourplanner.ui.views;
}