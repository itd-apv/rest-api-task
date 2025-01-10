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

            // Assuming you have resource data as well
            List<Map> resourcesData = service.readResourcesCSV() // This method should read the resources CSV
            println "Resources Data: ${resourcesData}"

            List<Map> projectsFromClarity = ClarityService.getProjects()
            println "Projects Data from Clarity: ${projectsFromClarity}"

            List<Map> tasksFromClarity = []
            projectsFromClarity.each { project ->
                // Get tasks for each project from Clarity
                List<Map> tasksForProject = ClarityService.getTasks(project.id)
                tasksFromClarity.addAll(tasksForProject)
                println "Tasks for Project ${project.name} from Clarity: ${tasksForProject}"
            }

            // Generate XOG for Resources and print or save the XML to file
            String resourcesXML = XOGGenerator.generateResourcesXML(resourcesData)
            println "Generated Resources XOG XML:\n${resourcesXML}"

             //Save resources XOG XML to file
            XOGGenerator.saveToFile("resources_xog.xml", resourcesXML)

            // Assuming you also have assignments data that needs to be processed
            List<Map> assignmentsData = service.readAssignmentsCSV() // This method should read the assignments CSV
            println "Assignments Data: ${assignmentsData}"

            // Generate XOG for Assignments and print or save the XML to file
            String assignmentsXML = XOGGenerator.generateAssignmentXOGXML(resourcesData, assignmentsData, tasksData, projectsData,tasksFromClarity, projectsFromClarity)
            println "Generated Assignments XOG XML:\n${assignmentsXML}"

            // Save assignments XOG XML to file
            XOGGenerator.saveToFile("assignments_xog.xml", assignmentsXML)

             //Post projects and tasks to Clarity
            //postProjectsAndTasksToClarity(projectsData, tasksData)

        } catch (Exception e) {
            logger.error("Error reading CSV files: {}", e.message)
            e.printStackTrace()
        }
    }

     //Method to post projects and their associated tasks to Clarity
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






