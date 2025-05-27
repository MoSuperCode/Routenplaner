package org.example.tourplanner.business.service;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.config.HttpClientConfig;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HttpReportService {
    private static final Logger logger = LogManager.getLogger(HttpReportService.class);
    private static HttpReportService instance;

    private HttpReportService() {}

    public static synchronized HttpReportService getInstance() {
        if (instance == null) {
            instance = new HttpReportService();
        }
        return instance;
    }

    /**
     * Generates a tour report and saves it to the specified path
     */
    public boolean generateTourReport(Long tourId, String outputPath) {
        // Erstelle einen neuen HttpClient für diesen Request
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = HttpClientConfig.getBaseUrl() + "/reports/tour/" + tourId;
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/pdf");

            logger.info("Requesting tour report from: {}", url);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                logger.info("Response status: {}", response.getCode());

                if (response.getCode() == 200) {
                    // Get the PDF content as bytes
                    byte[] pdfBytes = EntityUtils.toByteArray(response.getEntity());
                    logger.info("Received PDF with {} bytes", pdfBytes.length);

                    // Ensure directory exists
                    Path outputFile = Paths.get(outputPath);
                    if (outputFile.getParent() != null) {
                        Files.createDirectories(outputFile.getParent());
                    }

                    // Write PDF to file
                    try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                        fos.write(pdfBytes);
                        fos.flush();
                    }

                    logger.info("Tour report saved to: {}", outputPath);
                    return true;
                } else if (response.getCode() == 404) {
                    logger.error("Tour with ID {} not found", tourId);
                    return false;
                } else {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    logger.error("Failed to generate tour report: HTTP {} - {}", response.getCode(), responseBody);
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Error generating tour report for tourId: " + tourId, e);
            return false;
        }
    }

    /**
     * Generates a summary report for all tours and saves it to the specified path
     */
    public boolean generateSummaryReport(String outputPath) {
        // Erstelle einen neuen HttpClient für diesen Request
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String url = HttpClientConfig.getBaseUrl() + "/reports/summary";
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/pdf");

            logger.info("Requesting summary report from: {}", url);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                logger.info("Response status: {}", response.getCode());

                if (response.getCode() == 200) {
                    // Get the PDF content as bytes
                    byte[] pdfBytes = EntityUtils.toByteArray(response.getEntity());
                    logger.info("Received PDF with {} bytes", pdfBytes.length);

                    // Ensure directory exists
                    Path outputFile = Paths.get(outputPath);
                    if (outputFile.getParent() != null) {
                        Files.createDirectories(outputFile.getParent());
                    }

                    // Write PDF to file
                    try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                        fos.write(pdfBytes);
                        fos.flush();
                    }

                    logger.info("Summary report saved to: {}", outputPath);
                    return true;
                } else {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    logger.error("Failed to generate summary report: HTTP {} - {}", response.getCode(), responseBody);
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Error generating summary report", e);
            return false;
        }
    }

    /**
     * Gets a temporary file path for reports
     */
    public String getReportPath(String reportName) {
        String userHome = System.getProperty("user.home");
        String reportsDir = userHome + "/TourPlanner/Reports";

        // Create reports directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(reportsDir));
        } catch (IOException e) {
            logger.warn("Could not create reports directory, using temp directory: {}", e.getMessage());
            return System.getProperty("java.io.tmpdir") + "/" + reportName;
        }

        return reportsDir + "/" + reportName;
    }
}