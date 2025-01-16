package org.example

import groovy.sql.Sql
import de.itdesign.clarity.rest.ClarityRestClient
import de.itdesign.clarity.rest.RestResponse
import groovy.json.JsonBuilder
import groovy.xml.XmlParser

import java.sql.Connection
import java.sql.DriverManager

class ClarityService {

    // Static variable to hold the database connection and SQL object
    static Connection connection
    static Sql sql

    // Method to get the database connection (will reuse the connection)
    static Connection getDBConnection() {
        if (connection == null) {
            String jdbcurl = "jdbc:oracle:thin:@//10.0.0.98:11521/clarity"
            String username = "niku"
            String password = "niku"
            try {
                println("Attempting to connect to the database...")
                connection = DriverManager.getConnection(jdbcurl, username, password)
                sql = new Sql(connection)  // Reusing the connection to create a Sql object
                println("Database connection successful.")
            } catch (Exception e) {
                println("Caught exception: ${e.message}")
                e.printStackTrace()
            }
        }
        return connection
    }

    // Method to send HTTP requests
    static RestResponse sendRequest(String httpMethod, String endpoint, Map data = null) {
        // Ensure database connection is available
        getDBConnection()

        ClarityRestClient rest = new ClarityRestClient("admin", sql.getConnection(), "http://10.0.0.98:7080")

        // Convert the data to JSON format
        def jsonData = data ? new JsonBuilder(data).toString() : null

        RestResponse response
        try {
            // Send the appropriate HTTP request based on the method type
            if (httpMethod == 'POST') {
                response = rest.POST(endpoint, jsonData)
            } else if (httpMethod == 'PATCH') {
                response = rest.PATCH(endpoint, jsonData)
            } else if (httpMethod == 'GET') {
                response = rest.GET(endpoint)
            }

            println("Response: ${response?.jsonMap()}")
        } catch (Exception e) {
            println("Caught exception: ${e.message}")
            e.printStackTrace()
        } finally {
            rest?.close()
        }

        return response
    }

    // Method to post projects with tasks
    static void postProjectsWithTasks(List<Map> projects, List<Map> tasks) {
        projects.each { project ->
            // Prepare the JSON data for the project
            def projectData = [
                    name          : project.name,
                    scheduleStart : project.start,
                    scheduleFinish: project.finish,
                    createdDate   : project.created_date,
                    isActive      : project.is_active
            ]

            // Post the project data to the /projects endpoint
            println("Posting project: ${projectData}")
            RestResponse projectResponse = sendRequest('POST', '/projects', projectData)

            if (projectResponse?.jsonMap()?._internalId) {
                def internalId = projectResponse.jsonMap()?._internalId
                println("Project created with internal ID: ${internalId}")

                // Find tasks associated with this project based on project.id
                def associatedTasks = tasks.findAll { it.project_id == project.id }

                println("Associated tasks for project ${project.name} (ID: ${project.id}): ${associatedTasks}")

                if (associatedTasks.isEmpty()) {
                    println("No tasks found for project ${project.name} (ID: ${project.id})")
                } else {
                    // Maintain a set of task names that have already been posted
                    def postedTaskNames = []

                    associatedTasks.each { task ->
                        if (postedTaskNames.contains(task.name)) {
                            // Skip task if it has already been posted
                            println("Skipping task '${task.name}' as it has already been posted.")
                        } else {
                            println("Task: ${task.name}, Status: ${task.status}")


                            // Status lookup mapping
                            def statusLookup = [
                                    'Not Started': [displayValue: 'Not Started', _type: 'lookup', id: '0'],   // Lookup object for 'Not Started'
                                    'In Progress': [displayValue: 'In Progress', _type: 'lookup', id: '1'],   // Lookup object for 'In Progress'
                                    'Completed'  : [displayValue: 'Completed', _type: 'lookup', id: '2']      // Lookup object for 'Completed'
                            ]

                            // Retrieve the status lookup object based on task's status
                            def validStatus = statusLookup[task.status]

                            if (validStatus) {
                                // Prepare the task data with the internal project ID and the correct status object
                                def taskData = [
                                        name     : task.name,
                                        _parentId: internalId,
                                        status   : validStatus,
                                        code     : task.id  // Only send the valid status object
                                ]

                                // Construct the endpoint URL for the task
                                String taskEndpoint = "/projects/${internalId}/tasks"
                                println("Posting task to endpoint: ${taskEndpoint}, with data: ${taskData}")

                                RestResponse taskResponse = sendRequest('POST', taskEndpoint, taskData)

                                println("Task creation response: ${taskResponse?.jsonMap()}")


                                if (taskResponse) {
                                    // Mark task as posted by adding it to the set
                                    postedTaskNames.add(task.name)
                                    println("Task created successfully: ${taskData}")
                                } else {
                                    println("Failed to create task: ${taskData}. Response: ${taskResponse?.jsonMap()}")
                                }
                            } else {
                                println("Invalid status '${task.status}' for task '${task.name}'. Skipping task creation.")
                            }
                        }
                    }
                }
            } else {
                println("Failed to create project: ${projectData}. Response: ${projectResponse?.jsonMap()}")
            }
        }
    }

