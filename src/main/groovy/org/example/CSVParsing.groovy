package org.example

class CSVParsing extends BaseMethods {

    List<Map> readCSV(String csvFile) {
        validateCSVFilePath(csvFile)
        InputStream inputStream = locateCSVFile(csvFile)

        if (inputStream == null) {
            throw new FileNotFoundException("File not found: " + csvFile)
        }
        List<String> lines = readCSVLines(inputStream)
        validateCSVContent(lines)

        return parseCSVLinesToJson(lines)
    }


    private void validateCSVFilePath(String csvFile) {
        if (!csvFile || csvFile.trim().isEmpty()) {
            throw new IllegalArgumentException("CSV file path is missing or empty.")
        }
        println(" CSV file path validated: ${csvFile}")
    }


    private InputStream locateCSVFile(String csvFile) {
        InputStream inputStream = CSVParsing.class.classLoader.getResourceAsStream(csvFile)
        if (inputStream == null) {
            println("CSV file not found: ${csvFile}" )
        }
        return inputStream
    }


    private List<String> readCSVLines(InputStream inputStream) {
        List<String> lines = inputStream.readLines()
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty.")
        }
        return lines
    }


    private void validateCSVContent(List<String> lines) {
        if (lines.size() < 2) {
            throw new IllegalArgumentException("CSV file must contain at least one data row.")
        }
    }


    private List<Map> parseCSVLinesToJson(List<String> lines) {
        List<Map> data = []
        def headers = lines[0].split(",")


        lines[1..-1].each { line ->
            Map rowMap = [:]
            def values = line.split(",")
            if (values.size() != headers.size()) {
                println("Skipping row with mismatched columns: ${line}")
                return
            }
            headers.eachWithIndex { header, index ->
                rowMap[header] = index < values.size() ? values[index]?.trim() : null
            }
            if (isValidRow(rowMap)) {
                data << rowMap
            } else {
                println("Skipping invalid row: ${rowMap}")
            }
        }
        return data
    }

    private boolean isValidRow(Map row) {
        // Validation logic
        if (isValidProject(row)) return true
        if (isValidTask(row)) return true
        if (isValidAssignment(row)) return true
        if (isValidResource(row)) return true
        return false
    }

    private boolean isValidProject(Map row) {
        def projectId = row.id
        def projectName = row.name
        def isActive = row.is_active
        def startDate = row.start
        def finishDate = row.finish


        if (!validateId(projectId) || !validateName(projectName) || !validateActive(isActive) || !validateDates(startDate, finishDate)) {
            return false
        }
        return true
    }


    private boolean isValidTask(Map row) {
        def taskId = row.id
        def taskName = row.name
        def taskStatus = row.status


        if (!validateId(taskId) || !validateName(taskName)) {
            return false
        }
        if (!taskStatus || !(taskStatus in ["In Progress", "Not Started", "Completed"])) {
            return false
        }
        return true
    }


    private boolean isValidAssignment(Map row) {
        def assignmentId = row.id
        def resourceId = row.resource_id
        def actuals = row.actuals
        def etc = row.etc


        if (!validateId(assignmentId)
                || !validateId(resourceId) ||
                !validateDecimal(actuals, "actuals") || !validateDecimal(etc, "etc")
        ) {
            return false
        }
        return true
    }


    private boolean isValidResource(Map row) {
        def resourceId = row.id
        def resourceName = row.name
        def isActive = row.is_active

        if (!validateId(resourceId) || !validateName(resourceName) || !validateActive(isActive)) {
            return false
        }
        return true
    }


    List<Map> readProjectsCSV() {
        List<Map> projects = readCSV("projects.csv")
        return projects
    }


    List<Map> readTasksCSV() {
        def projects = readProjectsCSV()
        def validProjectIds = projects*.id
        // Read tasks and log them for verification
        List<Map> tasks = readCSV("tasks.csv")
        // Ensure the filtering works correctly
        def filteredTasks = tasks.findAll { it.project_id in validProjectIds }
        return filteredTasks
    }


    List<Map> readAssignmentsCSV() {
        def tasks = readTasksCSV()
        def validTaskIds = tasks*.id

        // Read assignments and log them for verification
        List<Map> assignments = readCSV("assignments.csv")

        //Ensure the filtering works correctly
        def filteredAssignments = assignments.findAll { it.task_id in validTaskIds }

        if (filteredAssignments.isEmpty()) {
        }
        return filteredAssignments
    }


    List<Map> readResourcesCSV() {
        return readCSV("resources.csv")
    }
}




