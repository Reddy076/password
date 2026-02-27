# Feature 14: User Settings API & Postman Guide

## Base URL
`http://localhost:8080`

## Authentication
All endpoints require a valid JWT token in the Authorization header.
`Authorization: Bearer <your_jwt_token>`

---

## 1. Get User Settings
**Endpoint:** `GET /api/users/settings`
**Description:** Retrieve the current user's settings.

### Request
**Method:** GET
**URL:** `{{base_url}}/api/users/settings`

### Response (200 OK)
```json
{
    "id": 1,
    "theme": "DARK",
    "language": "en_US",
    "marketingEmails": true,
    "securityAlerts": true,
    "twoFactorEnabled": false
}
```

---

## 2. Update User Settings
**Endpoint:** `PUT /api/users/settings`
**Description:** Update the current user's settings.

### Request
**Method:** PUT
**URL:** `{{base_url}}/api/users/settings`
**Body:** (JSON)
```json
{
    "theme": "LIGHT",
    "language": "es_ES",
    "marketingEmails": false,
    "securityAlerts": true
}
```

### Response (200 OK)
```json
{
    "id": 1,
    "theme": "LIGHT",
    "language": "es_ES",
    "marketingEmails": false,
    "securityAlerts": true,
    "twoFactorEnabled": false
}
```
