package org.example

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

abstract class BaseMethods {

    boolean validateId(String id) {
        if (!id || id.trim().isEmpty()) {
            return false
        }
        return true
    }

    boolean validateName(String name) {
        if (!name || name.trim().isEmpty()) {
            return false
        }
        return true
    }

    boolean validateActive(String isActive) {
        if (isActive == null) {
            return false
        }
        if (!(isActive.toLowerCase() in ["true", "false"])) {
            return false
        }
        return true
    }

    boolean validateDecimal(def value, String fieldName) {
        if (value == null) {
            return false
        }

        try {
            BigDecimal decimalValue = new BigDecimal(value.toString())
        } catch (NumberFormatException e) {
            return false
        }
        return true
    }

    boolean validateDates(String startDateStr, String finishDateStr) {
        if (!startDateStr || !finishDateStr) {
            return false
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss") // Date-time format

        try {
            // Parse the dates from the strings using LocalDateTime
            LocalDateTime startDate = LocalDateTime.parse(startDateStr, formatter)
            LocalDateTime finishDate = LocalDateTime.parse(finishDateStr, formatter)

            // Validate that start date is before finish date
            if (!startDate.isBefore(finishDate)) {
                return false
            }
        } catch (DateTimeParseException e) {
            return false
        }
        return true
    }
}
