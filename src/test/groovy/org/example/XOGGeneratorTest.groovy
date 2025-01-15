package org.example

import org.junit.jupiter.api.Test

import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.nio.file.Files
import java.nio.file.Paths

class XOGGeneratorTest {

    // Initialize the logger for the class
    private static final Logger logger = LogManager.getLogger(XOGGeneratorTest)

    @Test
    void testIsDuplicateAssignment() {
        logger.info("Test started: testIsDuplicateAssignment")

        // Test data
        List<Map> assignments = [
                [resource_id: 1, task_id: 101],
                [resource_id: 2, task_id: 102]
        ]
        Map assignmentToCheck = [resource_id: 1, task_id: 101]

        // Test case where assignment is a duplicate
        logger.debug("Checking for duplicate assignment: $assignmentToCheck")
        assertTrue(XOGGenerator.isDuplicateAssignment(assignments, assignmentToCheck))

        // Test case where assignment is not a duplicate
        Map assignmentNotDuplicate = [resource_id: 1, task_id: 103]
        logger.debug("Checking for non-duplicate assignment: $assignmentNotDuplicate")
        assertFalse(XOGGenerator.isDuplicateAssignment(assignments, assignmentNotDuplicate))

        logger.info("Test completed: testIsDuplicateAssignment")
    }

    @Test
    void testGenerateResourcesXML() {
        logger.info("Test started: testGenerateResourcesXML")

        // Sample input data
        List<Map> resourcesData = [
                [id: 1, name: "John Doe", is_active: true, email: "john.doe@example.com"],
                [id: 2, name: "Jane Smith", is_active: false, email: "jane.smith@example.com"]
        ]

        // Generate the XML
        logger.debug("Generating resources XML with data: $resourcesData")
        String xml = XOGGenerator.generateResourcesXML(resourcesData)

        // Check if the XML is generated correctly
        logger.debug("Generated XML: $xml")
        assertNotNull(xml)
        assertTrue(xml.contains("<Resource"))
        assertTrue(xml.contains("<PersonalInformation"))
        assertTrue(xml.contains("john.doe@example.com"))

        logger.info("Test completed: testGenerateResourcesXML")
    }

    @Test
    void testSaveToFile() throws IOException {
        String content = "<XOG>Test</XOG>";

        // Create temporary file for testing
        Path tempFile = Files.createTempFile("test_output", ".xml");
        tempFile.toFile().deleteOnExit();

        // Assuming XOGGenerator has the saveToFile method
        XOGGenerator service = new XOGGenerator(); // Instantiate service if not already instantiated

        // Save content to the file
        service.saveToFile(tempFile.toString(), content);

        // Verify file content
        String savedContent = Files.readString(tempFile);
        assertEquals(content, savedContent); // Assert the saved content matches the expected content
    }
}

