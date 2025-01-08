package org.example

class Patch {
    static void main(String[] args) {
        // Prepare task data for update
        def taskData = [
                "name": "Goals",
        ]

        // Send PATCH request to update the task (replace with actual project/task IDs)
        ClarityService.sendRequest("PATCH", "/projects/5002001/tasks/5007002", taskData)
    }
}

