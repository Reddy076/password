# Manual API Testing Guide (Features 19 - 22.5)

This document provides endpoint details for testing Features 19 through 22.5 using Postman or similar tools.

## Prerequisites
- **Base URL**: `http://localhost:8080` (adjust port if needed)
- **Auth Header**: All endpoints require a valid JWT Access Token.
    - Key: `Authorization`
    - Value: `Bearer <your_access_token>`

---

## Feature 19: Login History

### 1. Get Login History
Retrieves the history of login attempts for the current user.

- **Method**: `GET`
- **URL**: `/api/security/login-history`
- **Response (200 OK)**:
```json
[
  {
    "id": 1,
    "ipAddress": "127.0.0.1",
    "deviceInfo": "PostmanRuntime/7.32.3",
    "location": "Unknown", // GeoIP not implemented locally
    "successful": true,
    "failureReason": null,
    "timestamp": "2024-03-15T10:00:00"
  },
  {
    "id": 2,
    "ipAddress": "192.168.1.5",
    "deviceInfo": "Mozilla/5.0...",
    "location": "Unknown",
    "successful": false,
    "failureReason": "Invalid credentials",
    "timestamp": "2024-03-14T15:30:00"
  }
]
```

---

## Feature 20: Security Audit Logs

### 2. Get Audit Logs
Retrieves security audit logs (actions performed) for the current user.

- **Method**: `GET`
- **URL**: `/api/security/audit-logs`
- **Response (200 OK)**:
```json
[
  {
    "id": 105,
    "action": "ENTRY_CREATED",
    "details": "Created entry: Facebook",
    "ipAddress": "0:0:0:0:0:0:0:1",
    "timestamp": "2024-03-15T12:00:00"
  },
  {
    "id": 106,
    "action": "PASSWORD_VIEWED",
    "details": "Viewed password for entry: Netflix",
    "ipAddress": "0:0:0:0:0:0:0:1",
    "timestamp": "2024-03-16T09:15:00"
  }
]
```

---

## Feature 21: Security Alerts

### 3. Get Security Alerts
Retrieves active security alerts (e.g., failed logins, new devices) for the user.

- **Method**: `GET`
- **URL**: `/api/security/alerts`
- **Response (200 OK)**:
```json
[
  {
    "id": 12,
    "alertType": "NEW_DEVICE_LOGIN",
    "title": "New Device Detected",
    "message": "Login detected from a new device or browser: PostmanRuntime/7.32.3",
    "severity": "MEDIUM",
    "isRead": false,
    "createdAt": "2024-03-16T10:05:00"
  }
]
```

### 4. Mark Alert as Read
Marks a specific alert as read.

- **Method**: `PUT`
- **URL**: `/api/security/alerts/{id}/read`
- **Path Variables**:
    - `id`: ID of the alert (e.g., `12`)
- **Response (200 OK)**:
    - Body: `"Alert marked as read"`

### 5. Delete Alert
Deletes a specific alert.

- **Method**: `DELETE`
- **URL**: `/api/security/alerts/{id}`
- **Path Variables**:
    - `id`: ID of the alert (e.g., `12`)
- **Response (200 OK)**:
    - Body: `"Alert deleted"`

---

## Feature 22: Security Audit Report (Password Health)

### 6. Get Audit Report
Generates a report analyzing the health of the user's vault (weak, reused, old passwords).

- **Method**: `GET`
- **URL**: `/api/security/audit-report`
- **Response (200 OK)**:
```json
{
  "totalEntries": 15,
  "weakCount": 2,
  "reusedCount": 3,
  "oldCount": 1,
  "securityScore": 65,
  "recommendations": [
    "Update 2 weak password(s) with stronger alternatives",
    "Change 3 reused password(s) to unique values"
  ],
  "weakPasswords": [
    {
      "id": 5,
      "title": "Old Forum",
      "websiteUrl": "http://forum.example.com",
      "issue": "Weak password (score: 20/100)"
    }
  ],
  "reusedPasswords": [
    {
      "id": 8,
      "title": "Service A",
      "websiteUrl": "http://a.com",
      "issue": "Password reused across 2 entries"
    }
  ],
  "oldPasswords": []
}
```

---

## Feature 22.5: User Activity Heatmap

### 7. Get Activity Heatmap
Retrieves aggregated stats of user activity over the last 30 days for visualization.

- **Method**: `GET`
- **URL**: `/api/users/activity-heatmap`
- **Response (200 OK)**:
```json
{
  "accessByHour": [0, 0, 0, 5, 12, 8, 2, ...], // Array of 24 integers (00:00 - 23:00)
  "accessByDay": [15, 45, 30, 60, 20, 10, 5], // Array of 7 integers (Sun - Sat)
  "peakHour": 14,
  "peakDay": "Wednesday",
  "totalAccesses": 187,
  "period": "LAST_30_DAYS"
}
```
