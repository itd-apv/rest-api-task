package org.example

class DataInsertion {

    static void main(String[] args) {
        CSVParsing parsing = new CSVParsing()

        try {
            println("Starting CSV parsing...")
            // Read CSVs and get data as list of maps
            List<Map> projectsData = parsing.readProjectsCSV()
            println("Projects Data: ${projectsData}")

            List<Map> tasksData = parsing.readTasksCSV()
            println("Tasks Data: ${tasksData}")

            // Process and generate XOGs (like before)
            List<Map> resourcesData = parsing.readResourcesCSV()
            println("Resources Data: ${resourcesData}")

            List<Map> assignmentsData = parsing.readAssignmentsCSV()
            println("Assignments Data: ${assignmentsData}")

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
            println("Posted resources as teams to Clarity PPM successfully.")


        } catch (Exception e) {
            println("Error reading CSV files: ${e.message}")
            e.printStackTrace()
        }finally {
            // Close the database connection at the end of the process
            ClarityService.closeDBConnection()
            println("Database connection closed successfully.")
        }
    }


    //Method to post projects and their associated tasks to Clarity
    static void postProjectsAndTasksToClarity(List<Map> projectsData, List<Map> tasksData) {
        try {
            // Send projects data along with tasks to Clarity service
            ClarityService.postProjectsWithTasks(projectsData, tasksData)  // Passing both projects and tasks data
            println("Posted projects and tasks to Clarity PPM successfully.")

        } catch (Exception e) {
            println("Error posting projects and tasks to Clarity PPM: ${e.message}")
            e.printStackTrace()
        }
    }
}