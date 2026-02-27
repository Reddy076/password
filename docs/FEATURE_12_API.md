# 🔐 Feature 12: Password Generator API Documentation

Base URL: `http://localhost:8080/api/generator`

## Security & Authorization
*   **Authentication**: **NONE** (Public API).
*   **Access Control**: Open to all users (authenticated or anonymous).
*   **Reason**: This service provides stateless utility functions (random string generation, math-based strength calculation). It does **not** access, store, or modify any user data or database records.


## 1. Generate Password
**POST** `/api/generator/generate`
*   **Description**: Generates a secure random password.
*   **Request Body** (Optional):
    *   Custom options (see below).
    *   **If empty**, uses secure defaults (16 chars, Uppercase, Lowercase, Numbers, Special).
    ```json
    {
      "length": 16,
      // ... other options
    }
    ```
*   **Simple Usage (Defaults)**:
    *   `POST /api/generator/generate` (No body)
*   **Response**:
    ```json
    {
      "password": "CorrectHorseBatteryStaple1!"
    }
    ```

## 2. Check Password Strength
**POST** `/api/generator/strength`
*   **Description**: Analyzes a password and returns a strength score (0-100) and feedback.
*   **Request Body**:
    ```json
    {
      "password": "password123"
    }
    ```
*   **Response**:
    ```

## 3. Frontend Integration Example (JavaScript/Fetch)
Here is how you would call this API from a frontend application (e.g., React, Angular, or Vanilla JS):

```javascript
// 1. Collect values from your UI inputs (checkboxes, sliders)
const requestBody = {
  length: 20,                          // value from length slider
  includeUppercase: true,              // value from "A-Z" checkbox
  includeLowercase: true,              // value from "a-z" checkbox
  includeNumbers: true,                // value from "0-9" checkbox
  includeSpecial: false,               // value from "!@#" checkbox
  excludeSimilar: true                 // value from "Easy to read" checkbox
};

// 2. Send the request to the backend
fetch('http://localhost:8080/api/generator/generate', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer ' + yourAuthToken // If using JWT
  },
  body: JSON.stringify(requestBody) // Converts JS object to JSON string
})
.then(response => response.json())
.then(data => {
  // 3. Use the generated password
  console.log("Generated Password:", data.password);
  // Example: document.getElementById("passwordField").value = data.password;
})
.catch(error => console.error('Error:', error));
```json
    {
      "score": 16,
      "label": "Very Weak",
      "feedback": [
        "Add uppercase letters",
        "Add special characters",
        "Avoid common patterns or repeated characters"
      ]
    }
    ```
