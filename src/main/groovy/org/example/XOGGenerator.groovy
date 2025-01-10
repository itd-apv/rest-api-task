package org.example

import groovy.xml.MarkupBuilder
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Files
import java.nio.file.Paths

class XOGGenerator {


    private static final Logger logger = LogManager.getLogger(XOGGenerator)

    // Generate XOG file for Resources using StringWriter
    static String generateResourcesXML(List<Map> resourcesData) {
        // Create a StringWriter to hold the generated XML
        StringWriter writer = new StringWriter()

        // Create the MarkupBuilder with the StringWriter
        def xml = new MarkupBuilder(writer)

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

                    xml.Resource(resourceId: resource.id, isActive: resource.is_active.toString().toLowerCase(),
                            employmentType: "Employee", resourceType: "LABOR", externalId: "2323AAA") {
                        xml.PersonalInformation(lastName: lastName, firstName: firstName, emailAddress: resource.email)
                    }
                }
            }
        }

        // Return the generated XML as a string
        return writer.toString()
    }

    // Method to save the generated XML to a file
    static void saveToFile(String fileName, String content) {
        logger.info("Saving XOG XML to file: {}", fileName)

        try {
            // Save the content to the specified file
            Files.write(Paths.get(fileName), content.bytes)
            logger.info("File saved successfully: {}", fileName)
        } catch (IOException e) {
            logger.error("Failed to save file: {}", fileName, e)
        }
    }
    // In XOGGenerator
    static String generateAssignmentXOGXML(List<Map> resourcesData, List<Map> assignmentsData, List<Map> tasksData, List<Map> projectsData,List<Map> tasksFromClarity,List<Map> projectsFromClarity) {
        // Create a StringWriter to hold the generated XML
        StringWriter writer = new StringWriter()

        // Create the MarkupBuilder with the StringWriter
        def xml = new MarkupBuilder(writer)

        // Define the XML structure with the necessary namespaces and schema
        xml.NikuDataBus('xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                'xsi:noNamespaceSchemaLocation': '../xsd/nikuxog_project.xsd') {

            xml.Header(action: 'write', externalSource: 'NIKU', objectType: 'project', version: '7.1.0.3023')

            xml.Projects {
                // Loop through each resource and generate the XML structure
                resourcesData.each { resource ->
                    // Loop through the assignments
                    assignmentsData.each { assignment ->
                        // Find the task associated with the assignment using task_id
                        def task = tasksData.find { it.id == assignment.task_id }

                        // If task is found, find the corresponding project
                        if (task) {
                            // Fetch the project associated with the task (using task.project_id)
                            def project = projectsData.find { it.id == task.project_id }

                            // If project is found, generate the XML for the assignment
                            if (project) {
                                // Generate XML for each project-task assignment
                                xml.Project(projectID: projectsFromClarity.code, name: project.name) {
                                    xml.Tasks {
                                        xml.Task(taskID: tasksFromClarity.code, outlineLevel: "1", name: task.name) {
                                            xml.Assignments {
                                                xml.TaskLabor(actualWork: assignment.actuals, baselineWork: "0",
                                                        remainingWork: assignment.etc, resourceID: resource.resource_id) {
                                                    xml.CustomInformation()
                                                }
                                            }
                                            xml.CustomInformation()
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
            }
        }

        // Return the generated XML as a string
        return writer.toString()
    }
}





