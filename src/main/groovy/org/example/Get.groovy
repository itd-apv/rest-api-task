package org.example

class Get {
    static void main(String[] args) {
        // Send GET request to fetch project data
        ClarityService.sendRequest("GET", "/projects/5002001")
    }
}
