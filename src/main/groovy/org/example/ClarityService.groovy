package org.example

import de.itdesign.clarity.rest.ClarityRestClient
import de.itdesign.clarity.rest.RestResponse
import groovy.sql.Sql
import groovy.json.JsonBuilder
import java.sql.Connection
import java.sql.DriverManager

class ClarityService {

    static Connection getDBConnection() {
        Connection connection
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
        return connection
    }

    static RestResponse sendRequest(String httpMethod, String endpoint, Map data = null) {
        Connection connection = getDBConnection()
        Sql sql = new Sql(connection)
        ClarityRestClient rest = new ClarityRestClient("admin", sql.getConnection(), "http://10.0.0.98:7080")

        // Convert the data to JSON format if it exists
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
            sql?.close()
            connection?.close()
        }

        return response
    }
}


//import de.itdesign.clarity.rest.ClarityRestClient
//import de.itdesign.clarity.rest.RestResponse
//import groovy.sql.Sql
//import groovy.json.JsonBuilder
//import org.example.controller.ResourceController
//import org.example.dao.ResourceDao
//import org.example.service.ResourceService
//
//import java.sql.Connection
//import java.sql.DriverManager
//
//class ClarityService {
//
//    static Connection getDBConnection() {
//        Connection connection
//        String jdbcurl = "jdbc:oracle:thin:@//10.0.0.98:11521/clarity"
//        String username = "niku"
//        String password = "niku"
//        try {
//            println "Attempting to connect to database..."
//            connection = DriverManager.getConnection(jdbcurl, username, password)
//            println "Database connection successful."
//        } catch (Exception e) {
//            println "Caught exception: ${e.message}"
//            e.printStackTrace()
//        }
//        return connection
//    }
//
//    static RestResponse sendRequest(String httpMethod, String endpoint, Map data = null) {
//        Connection connection = getDBConnection()
//        Sql sql = new Sql(connection)
//        ClarityRestClient rest = new ClarityRestClient("admin", sql.getConnection(), "http://10.0.0.98:7080")
//
//        // Convert the data to JSON format if it exists
//        def jsonData = data ? new JsonBuilder(data).toString() : null
//
//        RestResponse response
//        try {
//            // Send the appropriate HTTP request based on the method type
//            if (httpMethod == 'POST') {
//                response = rest.POST(endpoint, jsonData)
//            } else if (httpMethod == 'PATCH') {
//                response = rest.PATCH(endpoint, jsonData)
//            } else if (httpMethod == 'GET') {
//                response = rest.GET(endpoint)
//            }
//
//            // Optionally parse and print the JSON response
//            println "Response: ${response?.jsonMap()}"
//        } catch (Exception e) {
//            println "Caught exception: ${e.message}"
//            e.printStackTrace()
//        } finally {
//            rest?.close()
//            sql?.close()
//            connection?.close()
//        }
//
//        return response
//    }
//
//    // Main method to execute the process
//    static void main(String[] args) {
//        // Ensure to pass resourcePath as an argument or set a default
//        // Set the resource path directly (no need to check arguments)
//        String resourcePath = "itdesignSqlAssignment - Sheet1.csv"
//
//        // Print the resource path for confirmation
//        println "Using resource file: ${resourcePath}"  // Get the file path passed as an argument
//
//        // Get DB Connection
//        Connection connection = getDBConnection()
//
//        // Assuming ResourceDao, ResourceService, and ResourceController are properly defined in your Groovy files
//        ResourceDao resourceDao = new ResourceDao(connection)
//        ResourceService resourceService = new ResourceService(resourceDao)
//        ResourceController resourceController = new ResourceController(resourceService)
//
//        try {
//            resourceController.readFromCsv(resourcePath)  // Processing the CSV file
//            println "Data insertion completed."
//        } catch (Exception e) {
//            println "Error occurred during data insertion: ${e.message}"
//            e.printStackTrace()
//        } finally {
//            connection?.close()  // Always close the connection
//        }
//    }
//}
//
//
//
//
