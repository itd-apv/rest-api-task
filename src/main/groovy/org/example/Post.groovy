package org.example

class Post {
    static void main(String[] args) {
        // Prepare task data
        def taskData = [
                "code": "z_exam",
                "name": "exam",
        ]

        // Send POST request to create task
        ClarityService.sendRequest("POST", "/projects/5002001/tasks", taskData)
    }
}



