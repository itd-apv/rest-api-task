package org.example

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class DataInsertion {
    private static final Logger logger = LogManager.getLogger(DataInsertion)

    static void main(String[] args) {

        CSVParsing service = new CSVParsing()

        try {
            // Read CSVs and get data as list of maps
            List<Map> projectsData = service.readProjectsCSV() // Assuming this method exists to read project data
            println "Projects Data: ${projectsData}"

            List<Map> tasksData = service.readTasksCSV()
            println "Tasks Data: ${tasksData}"

            postProjectsAndTasksToClarity(projectsData, tasksData)

        } catch (Exception e) {
            logger.error("Error reading CSV files: {}", e.message)
            e.printStackTrace()
        }
    }

    // Method to post projects and their associated tasks to Clarity
    static void postProjectsAndTasksToClarity(List<Map> projectsData, List<Map> tasksData) {
        try {
            // Send projects data along with tasks to Clarity service
            ClarityService.postProjectsWithTasks(projectsData, tasksData)  // Passing both projects and tasks data
            logger.info("Posted projects and tasks to Clarity PPM successfully.")

        } catch (Exception e) {
            logger.error("Error posting projects and tasks to Clarity PPM: {}", e.message)
            e.printStackTrace()
        }
    }
}


