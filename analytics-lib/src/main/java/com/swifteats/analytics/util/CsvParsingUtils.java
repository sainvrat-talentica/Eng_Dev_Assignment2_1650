package com.swifteats.analytics.util;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public final class CsvParsingUtils {

    private static final DateTimeFormatter CSV_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private CsvParsingUtils() {
    }

    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static Long parseLong(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        return Long.parseLong(normalized);
    }

    public static Integer parseInteger(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        return Integer.parseInt(normalized);
    }

    public static BigDecimal parseDecimal(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        return new BigDecimal(normalized);
    }

    public static LocalDateTime parseDateTime(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(normalized, CSV_TIMESTAMP);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid timestamp: " + normalized, ex);
        }
    }

    public static boolean isFailedStatus(String status) {
        return status != null && "Failed".equalsIgnoreCase(status.trim());
    }

    public static boolean isDelayed(LocalDateTime actual, LocalDateTime promised) {
        return actual != null && promised != null && actual.isAfter(promised);
    }

    public static java.time.Instant toInstant(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.toInstant(ZoneOffset.UTC);
    }
}
