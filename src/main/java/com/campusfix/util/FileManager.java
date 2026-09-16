package com.campusfix.util;

import com.campusfix.model.Complaint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the file side of the app: CSV backups and generated text reports.
 * Everything goes through java.nio.file rather than the older java.io.File API,
 * per the project's NIO.2 requirement.
 */
public final class FileManager {

    private static final Path DATA_DIR = Paths.get("data");
    private static final Path BACKUP_FILE = DATA_DIR.resolve("complaints_backup.csv");
    private static final Path REPORTS_DIR = DATA_DIR.resolve("reports");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private FileManager() {
    }

    public static void ensureDataFolders() {
        try {
            Files.createDirectories(DATA_DIR);
            Files.createDirectories(REPORTS_DIR);
        } catch (IOException e) {
            System.err.println("Could not set up the data/ folder: " + e.getMessage());
        }
    }

    /** Dumps every complaint to a CSV backup file, overwriting whatever was there before. */
    public static void backupComplaints(List<Complaint> complaints) {
        ensureDataFolders();
        List<String> lines = new ArrayList<>();
        lines.add("complaint_id,title,category,priority,status,created_at");
        for (Complaint c : complaints) {
            lines.add(toCsvRow(c));
        }
        try {
            Files.write(BACKUP_FILE, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Backup failed: " + e.getMessage());
        }
    }

    private static String toCsvRow(Complaint c) {
        return String.join(",",
                c.getComplaintId(),
                escapeCsv(c.getTitle()),
                c.getCategory(),
                c.getPriority().toString(),
                c.getStatus().toString(),
                c.getCreatedAt().toString());
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /** Writes a text report to data/reports/ and returns the path it was written to. */
    public static Path writeReport(String fileNamePrefix, String content) {
        ensureDataFolders();
        String fileName = fileNamePrefix + "_" + java.time.LocalDateTime.now().format(TS) + ".txt";
        Path target = REPORTS_DIR.resolve(fileName);
        try {
            Files.writeString(target, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            System.err.println("Could not write report: " + e.getMessage());
        }
        return target;
    }
}
