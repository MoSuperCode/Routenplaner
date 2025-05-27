package org.example.tourplanner.business.service;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.config.HttpClientConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Service for handling import/export operations via HTTP backend
 * Must-Have: Import/Export functionality
 */
public class HttpImportExportService {
    private static final Logger logger = LogManager.getLogger(HttpImportExportService.class);
    private static HttpImportExportService instance;

    private HttpImportExportService() {}

    public static synchronized HttpImportExportService getInstance() {
        if (instance == null) {
            instance = new HttpImportExportService();
        }
        return instance;
    }

    /**
     * Export all tours to JSON file
     */
    public boolean exportToursToJson(String outputPath) {
        try (CloseableHttpClient httpClient = HttpClientConfig.createHttpClient()) {
            String url = HttpClientConfig.getBaseUrl() + "/import-export/export/tours";
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/json");

            logger.info("Exporting tours to: {}", outputPath);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    // Get the JSON content as bytes
                    byte[] jsonBytes = EntityUtils.toByteArray(response.getEntity());
                    logger.info("Received JSON export with {} bytes", jsonBytes.length);

                    // Ensure directory exists
                    Path outputFile = Paths.get(outputPath);
                    if (outputFile.getParent() != null) {
                        Files.createDirectories(outputFile.getParent());
                    }

                    // Write JSON to file
                    try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                        fos.write(jsonBytes);
                        fos.flush();
                    }

                    logger.info("Tours exported successfully to: {}", outputPath);
                    return true;
                } else {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    logger.error("Failed to export tours: HTTP {} - {}", response.getCode(), responseBody);
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Error exporting tours", e);
            return false;
        }
    }

    /**
     * Export specific tour to JSON file
     */
    public boolean exportTourToJson(Long tourId, String outputPath) {
        try (CloseableHttpClient httpClient = HttpClientConfig.createHttpClient()) {
            String url = HttpClientConfig.getBaseUrl() + "/import-export/export/tour/" + tourId;
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/json");

            logger.info("Exporting tour {} to: {}", tourId, outputPath);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    // Get the JSON content as bytes
                    byte[] jsonBytes = EntityUtils.toByteArray(response.getEntity());
                    logger.info("Received JSON export with {} bytes", jsonBytes.length);

                    // Ensure directory exists
                    Path outputFile = Paths.get(outputPath);
                    if (outputFile.getParent() != null) {
                        Files.createDirectories(outputFile.getParent());
                    }

                    // Write JSON to file
                    try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                        fos.write(jsonBytes);
                        fos.flush();
                    }

                    logger.info("Tour exported successfully to: {}", outputPath);
                    return true;
                } else if (response.getCode() == 404) {
                    logger.error("Tour with ID {} not found", tourId);
                    return false;
                } else {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    logger.error("Failed to export tour: HTTP {} - {}", response.getCode(), responseBody);
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Error exporting tour {}", tourId, e);
            return false;
        }
    }

    /**
     * Export tours to CSV file
     */
    public boolean exportToursToCsv(String outputPath) {
        try (CloseableHttpClient httpClient = HttpClientConfig.createHttpClient()) {
            String url = HttpClientConfig.getBaseUrl() + "/import-export/export/tours/csv";
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "text/csv");

            logger.info("Exporting tours to CSV: {}", outputPath);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    // Get the CSV content as bytes
                    byte[] csvBytes = EntityUtils.toByteArray(response.getEntity());
                    logger.info("Received CSV export with {} bytes", csvBytes.length);

                    // Ensure directory exists
                    Path outputFile = Paths.get(outputPath);
                    if (outputFile.getParent() != null) {
                        Files.createDirectories(outputFile.getParent());
                    }

                    // Write CSV to file
                    try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                        fos.write(csvBytes);
                        fos.flush();
                    }

                    logger.info("Tours exported to CSV successfully: {}", outputPath);
                    return true;
                } else {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    logger.error("Failed to export tours to CSV: HTTP {} - {}", response.getCode(), responseBody);
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("Error exporting tours to CSV", e);
            return false;
        }
    }

    /**
     * Import tours from JSON file
     */
    public ImportResult importToursFromJson(String filePath) {
        try (CloseableHttpClient httpClient = HttpClientConfig.createHttpClient()) {
            File file = new File(filePath);
            if (!file.exists()) {
                logger.error("Import file not found: {}", filePath);
                return new ImportResult(false, 0, "File not found: " + filePath);
            }

            String url = HttpClientConfig.getBaseUrl() + "/import-export/import/tours";
            HttpPost request = new HttpPost(url);

            // Create multipart entity with file
            HttpEntity entity = MultipartEntityBuilder.create()
                    .addBinaryBody("file", file, ContentType.APPLICATION_JSON, file.getName())
                    .build();

            request.setEntity(entity);

            logger.info("Importing tours from: {}", filePath);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());

                if (response.getCode() == 200) {
                    // Parse the success message to extract count
                    // Expected format: "Successfully imported X tours"
                    int importedCount = parseImportedCount(responseBody);
                    logger.info("Tours imported successfully: {}", responseBody);
                    return new ImportResult(true, importedCount, responseBody);
                } else {
                    logger.error("Failed to import tours: HTTP {} - {}", response.getCode(), responseBody);
                    return new ImportResult(false, 0, responseBody);
                }
            }
        } catch (Exception e) {
            logger.error("Error importing tours from {}", filePath, e);
            return new ImportResult(false, 0, "Error importing tours: " + e.getMessage());
        }
    }

    /**
     * Parse the imported count from success message
     */
    private int parseImportedCount(String message) {
        try {
            // Extract number from message like "Successfully imported 5 tours"
            String[] parts = message.split(" ");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("imported".equals(parts[i]) && i + 1 < parts.length) {
                    return Integer.parseInt(parts[i + 1]);
                }
            }
        } catch (Exception e) {
            logger.debug("Could not parse imported count from message: {}", message);
        }
        return 0;
    }

    /**
     * Get default export directory
     */
     static public String getExportDirectory() {
        String userHome = System.getProperty("user.home");
        String exportDir = userHome + "/TourPlanner/Exports";

        // Create directory if it doesn't exist
        try {
            Files.createDirectories(Paths.get(exportDir));
        } catch (IOException e) {
            logger.warn("Could not create export directory, using temp directory: {}", e.getMessage());
            return System.getProperty("java.io.tmpdir");
        }

        return exportDir;
    }

    /**
     * Result class for import operations
     */
    public static class ImportResult {
        private final boolean success;
        private final int importedCount;
        private final String message;

        public ImportResult(boolean success, int importedCount, String message) {
            this.success = success;
            this.importedCount = importedCount;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public int getImportedCount() { return importedCount; }
        public String getMessage() { return message; }
    }
}