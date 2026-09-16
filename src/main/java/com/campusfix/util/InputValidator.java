package com.campusfix.util;

import com.campusfix.exception.InvalidComplaintException;

import java.util.Set;

public final class InputValidator {

    public static final Set<String> VALID_CATEGORIES = Set.of(
            "Electrical", "Wi-Fi/Network", "Classroom Equipment", "Hostel",
            "Cleanliness", "Water", "Furniture", "Library", "Security",
            "Transportation", "Other"
    );

    private InputValidator() {
    }

    public static void validateComplaintInput(String title, String description, String category)
            throws InvalidComplaintException {
        if (isBlank(title)) {
            throw new InvalidComplaintException("Complaint title cannot be empty.");
        }
        if (title.trim().length() > 150) {
            throw new InvalidComplaintException("Complaint title is too long (max 150 characters).");
        }
        if (isBlank(description)) {
            throw new InvalidComplaintException("Complaint description cannot be empty.");
        }
        if (category == null || !VALID_CATEGORIES.contains(category)) {
            throw new InvalidComplaintException("'" + category + "' is not a recognised category.");
        }
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return email != null && email.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    }

    public static int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }
}