    static List<Map> getProjects(List<String> projectNamesFromCSV) {
        getDBConnection()  // Ensure the connection is established

        def allProjects = []

        // Query to get matching projects
        String query = """
        SELECT ID, CODE, NAME
        FROM INV_INVESTMENTS
        WHERE NAME IN (${projectNamesFromCSV.collect { "'${it}'" }.join(",")})
        """

        sql.eachRow(query) { row ->  // Process each row returned by the query
            allProjects << [
                    id  : row.id,  // Access the 'id' column
                    code: row.code,  // Access the 'code' column
                    name: row.name  // Access the 'name' column
            ]
        }

        return allProjects
    }

    static List<Map> getTasks(Integer projectId, List<String> taskNamesFromCSV) {
        getDBConnection()  // Ensure the connection is established

        def allTasks = []

        // Define the SQL query to fetch tasks for the given projectId
        def query = """
        SELECT PRID, PRNAME 
        FROM PRTASK 
        WHERE PRPROJECTID = ? AND PRNAME IN (${taskNamesFromCSV.collect { '?' }.join(', ')})
        """

        // Execute the query with parameters
        sql.eachRow(query, [projectId, *taskNamesFromCSV]) { row ->  // Pass parameters to the query
            allTasks << [
                    id  : row.prid,  // Access the 'prid' column
                    name: row.prname  // Access the 'prname' column
            ]
        }

        return allTasks
    }

    // Method to retrieve project internal_id from the database using project name
    static String getProjectInternalId(String projectName) {
        getDBConnection()  // Ensure the connection is established

        def query = "SELECT ID FROM INV_INVESTMENTS WHERE name = ?"
        def result = sql.firstRow(query, [projectName])

        if (result?.ID) {
            return result.ID
        } else {
            return null
        }
    }

    static void postTeamsWithResources(String xmlData) {
        getDBConnection()  // Ensure the connection is established

        def xmlParser = new XmlParser()
        def parsedXml = xmlParser.parseText(xmlData)

        // Iterate through projects in the XML
        parsedXml.'Projects'.'Project'.each { project ->
            def projectName = project.@name
            def projectId = project.@projectID

            // Retrieve the internal_id of the project from the database
            String internalId = getProjectInternalId(projectName)
            if (internalId) {
                project.'Tasks'.'Task'.each { task ->
                    task.'Assignments'.'TaskLabor'.each { assignment ->
                        def resourceCode = assignment.@resourceID

                        // Retrieve resource details from the database
                        Map resourceDetails = getResourceDetails(resourceCode)

                        if (resourceDetails) {
                            def teamData = [
                                    resource: resourceDetails.id
                            ]

                            // Post the team assignment to the Clarity API
                            RestResponse response = postTeamToClarity("/projects/${internalId}/teams", teamData)

                            if (response?.jsonMap()) {
                                println("Successfully added resource ${resourceDetails.code} to project ${projectId} team.")
                            } else {
                                // Handle the error if the resource is already assigned to another team (project)
                                if (response?.jsonMap()?.errorCode == 'projmgr.TEAM_RESOURCE_ALREADY_STAFFED') {
                                    // Resource is already assigned to another project, check if it's the same one
                                    def existingProjectId = response.jsonMap()?.errorMessage?.split(":")?.last()?.trim()
                                    if (existingProjectId != projectId) {
                                        // Resource is already assigned to another project, but not the current one
                                        println("Resource ${resourceDetails.code} is already assigned to another project ${existingProjectId}, proceeding with adding to current project.")
                                        // You can either skip or reassign based on your requirements
                                        // Continue processing and allow adding resource to current project
                                    } else {
                                        println("Resource ${resourceDetails.code} is already assigned to project ${projectId}. Skipping.")
                                    }
                                } else {
                                    println("Failed to add resource ${resourceDetails.code} to project ${projectId} team. Response: ${response?.jsonMap()}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static RestResponse postTeamToClarity(String taskEndpoint, Map teamData) {
        RestResponse response = sendRequest('POST', taskEndpoint, teamData)

        if (response?.jsonMap()) {
            println("Successfully posted team data: ${teamData}")
        } else {
            println("Failed to post team data: ${teamData}. Response: ${response?.jsonMap()}")
        }

        return response
    }

// Method to retrieve resource id and code from the database using resource code
    static Map getResourceDetails(String resourceCode) {
        getDBConnection()  // Ensure the connection is established

        def query = "SELECT ID, UNIQUE_NAME FROM SRM_RESOURCES WHERE UNIQUE_NAME = ?"
        def resource = sql.firstRow(query, [resourceCode])

        if (resource) {
            return [id: resource.ID, code: resource.UNIQUE_NAME]
        } else {
            println("Resource with code ${resourceCode} not found in the database.")
            return null
        }
    }
}