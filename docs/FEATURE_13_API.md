# Feature 13: Vault Items API Documentation

## Overview
The Vault Items API allows users to manage their encrypted password entries.

## Security & Authorization
*   **Authentication**: `Bearer Token` (JWT) is **REQUIRED** for all endpoints.
*   **Authorization**: Access is strictly scoped to the authenticated user via JWT subject (`username`).
    *   Users cannot access vault entries belonging to others.
*   **Encryption (At-Rest)**:
    *   **Sensitive Fields**: `password`, `username`, and `notes` are **ENCRYPTED** before storage.
    *   **Algorithm**: AES-256 (GCM mode).
    *   **Key Management**: Keys are dynamically derived from the user's `masterPasswordHash` and unique `salt` using PBKDF2. This ensures that even database administrators cannot decrypt user passwords without the user's credentials.
    *   **Plaintext Fields**: `title`, `websiteUrl`, and `category`/`folder` links are stored in plaintext to enable efficient searching and organization.


## Endpoints

### 1. Create Vault Entry
**POST** `/api/vault`

Creates a new encrypted vault entry.
- **Title**: Plain text for searching.
- **Username, Password, Notes**: Encrypted server-side using derived key.

**Request Body:**
```json
{
  "title": "Netflix",
  "username": "myemail@example.com",
  "password": "superSecretPassword123!",
  "websiteUrl": "https://netflix.com",
  "notes": "Shared with family",
  "categoryId": 1,
  "folderId": 5,
  "isFavorite": true
}
```

**Response:** `201 Created`
```json
{
  "id": 10,
  "title": "Netflix",
  "username": "******",
  "websiteUrl": "https://netflix.com",
  "categoryId": 1,
  "categoryName": "Login",
  "folderId": 5,
  "folderName": "Entertainment",
  "isFavorite": true,
  "createdAt": "2024-03-15T10:00:00",
  "updatedAt": "2024-03-15T10:00:00"
}
```

### 2. Get All Entries
**GET** `/api/vault`

Retrieves all vault entries for the current user. Passwords and usernames are MASKED or omitted for security and performance.

**Response:** `200 OK`
```json
[
  {
    "id": 10,
    "title": "Netflix",
    "username": "******",
    "websiteUrl": "https://netflix.com",
    "categoryId": 1,
    "folderId": 5,
    "isFavorite": true
  },
  {
    "id": 11,
    "title": "Chase Bank",
    "username": "******",
    "websiteUrl": "https://chase.com",
    "categoryId": 2,
    "folderId": 2,
    "isFavorite": false
  }
]
```

### 3. Get Entry Details
**GET** `/api/vault/{id}`

Retrieves the **decrypted** details of a specific entry.
*Requires authenticated session.*

**Response:** `200 OK`
```json
{
  "id": 10,
  "title": "Netflix",
  "username": "myemail@example.com",
  "password": "superSecretPassword123!",
  "websiteUrl": "https://netflix.com",
  "notes": "Shared with family",
  "categoryId": 1,
  "categoryName": "Login",
  "folderId": 5,
  "folderName": "Entertainment",
  "isFavorite": true,
  "createdAt": "2024-03-15T10:00:00",
  "updatedAt": "2024-03-15T10:00:00"
}
```

### 4. Update Entry
**PUT** `/api/vault/{id}`

Updates an existing entry. All fields are optional; provides partial update behavior but typically sends full object.
*If password/username/notes are provided, they are re-encrypted.*

**Request Body:**
```json
{
  "title": "Netflix Premium",
  "password": "newPassword456"
}
```

**Response:** `200 OK` (Similar to Create Response)

### 5. Delete Entry
**DELETE** `/api/vault/{id}`

Permanently deletes a vault entry.

**Response:** `204 No Content`
