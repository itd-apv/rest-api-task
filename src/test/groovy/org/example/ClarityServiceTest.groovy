package org.example

import de.itdesign.clarity.rest.ClarityRestClient
import de.itdesign.clarity.rest.RestResponse
import groovy.sql.Sql
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.InjectMocks
import org.mockito.MockitoAnnotations
import static org.mockito.Mockito.*

class ClarityServiceTest {

    @Mock
    Sql mockSql

    @Mock
    ClarityRestClient mockRestClient // Mock the RestClient

    @InjectMocks
    ClarityService clarityService  // Inject ClarityService

    @BeforeEach
    void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this)

        // Ensure getDBConnection is returning the mockSql
        clarityService.metaClass.getDBConnection = { return mockSql }

        // Mock the `firstRow()` method on Sql
        def mockRow = new groovy.sql.GroovyRowResult([ID: 1, CODE: 'P1', NAME: 'Project1'])
        when(mockSql.firstRow(anyString(), anyList())).thenReturn(mockRow)

        // Mock `eachRow()` for multiple rows
        when(mockSql.eachRow(anyString(), anyList(), any())).thenAnswer { invocation ->
            def closure = invocation.getArgument(2)
            closure.call([ID: 1, CODE: 'P1', NAME: 'Project1'])
            closure.call([ID: 2, CODE: 'P2', NAME: 'Project2'])
        }

        // Mock the behavior of RestClient's POST method if needed
        RestResponse mockResponse = mock(RestResponse)
        when(mockRestClient.POST(anyString(), anyString())).thenReturn(mockResponse)
    }

    @Test
    void testPostProjectsWithTasks() {
        // Test posting projects and tasks
        def projects = [
                [id: 1, name: "Project1", scheduleStart: "2023-01-02T08:00:00", scheduleFinish: "2024-01-02T08:00:00", createdDate: "2023-01-01T08:00:00", isActive: true]
        ]
        def tasks = [
                [id: 101, name: "Task1", _parentId:'5000002', status: "'Not Started': [displayValue: 'Not Started', _type: 'lookup', id: '0']"]
        ]

        // Call the method to post projects and tasks
        ClarityService.postProjectsWithTasks(projects, tasks)

        // Verify interactions using matchers for both arguments
        verify(mockRestClient, times(1)).POST(eq("http://10.0.0.98:7080/ppm/rest/v1/projects/5000002/tasks"), anyString())
    }




    @Test
    void testGetResourceDetails() {
        String resourceCode = "RS01"

        // Mock the behavior to return a resource
        when(mockSql.firstRow(anyString(), eq([resourceCode]))).thenReturn([ID: 1, UNIQUE_NAME: resourceCode])

        // Call the method to get resource details
        def resource = ClarityService.getResourceDetails(resourceCode)

        // Verify the returned resource
        assert resource != null
        assert resource.code == resourceCode
        assert resource.id == 1
    }

    @Test
    void testPostTeamsWithResources() {
        // Prepare sample XML data for posting teams and resources
        def xmlData = '''<Projects>
                            <Project name="Project1" projectID="1">
                                <Tasks>
                                    <Task>
                                        <Assignments>
                                            <TaskLabor resourceID="RS01"/>
                                        </Assignments>
                                    </Task>
                                </Tasks>
                            </Project>
                        </Projects>'''

        // Mock behavior for posting team data
        when(mockRestClient.POST(anyString(), anyString())).thenReturn(mockResponse)

        // Call the method to post teams with resources
        ClarityService.postTeamsWithResources(xmlData)

        // Verify the interactions
        verify(mockRestClient, times(1)).POST(anyString(), anyString())
    }

    @Test
    void testPostTeamToClarity() {
        // Prepare team data for posting
        def teamData = [resource: 1]

        // Mock behavior for posting team data
        when(mockRestClient.POST(anyString(), anyString())).thenReturn(mockResponse)

        // Call the method to post team data
        RestResponse response = ClarityService.postTeamToClarity("/projects/1/teams", teamData)

        // Verify the result
        assert response != null
        verify(mockRestClient, times(1)).POST(anyString(), anyString())
    }
}




