package org.example

import de.itdesign.clarity.rest.ClarityRestClient
import de.itdesign.clarity.rest.RestResponse
import groovy.json.JsonBuilder
import groovy.sql.Sql

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

                        // Prepare the task data with the internal project ID
                        def taskData = [
                                name     : task.name,
                                _parentId: internalId,
                                status   : task.status
                        ]

                        // Construct the endpoint URL for the task
                        String taskEndpoint = "/projects/${internalId}/tasks"
                        println "Posting task to endpoint: ${taskEndpoint}, with data: ${taskData}"

                        RestResponse taskResponse = sendRequest('POST', taskEndpoint, taskData)

                        println "Task creation response: ${taskResponse?.jsonMap()}"

                        if (taskResponse) {
                            println "Task created successfully: ${taskData}"
                        } else {
                            println "Failed to create task: ${taskData}. Response: ${taskResponse?.jsonMap()}"
                        }
                    }
                }
            } else {
                println "Failed to create project: ${projectData}. Response: ${projectResponse?.jsonMap()}"
            }
        }
    }
}





