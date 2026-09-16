package com.campusfix.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Small audit logger - appends one line per action to data/audit.log.
 * synchronized so two threads submitting complaints at the same time don't interleave their lines.
 */
public final class LoggerUtil {

    private static final Path LOG_FILE = Paths.get("data", "audit.log");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private LoggerUtil() {
    }

    public static synchronized void log(String actor, String action, String description) {
        String line = String.format("[%s] %-20s actor=%-15s %s",
                LocalDateTime.now().format(TS), action, actor, description);
        try {
            Files.createDirectories(LOG_FILE.getParent());
            Files.writeString(LOG_FILE, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Audit log write failed: " + e.getMessage());
        }
        System.out.println(">> " + line);
    }

    public static synchronized void error(String actor, String context, Exception e) {
        log(actor, "ERROR", context + " :: " + e.getClass().getSimpleName() + " - " + e.getMessage());
    }
}
