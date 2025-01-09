package org.example

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class CSVParsing {

    private static final Logger logger = LogManager.getLogger(CSVParsing)

    List<Map> readCSV(String csvFile) {
        validateCSVFilePath(csvFile)
        InputStream inputStream = locateCSVFile(csvFile)

        if (inputStream == null) {
            logger.error("File not found: {}", csvFile)
            return [] // Return an empty list if file is not found
        }

        List<String> lines = readCSVLines(inputStream)
        validateCSVContent(lines)

        return parseCSVLinesToJson(lines)
    }

    // Validate the CSV file path
    private void validateCSVFilePath(String csvFile) {
        if (!csvFile || csvFile.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV file path is missing or empty.")
        }
        logger.debug("CSV file path validated: {}", csvFile)
    }

    // Locate the CSV file in the resources folder
    private InputStream locateCSVFile(String csvFile) {
        logger.debug("Locating CSV file: {}", csvFile)
        InputStream inputStream = CSVParsing.class.classLoader.getResourceAsStream(csvFile)
        if (inputStream != null) {
            logger.debug("CSV file found: {}", csvFile)
            return inputStream
        }
        logger.warn("CSV file not found in resources: {}", csvFile)
        return null
    }

    // Read lines from the CSV file
    private List<String> readCSVLines(InputStream inputStream) {
        logger.info("Reading lines from input stream.")
        List<String> lines = inputStream.readLines() // Converting the InputStream to a List of lines
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty.")
        }
        logger.debug("Read {} lines from input stream.", lines.size())
        return lines
    }

    // Validate the CSV content (for now, just a simple check)
    private void validateCSVContent(List<String> lines) {
        if (lines.size() < 2) {
            throw new IllegalArgumentException("CSV file must contain at least one data row.")
        }
    }

    // Parse the CSV lines into a list of maps and convert to JSON
    private List<Map> parseCSVLinesToJson(List<String> lines) {
        List<Map> data = []
        def headers = lines[0].split(",") // Ignoring headers
        lines[1..-1].each { line ->  // Iterate over each line after the header
            Map rowMap = [:]
            def values = line.split(",")
            headers.eachWithIndex { header, index ->
                rowMap[header] = values[index]
            }
            // Add the rowMap to the data list
            data << rowMap
        }
        return data
    }


    // Method to read all 4 CSV files: projects, tasks, assignments, and resources
    List<Map> readProjectsCSV() {
        return readCSV("projects.csv")
    }

    List<Map> readTasksCSV() {
        return readCSV("tasks.csv")
    }

    List<Map> readAssignmentsCSV() {
        return readCSV("assignments.csv")
    }

    List<Map> readResourcesCSV() {
        return readCSV("resources.csv")
    }
}


