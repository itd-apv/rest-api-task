package org.example

import de.itdesign.clarity.rest.ClarityRestClient
import de.itdesign.clarity.rest.RestResponse
import groovy.json.JsonBuilder
import groovy.sql.Sql
import groovy.xml.XmlParser

import java.sql.Connection
import java.sql.DriverManager

class ClarityService {

    // Static variable to hold the database connection
    static Connection connection = null

    // Method to get a database connection
    static Connection getDBConnection() {
        // Checking if the connection already exists, if not, create a new one
        if (connection == null) {
            String jdbcurl = "jdbc:oracle:thin:@//10.0.0.98:11521/clarity"
            String username = "niku"
            String password = "niku"
            try {
                println "Attempting to connect to database..."
                connection = DriverManager.getConnection(jdbcurl, username, password)
                println "Database connection successful."
            } catch (Exception e) {
                println "Caught exception: ${e.message}"
                e.printStackTrace()
            }
        }
        return connection
    }

    // Method to send HTTP requests
    static RestResponse sendRequest(String httpMethod, String endpoint, Map data = null) {
        // Use the static connection
        Connection conn = getDBConnection()
        Sql sql = new Sql(conn)
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

            println "Response: ${response?.jsonMap()}"
        } catch (Exception e) {
            println "Caught exception: ${e.message}"
            e.printStackTrace()
        } finally {
            rest?.close()
        }

