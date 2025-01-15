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

            // Process and generate XOGs (like before)
            List<Map> resourcesData = service.readResourcesCSV() // This method should read the resources CSV
            println "Resources Data: ${resourcesData}"

            List<Map> assignmentsData = service.readAssignmentsCSV() // This method should read the assignments CSV
            println "Assignments Data: ${assignmentsData}"

            //Post projects and tasks to Clarity
            //postProjectsAndTasksToClarity(projectsData, tasksData)

            // Get project names from CSV for filtering
            List<String> projectNamesFromCSV = projectsData.collect { it.name }

            // Get task names from CSV for filtering
            List<String> taskNamesFromCSV = tasksData.collect { it.name }

            // Fetch projects from the database instead of Clarity
            List<Map> filteredProjects = ClarityService.getProjects(projectNamesFromCSV)
            println "Filtered Projects from Database: ${filteredProjects}"

            List<Map> filteredTasks = []
            filteredProjects.each { project ->
                // Fetch tasks from the database for each filtered project
                List<Map> tasksForProject = ClarityService.getTasks(project.id.intValue(), taskNamesFromCSV)
                filteredTasks.addAll(tasksForProject)
                println "Filtered Tasks for Project ${project.name} from Database: ${tasksForProject}"
            }

            // Generate XOG for Resources and print or save the XML to file
            String resourcesXML = XOGGenerator.generateResourcesXML(resourcesData)
            println "Generated Resources XOG XML:\n${resourcesXML}"

            //Save resources XOG XML to file
            XOGGenerator.saveToFile("resources_xog.xml", resourcesXML)

            // Generate XOG for Assignments and print or save the XML to file
            String assignmentsXML = XOGGenerator.generateAssignmentXOGXML(resourcesData, assignmentsData, tasksData, projectsData, filteredProjects)
            println "Generated Assignments XOG XML:\n${assignmentsXML}"

            // Save assignments XOG XML to file
            XOGGenerator.saveToFile("assignments_xog.xml", assignmentsXML)

            String xmlData = new File("assignments_xog.xml").text // Reading the XML file generated above
            ClarityService.postTeamsWithResources(xmlData)  // Call the method to post teams with resources
            logger.info("Posted resources as teams to Clarity PPM successfully.")


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