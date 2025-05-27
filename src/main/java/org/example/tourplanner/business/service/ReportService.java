package org.example.tourplanner.business.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.tourplanner.models.Tour;
import org.example.tourplanner.models.TourLog;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportService {
    private static final Logger logger = LogManager.getLogger(ReportService.class);
    private static ReportService instance;

    private ReportService() {}

    public static synchronized ReportService getInstance() {
        if (instance == null) {
            instance = new ReportService();
        }
        return instance;
    }

    /**
     * Generates a detailed tour report with all tour logs
     */
    public boolean generateTourReport(Tour tour, String outputPath) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Paragraph title = new Paragraph("Tour Report: " + tour.getName(), titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // Tour Details
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
            document.add(new Paragraph("Tour Details", headerFont));
            document.add(new Paragraph("Name: " + tour.getName()));
            document.add(new Paragraph("Description: " + (tour.getDescription() != null ? tour.getDescription() : "N/A")));
            document.add(new Paragraph("From: " + tour.getFrom()));
            document.add(new Paragraph("To: " + tour.getTo()));
            document.add(new Paragraph("Transport Type: " + tour.getTransportType()));
            document.add(new Paragraph("Distance: " + String.format("%.2f km", tour.getDistance())));
            document.add(new Paragraph("Estimated Time: " + formatMinutes(tour.getEstimatedTime())));
            document.add(new Paragraph("Popularity: " + tour.getPopularity() + " logs"));
            document.add(new Paragraph("Child Friendliness: " + String.format("%.1f/10", tour.getChildFriendliness())));
            document.add(new Paragraph("\n"));

            // Tour Logs
            if (tour.getTourLogs() != null && !tour.getTourLogs().isEmpty()) {
                document.add(new Paragraph("Tour Logs", headerFont));

                PdfPTable table = new PdfPTable(6);
                table.setWidthPercentage(100);
                table.setSpacingBefore(10f);

                // Headers
                table.addCell("Date");
                table.addCell("Time");
                table.addCell("Distance");
                table.addCell("Difficulty");
                table.addCell("Rating");
                table.addCell("Comment");

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

                for (TourLog log : tour.getTourLogs()) {
                    table.addCell(log.getDate() != null ? log.getDate().format(formatter) : "N/A");
                    table.addCell(formatMinutes(log.getTotalTime()));
                    table.addCell(String.format("%.2f km", log.getTotalDistance()));
                    table.addCell(String.valueOf(log.getDifficulty()));
                    table.addCell(String.valueOf(log.getRating()));
                    table.addCell(log.getComment() != null ? log.getComment() : "");
                }

                document.add(table);
            } else {
                document.add(new Paragraph("No tour logs available."));
            }

            document.close();
            logger.info("Tour report generated successfully: {}", outputPath);
            return true;

        } catch (DocumentException | FileNotFoundException e) {
            logger.error("Error generating tour report", e);
            return false;
        }
    }

    /**
     * Generates a summary report for multiple tours
     */
    public boolean generateSummaryReport(List<Tour> tours, String outputPath) {
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(outputPath));
            document.open();

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.BLACK);
            Paragraph title = new Paragraph("Tours Summary Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // Summary Statistics
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, BaseColor.BLACK);
            document.add(new Paragraph("Summary Statistics", headerFont));
            document.add(new Paragraph("Total Tours: " + tours.size()));

            double totalDistance = tours.stream().mapToDouble(Tour::getDistance).sum();
            int totalTime = tours.stream().mapToInt(Tour::getEstimatedTime).sum();
            int totalLogs = tours.stream().mapToInt(tour -> tour.getTourLogs().size()).sum();

            document.add(new Paragraph("Total Distance: " + String.format("%.2f km", totalDistance)));
            document.add(new Paragraph("Total Estimated Time: " + formatMinutes(totalTime)));
            document.add(new Paragraph("Total Tour Logs: " + totalLogs));
            document.add(new Paragraph("\n"));

            // Tours Table
            document.add(new Paragraph("Tours Overview", headerFont));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);

            // Headers
            table.addCell("Name");
            table.addCell("From");
            table.addCell("To");
            table.addCell("Distance");
            table.addCell("Est. Time");
            table.addCell("Logs");
            table.addCell("Avg Rating");

            for (Tour tour : tours) {
                table.addCell(tour.getName());
                table.addCell(tour.getFrom());
                table.addCell(tour.getTo());
                table.addCell(String.format("%.2f km", tour.getDistance()));
                table.addCell(formatMinutes(tour.getEstimatedTime()));
                table.addCell(String.valueOf(tour.getTourLogs().size()));

                double avgRating = tour.getTourLogs().isEmpty() ? 0 :
                        tour.getTourLogs().stream().mapToInt(TourLog::getRating).average().orElse(0);
                table.addCell(String.format("%.1f", avgRating));
            }

            document.add(table);
            document.close();

            logger.info("Summary report generated successfully: {}", outputPath);
            return true;

        } catch (DocumentException | FileNotFoundException e) {
            logger.error("Error generating summary report", e);
            return false;
        }
    }

    private String formatMinutes(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        return String.format("%d:%02d", hours, mins);
    }
}