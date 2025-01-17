package org.example

import groovy.xml.MarkupBuilder
import java.nio.file.Files
import java.nio.file.Paths

class XOGGenerator {

    // Validate if a resource-task combination already exists to avoid duplicates
    static boolean isDuplicateAssignment(List<Map> existingAssignments, Map assignment) {
        println("Checking if assignment is duplicate: ${assignment}")
        return existingAssignments.any { existingAssignment ->
            existingAssignment.resource_id == assignment.resource_id && existingAssignment.task_id == assignment.task_id
        }
    }

    // Generate XOG file for Resources using StringWriter
    static String generateResourcesXML(List<Map> resourcesData) {
        // Create a StringWriter to hold the generated XML
        StringWriter writer = new StringWriter()

        // Create the MarkupBuilder with the StringWriter
        def xml = new MarkupBuilder(writer)

        println("Generating Resources XOG XML...")
        // Define the XML structure with the necessary namespaces and schema
        xml.NikuDataBus('xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                'xsi:noNamespaceSchemaLocation': '../xsd/nikuxog_resource.xsd') {
            xml.Header(version: '6.0.12', action: 'write', objectType: 'resource', externalSource: 'ORACLE-FINANCIAL')

            xml.Resources {
                // Iterate through each resource and create a <Resource> element
                resourcesData.each { resource ->
                    // Split the resource name into first and last name
                    def nameParts = resource.name.split(' ', 2)
                    def firstName = nameParts[0]
                    def lastName = nameParts.size() > 1 ? nameParts[1] : ''

                    println("Creating XML for resource: ${resource.name}")
                    xml.Resource(resourceId: resource.id, isActive: resource.is_active.toString().toLowerCase(),
                            employmentType: "Employee", resourceType: "LABOR", externalId: "2323AAA") {
                        xml.PersonalInformation(lastName: lastName, firstName: firstName, emailAddress: resource.email)
                    }
                }
            }
        }
        println("Generated Resources XOG XML successfully.")
        return writer.toString()
    }

    // Method to save the generated XML to a file
    static void saveToFile(String fileName, String content) {
        println("Saving XOG XML to file: ${fileName}")

        try {
            // Save the content to the specified file
            Files.write(Paths.get(fileName), content.bytes)
            println("File saved successfully: ${fileName}")
        } catch (IOException e) {
            println("Failed to save file: ${fileName}")
            e.printStackTrace()
        }
    }

    static String generateAssignmentXOGXML(
            List<Map> resourcesData,
            List<Map> assignmentsData,
            List<Map> tasksData,
            List<Map> projectsData,
            List<Map> filteredProjects) {

        // Create a StringWriter to hold the generated XML
        StringWriter writer = new StringWriter()

        // Create the MarkupBuilder with the StringWriter
        def xml = new MarkupBuilder(writer)

        println("Generating Assignment XOG XML...")
        // Define the XML structure with the necessary namespaces and schema
        xml.NikuDataBus('xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                'xsi:noNamespaceSchemaLocation': '../xsd/nikuxog_project.xsd') {

            xml.Header(action: 'write', externalSource: 'NIKU', objectType: 'project', version: '7.1.0.3023')

            xml.Projects {
                // Loop through each project and generate the XML for it
                projectsData.each { project ->
                    // Ensure the project is valid and matches the filtered projects
                    def matchingProjectFromClarity = filteredProjects.find { it.name == project.name }

                    if (matchingProjectFromClarity) {
                        // Start the Project element
                        xml.Project(projectID: matchingProjectFromClarity.code, name: project.name) {
                            xml.Tasks {
                                // Track tasks processed for the current project
                                def processedTasks = new HashSet()

                                // Loop through tasksData and create XML for tasks related to this project
                                tasksData.findAll { it.project_id == project.id }.each { task ->
                                    // Ensure the task has assignments
                                    def taskAssignments = assignmentsData.findAll { it.task_id == task.id }

                                    if (taskAssignments) {
                                        // Only generate XML for tasks that haven't been processed yet
                                        if (!processedTasks.contains(task.name)) {
                                            processedTasks.add(task.name)

                                            println("Generating XML for task: ${task.name}")

                                            // Generate XML for the task and its assignments
                                            xml.Task(taskID: task.id, outlineLevel: "1", name: task.name) {
                                                xml.Assignments {
                                                    // Loop through the assignment data and create TaskLabor tags
                                                    def existingAssignments = []
                                                    // Stores the already processed assignments

                                                    taskAssignments.each { assignment ->
                                                        // Check for duplicate assignment
                                                        if (isDuplicateAssignment(existingAssignments, assignment)) {
                                                            // Log the duplicate and skip it
                                                            println("Duplicate assignment skipped: ${assignment}")
                                                            return // Skip the duplicate
                                                        }

                                                        // Add the current assignment to the list of processed ones
                                                        existingAssignments << assignment

                                                        println("Processing assignment for task: ${task.name}, Assignment: ${assignment}")
                                                        // Check if resource matches
                                                        def resource = resourcesData.find { it.id.toString().trim() == assignment.resource_id.toString().trim() }

                                                        if (resource) {
                                                            // Debugging - log the resource match
                                                            println("Found matching resource for assignment: ${assignment.resource_id} -> ${resource.id}")

                                                            // Add TaskLabor inside the Assignments tag
                                                            xml.TaskLabor(
                                                                    resourceID: resource.id) {
                                                                xml.CustomInformation()
                                                            }
                                                        } else {
                                                            // If no matching resource, log this for debugging
                                                            println("No matching resource for assignment: ${assignment}")
                                                        }
                                                    }
                                                }
                                                xml.CustomInformation()
                                            }
                                        }
                                    } else {
                                        // Log if no assignments were found for the task
                                        println("No assignments found for task: ${task.name}")
                                    }
                                }
                            }
                            xml.Dependencies()
                            xml.CustomInformation()
                            xml.OBSAssocs()
                        }
                    }
                }
            }
        }
        // Return the generated XML as a string
        println("Generated Assignment XOG XML successfully.")
        return writer.toString()
    }
}






