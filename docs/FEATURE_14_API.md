# Feature 14: User Settings API Documentation

## Overview
The User Settings API allows users to customize their application experience, such as theme, language, and security preferences.

## Security & Authorization
*   **Authentication**: `Bearer Token` (JWT) is **REQUIRED**.
*   **Authorization**: Users can only access and modify their own settings.

## Endpoints

### 1. Get User Settings
**GET** `/api/settings`

Retrieves the current user's settings. If no settings exist, default values are created and returned.

**Response:** `200 OK`
```json
{
  "theme": "SYSTEM",
  "language": "en-US",
  "autoLogoutMinutes": 15
}
```

### 2. Update User Settings
**PUT** `/api/settings`

Updates the user's settings. You can provide one or more fields to update.

**Request Body:**
```json
{
  "theme": "DARK",
  "autoLogoutMinutes": 30
}
```

**Response:** `200 OK`
```json
{
  "theme": "DARK",
  "language": "en-US",
  "autoLogoutMinutes": 30
}
```
