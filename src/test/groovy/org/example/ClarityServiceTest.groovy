package org.example

//import de.itdesign.clarity.rest.ClarityRestClient
//import de.itdesign.clarity.rest.RestResponse
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.MockitoAnnotations;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import static org.mockito.Mockito.*;
//import static org.mockito.ArgumentMatchers.*;
//
//@ExtendWith(MockitoExtension.class)  // Use MockitoExtension for JUnit 5
//class ClarityServiceTest {
//
//    @Mock
//    ClarityRestClient mockRestClient;
//
//    @Mock
//    RestResponse mockRestResponse;
//
//    @BeforeEach
//    void setUp() {
//        // MockitoExtension automatically initializes the mocks
//    }
//
//    @Test
//    void testPostProjectsWithTasks() {
//        // Arrange: Prepare mock responses and request data
//        List<Map> projects = List.of(Map.of(
//                "name", "Project 1",
//                "start", "2025-01-01",
//                "finish", "2025-12-31",
//                "created_date", "2025-01-01",
//                "is_active", true)
//        );
//        List<Map> tasks = List.of(Map.of(
//                "name", "Task 1",
//                "project_id", 1,
//                "status", "Not Started")
//        );
//
//        // Mock the response for the project creation
//        when(mockRestClient.POST(eq("/projects"), anyString())).thenReturn(mockRestResponse);
//        when(mockRestResponse.jsonMap()).thenReturn(Map.of("_internalId", "12345"));
//
//        // Act: Call the method you're testing
//        ClarityService.postProjectsWithTasks(projects, tasks);
//
//        // Assert: Verify interactions with the mockRestClient
//        verify(mockRestClient, times(1)).POST(eq("/projects"), anyString());
//        verify(mockRestClient, times(1)).POST(eq("/projects/12345/tasks"), anyString());
//    }
//}
//
//


