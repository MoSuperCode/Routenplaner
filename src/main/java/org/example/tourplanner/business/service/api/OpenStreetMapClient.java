package org.example.tourplanner.business.service.api;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.config.ConfigurationManager;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OpenStreetMapClient {
    private static final Logger logger = LogManager.getLogger(OpenStreetMapClient.class);
    private final String tileServerUrl;
    private final String basePath;

    public OpenStreetMapClient() {
        ConfigurationManager configManager = ConfigurationManager.getInstance();
        this.tileServerUrl = configManager.getProperty("osm.tile.url");
        this.basePath = configManager.getProperty("file.basePath", "./resources/images");

        // Ensure the base directory exists
        try {
            Files.createDirectories(Paths.get(basePath));
        } catch (IOException e) {
            logger.error("Failed to create base directory", e);
        }

        logger.info("OpenStreetMapClient initialized with tile server: {}", tileServerUrl);
    }

    /**
     * Generates and saves a static map for a route between two points
     * @param startLon Starting point longitude
     * @param startLat Starting point latitude
     * @param endLon Ending point longitude
     * @param endLat Ending point latitude
     * @param zoom Zoom level (1-19, where 19 is most detailed)
     * @param fileName File name to save the map to
     * @return Path to the saved image file
     */
    public String generateStaticMap(double startLon, double startLat, double endLon, double endLat, int zoom, String fileName) {
        try {
            // Calculate the center point
            double centerLon = (startLon + endLon) / 2;
            double centerLat = (startLat + endLat) / 2;

            // Fetch the tile for the center point
            BufferedImage mapImage = fetchTile(centerLon, centerLat, zoom);
            if (mapImage == null) {
                logger.error("Failed to fetch map tile");
                return null;
            }

            // Draw the route on the map
            drawRoute(mapImage, startLon, startLat, endLon, endLat, centerLon, centerLat, zoom);

            // Save the map
            File outputFile = new File(basePath, fileName);
            ImageIO.write(mapImage, "png", outputFile);
            logger.info("Map saved to: {}", outputFile.getAbsolutePath());

            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            logger.error("Error generating static map", e);
            return null;
        }
    }

    /**
     * Fetches a tile from the tile server
     */
    private BufferedImage fetchTile(double lon, double lat, int zoom) throws IOException {
        // Convert lon/lat to tile coordinates
        int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
        int ytile = (int) Math.floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2 * (1 << zoom));

        String url = tileServerUrl.replace("{z}", String.valueOf(zoom))
                .replace("{x}", String.valueOf(xtile))
                .replace("{y}", String.valueOf(ytile));

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            // Add User-Agent to comply with OSM usage policy
            request.setHeader("User-Agent", "TourPlanner/1.0");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                if (response.getCode() == 200) {
                    try (InputStream is = response.getEntity().getContent()) {
                        return ImageIO.read(is);
                    }
                } else {
                    logger.error("Failed to fetch tile, status: {}", response.getCode());
                    return null;
                }
            }
        }
    }

    /**
     * Draws a route on the map image
     */
    private void drawRoute(BufferedImage mapImage, double startLon, double startLat, double endLon, double endLat,
                           double centerLon, double centerLat, int zoom) {
        // Convert coordinates to pixel positions on the map
        int[] startPixel = geoToPixel(startLon, startLat, centerLon, centerLat, zoom, mapImage.getWidth(), mapImage.getHeight());
        int[] endPixel = geoToPixel(endLon, endLat, centerLon, centerLat, zoom, mapImage.getWidth(), mapImage.getHeight());

        // Draw the route line
        Graphics2D g = mapImage.createGraphics();
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(3));
        g.drawLine(startPixel[0], startPixel[1], endPixel[0], endPixel[1]);

        // Draw start marker
        g.setColor(Color.GREEN);
        g.fillOval(startPixel[0] - 5, startPixel[1] - 5, 10, 10);

        // Draw end marker
        g.setColor(Color.RED);
        g.fillOval(endPixel[0] - 5, endPixel[1] - 5, 10, 10);

        g.dispose();
    }

    /**
     * Converts geo coordinates to pixel positions on the map
     */
    private int[] geoToPixel(double lon, double lat, double centerLon, double centerLat, int zoom, int mapWidth, int mapHeight) {
        // Calculate the pixel position relative to the top-left corner of the map
        double x = (lon - centerLon) * (256 * Math.pow(2, zoom)) / 360;
        double y = (Math.log(Math.tan(Math.PI / 4 + centerLat * Math.PI / 360)) -
                Math.log(Math.tan(Math.PI / 4 + lat * Math.PI / 360))) * (256 * Math.pow(2, zoom)) / (2 * Math.PI);

        // Adjust for map center
        x += mapWidth / 2.0;
        y += mapHeight / 2.0;

        return new int[] { (int) x, (int) y };
    }
}