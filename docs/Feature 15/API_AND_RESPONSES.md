# Feature 15: Favorites API & Postman Guide

## Base URL
`http://localhost:8080`

## Authentication
All endpoints require a valid JWT token in the Authorization header.
`Authorization: Bearer <your_jwt_token>`

---

## 1. Toggle Favorite Status
**Endpoint:** `PUT /api/vault/{id}/favorite`
**Description:** Toggle the 'isFavorite' status of a vault entry.

### Request
**Method:** PUT
**URL:** `{{base_url}}/api/vault/1/favorite`
**Path Variables:**
*   `id`: ID of the vault entry (e.g., 1)

### Response (200 OK)
```json
{
    "id": 1,
    "title": "My Favorite Entry",
    "username": "user1",
    "url": "https://example.com",
    "isFavorite": true,
    "folderId": 2,
    "createdAt": "2024-01-01T12:00:00",
    "updatedAt": "2024-01-02T12:00:00"
}
```

---

## 2. Get All Favorites
**Endpoint:** `GET /api/vault/favorites`
**Description:** Retrieve all vault entries marked as favorites.

### Request
**Method:** GET
**URL:** `{{base_url}}/api/vault/favorites`

### Response (200 OK)
```json
[
    {
        "id": 1,
        "title": "My Favorite Entry",
        "username": "user1",
        "url": "https://example.com",
        "isFavorite": true,
        "folderId": 2,
        "createdAt": "2024-01-01T12:00:00",
        "updatedAt": "2024-01-02T12:00:00"
    },
    {
        "id": 5,
        "title": "Another Favorite",
        "username": "user1",
        "url": "https://service.com",
        "isFavorite": true,
        "folderId": null,
        "createdAt": "2024-01-03T10:00:00",
        "updatedAt": "2024-01-03T10:00:00"
    }
]
```