        return response
    }

    // Method to post projects and tasks
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
            println "Posting project: ${projectData}"
            RestResponse projectResponse = sendRequest('POST', '/projects', projectData)

            if (projectResponse?.jsonMap()?._internalId) {
                def internalId = projectResponse.jsonMap()?._internalId
                println "Project created with internal ID: ${internalId}"

                // Find tasks associated with this project based on project.id
                def associatedTasks = tasks.findAll { it.project_id == project.id }

                println "Associated tasks for project ${project.name} (ID: ${project.id}): ${associatedTasks}"

                if (associatedTasks.isEmpty()) {
                    println "No tasks found for project ${project.name} (ID: ${project.id})"
                } else {
                    associatedTasks.each { task ->
                        println "Task: ${task.name}, Status: ${task.status}"

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
                                    status   : validStatus // Only send the valid status object
                            ]

                            // Construct the endpoint URL for the task
                            String taskEndpoint = "/projects/${internalId}/tasks"
                            println "Posting task to endpoint: ${taskEndpoint}, with data: ${taskData}"

                            RestResponse taskResponse = sendRequest('POST', taskEndpoint, taskData)

                            println "Task creation response: ${taskResponse?.jsonMap()}"

                            // Correct status code comparison
                            if (taskResponse) {
                                println "Task created successfully: ${taskData}"
                            } else {
                                println "Failed to create task: ${taskData}. Response: ${taskResponse?.jsonMap()}"
                            }
                        } else {
                            println "Invalid status '${task.status}' for task '${task.name}'. Skipping task creation."
                        }
                    }
                }
            } else {
                println "Failed to create project: ${projectData}. Response: ${projectResponse?.jsonMap()}"
            }
        }
    }

    static List<Map> getProjects(List<String> projectNamesFromCSV) {
        def allProjects = []
        def offset = 0
        def limit = 100 // You can adjust this based on the API's max page size

        while (true) {
            // Make the request with the pagination parameters
            def response = sendRequest('GET', "/projects?offset=${offset}&limit=${limit}")

            if (response?.jsonMap()) {
                def projects = response.jsonMap()?._results
                allProjects.addAll(projects.collect { project ->
                    [
                            id  : project._internalId,  // Access project._internalId
                            code: project.code,         // Access project.code
                            name: project.name,         // Access project.name
                    ]
                }.findAll { it.name in projectNamesFromCSV }) // Filter based on project names from CSV

                // Check if there are more results
                if (projects.size() < limit) {
                    break // No more pages, exit the loop
                }
                offset += limit // Move to the next set of projects
            } else {
                break // Exit the loop if the response is empty or invalid
            }
        }
        return allProjects
    }

    static List<Map> getTasks(Integer projectId, List<String> taskNamesFromCSV) {
        def response = sendRequest('GET', "/projects/${projectId}/tasks?fields=code,name")
        if (response?.jsonMap()) {
            return response.jsonMap()._results.collect { task ->  // Accessing the '_results' directly
                [
                        id  : task._internalId,     // Accessing the property of each task directly
                        name: task.name
                ]
            }.findAll { it.name in taskNamesFromCSV } // Filter based on task names from CSV
        }
        return []
    }

    // Method to retrieve project internal_id from the database using project name
    static String getProjectInternalId(String projectName) {
        // Assuming the database connection is already available
        def connection = getDBConnection()
        Sql sql = new Sql(connection)

        // Query to get the internal ID of the project by name
        def query = "SELECT ID FROM INV_INVESTMENTS WHERE name = ?"
        def result = sql.firstRow(query, [projectName])

        // Log the result to inspect the columns returned
        println "Query result: ${result}"

        // Check if the result has the 'ID' column
        if (result?.ID) {
            def internalId = result.ID
            println "Found internal ID for project ${projectName}: ${internalId}"
            return internalId
        } else {
            println "Project ${projectName} not found in the database or missing internal_id."
            return null
        }
    }

    // Method to post teams with resources (mapping projects and resources, then posting teams)
    static void postTeamsWithResources(String xmlData) {
        // Parse the XML file content
        def xmlParser = new XmlParser()
        def parsedXml = xmlParser.parseText(xmlData)

        // Iterate through projects in the XML
        parsedXml.'Projects'.'Project'.each { project ->
            def projectName = project.@name
            def projectId = project.@projectID

            println "Processing project: ${projectName} (ID: ${projectId})"

            // Retrieve the internal_id of the project from the database
            String internalId = getProjectInternalId(projectName)
            if (internalId) {
                // Post resources as part of the team for this project
                project.'Tasks'.'Task'.each { task ->
                    task.'Assignments'.'TaskLabor'.each { assignment ->
                        def resourceCode = assignment.@resourceID // XML resourceID corresponds to resource code in DB
                        println "Mapping resource code: ${resourceCode} to project: ${projectName}"

                        // Retrieve resource details from the database using resourceCode
                        Map resourceDetails = getResourceDetails(resourceCode)

                        if (resourceDetails) {
                            // Prepare the data for posting the team assignment
                            def teamData = [
                                    resource: resourceDetails.id, // Use the ID from the database
                                      // Use the internalId for the project
                            ]

                            // Construct the endpoint for posting the resource as a team under the project
                            String taskEndpoint = "/projects/${internalId}/teams" // Endpoint for teams

                            // Post the team assignment to the Clarity API
                            RestResponse response = postTeamToClarity(taskEndpoint, teamData)

                            if (response?.jsonMap()) {
                                println "Successfully added resource to project team: ${teamData}"
                            } else {
                                println "Failed to add resource to project team: ${teamData}. Response: ${response?.jsonMap()}"
                            }
                        }
                    }
                }
            }
        }
    }

    // Method to post team data to Clarity
    static RestResponse postTeamToClarity(String taskEndpoint, Map teamData) {
        RestResponse response = sendRequest('POST', taskEndpoint, teamData)

        if (response?.jsonMap()) {
            println "Successfully posted team data: ${teamData}"
        } else {
            println "Failed to post team data: ${teamData}. Response: ${response?.jsonMap()}"
        }

        return response
    }

    // Method to retrieve resource id and code from the database using resource code
    static Map getResourceDetails(String resourceCode) {
        def connection = getDBConnection()
        Sql sql = new Sql(connection)

        // Query to get resource id and code from the resources table
        def query = "SELECT ID, UNIQUE_NAME FROM SRM_RESOURCES WHERE UNIQUE_NAME = ?"
        def resource = sql.firstRow(query, [resourceCode])

        if (resource) {
            return [id: resource.ID, code: resource.UNIQUE_NAME]
        } else {
            println "Resource with code ${resourceCode} not found in the database."
            return null
        }
    }
}