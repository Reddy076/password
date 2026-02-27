# 📡 Feature 10: Categories - API Documentation

## Base URL
```
http://localhost:8080/api/categories
```

## Authentication Required
All endpoints require a valid JWT token in the `Authorization` header:
```
Authorization: Bearer <your_jwt_token>
```

---

# 🔗 API Endpoints

## 1. Get All Categories

**GET** `/api/categories`

### Description
Retrieves all categories for the authenticated user, including default system categories.

### Request
```http
GET http://localhost:8080/api/categories
Authorization: Bearer <jwt_token>
```

### Response (200 OK)
```json
[
  {
    "id": 1,
    "name": "Social Media",
    "icon": "social-icon",
    "isDefault": true,
    "createdAt": "2026-02-09T11:50:00",
    "entryCount": 0
  },
  {
    "id": 2,
    "name": "Banking",
    "icon": "bank-icon",
    "isDefault": true,
    "createdAt": "2026-02-09T11:50:00",
    "entryCount": 0
  },
  {
    "id": 5,
    "name": "My Custom Category",
    "icon": "custom-icon",
    "isDefault": false,
    "createdAt": "2026-02-09T12:00:00",
    "entryCount": 0
  }
]
```

---

## 2. Get Category by ID

**GET** `/api/categories/{id}`

### Description
Retrieves a specific category by its ID.

### Request
```http
GET http://localhost:8080/api/categories/1
Authorization: Bearer <jwt_token>
```

### Response (200 OK)
```json
{
  "id": 1,
  "name": "Social Media",
  "icon": "social-icon",
  "isDefault": true,
  "createdAt": "2026-02-09T11:50:00",
  "entryCount": 0
}
```

### Error Response (404 Not Found)
```json
{
  "message": "Category not found with id: 999",
  "status": 404
}
```

---

## 3. Create Category

**POST** `/api/categories`

### Description
Creates a new custom category for the authenticated user.

### Request
```http
POST http://localhost:8080/api/categories
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "name": "Gaming",
  "icon": "game-controller"
}
```

### Request Body Schema
| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `name` | string | ✅ Yes | 1-100 characters |
| `icon` | string | ❌ No | Max 50 characters |

### Response (201 Created)
```json
{
  "id": 6,
  "name": "Gaming",
  "icon": "game-controller",
  "isDefault": false,
  "createdAt": "2026-02-09T12:05:00",
  "entryCount": 0
}
```

### Error Response (400 Bad Request - Duplicate)
```json
{
  "message": "Category with name 'Gaming' already exists",
  "status": 400
}
```

### Error Response (400 Bad Request - Validation)
```json
{
  "message": "Validation failed",
  "errors": [
    {
      "field": "name",
      "message": "Category name is required"
    }
  ]
}
```

---

## 4. Update Category

**PUT** `/api/categories/{id}`

### Description
Updates an existing custom category. Default categories cannot be modified.

### Request
```http
PUT http://localhost:8080/api/categories/6
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "name": "Video Games",
  "icon": "joystick"
}
```

### Response (200 OK)
```json
{
  "id": 6,
  "name": "Video Games",
  "icon": "joystick",
  "isDefault": false,
  "createdAt": "2026-02-09T12:05:00",
  "entryCount": 0
}
```

### Error Response (400 Bad Request - Default Category)
```json
{
  "message": "Cannot modify default categories",
  "status": 400
}
```

---

## 5. Delete Category

**DELETE** `/api/categories/{id}`

### Description
Deletes a custom category. Default categories cannot be deleted. Vault entries in this category will have their category set to null.

### Request
```http
DELETE http://localhost:8080/api/categories/6
Authorization: Bearer <jwt_token>
```

### Response (204 No Content)
*No response body*

### Error Response (400 Bad Request - Default Category)
```json
{
  "message": "Cannot delete default categories",
  "status": 400
}
```

### Error Response (404 Not Found)
```json
{
  "message": "Category not found with id: 999",
  "status": 404
}
```

---

## 6. Get Entries in Category

**GET** `/api/categories/{id}/entries`

### Description
Gets all vault entries in a specific category. (Placeholder - will be fully implemented with Feature 12)

### Request
```http
GET http://localhost:8080/api/categories/1/entries
Authorization: Bearer <jwt_token>
```

### Response (200 OK)
```json
"This endpoint will return vault entries when the Vault feature is implemented"
```

---

# 📊 Response Codes Summary

| Code | Description |
|------|-------------|
| 200 | Success |
| 201 | Created |
| 204 | No Content (Delete success) |
| 400 | Bad Request (Validation error or business rule violation) |
| 401 | Unauthorized (Invalid or missing JWT) |
| 404 | Not Found |

---

# 🧪 Postman Testing Guide

## Step 1: Get JWT Token First

Before testing category endpoints, you need to login to get a JWT token.

**Login Request:**
```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "identifier": "testuser@example.com",
  "masterPassword": "YourSecurePassword123!"
}
```

**Copy the `accessToken` from the response to use in subsequent requests.**

---

## Step 2: Test Category Endpoints

### Test 1: Create a Category
```
Method: POST
URL: http://localhost:8080/api/categories
Headers:
  - Authorization: Bearer <paste_your_token>
  - Content-Type: application/json
Body (raw JSON):
{
  "name": "Social Media",
  "icon": "users"
}
```

### Test 2: Get All Categories
```
Method: GET
URL: http://localhost:8080/api/categories
Headers:
  - Authorization: Bearer <paste_your_token>
```

### Test 3: Get Single Category
```
Method: GET
URL: http://localhost:8080/api/categories/1
Headers:
  - Authorization: Bearer <paste_your_token>
```

### Test 4: Update a Category
```
Method: PUT
URL: http://localhost:8080/api/categories/1
Headers:
  - Authorization: Bearer <paste_your_token>
  - Content-Type: application/json
Body (raw JSON):
{
  "name": "Social Networks",
  "icon": "network"
}
```

### Test 5: Delete a Category
```
Method: DELETE
URL: http://localhost:8080/api/categories/1
Headers:
  - Authorization: Bearer <paste_your_token>
```

---

# 📋 Sample Test Data

## Categories to Create

```json
// Category 1: Social Media
{
  "name": "Social Media",
  "icon": "users"
}

// Category 2: Banking
{
  "name": "Banking",
  "icon": "bank"
}

// Category 3: Email
{
  "name": "Email",
  "icon": "envelope"
}

// Category 4: Shopping
{
  "name": "Shopping",
  "icon": "shopping-cart"
}

// Category 5: Entertainment
{
  "name": "Entertainment",
  "icon": "play"
}

// Category 6: Work
{
  "name": "Work",
  "icon": "briefcase"
}

// Category 7: Gaming
{
  "name": "Gaming",
  "icon": "gamepad"
}

// Category 8: Travel
{
  "name": "Travel",
  "icon": "plane"
}

// Category 9: Education
{
  "name": "Education",
  "icon": "graduation-cap"
}

// Category 10: Health
{
  "name": "Health",
  "icon": "heart"
}
```

---

# 🔄 Postman Collection Import

Save this as `Feature10_Categories.postman_collection.json` and import into Postman:

```json
{
  "info": {
    "name": "Feature 10 - Categories API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080"
    },
    {
      "key": "token",
      "value": "YOUR_JWT_TOKEN_HERE"
    }
  ],
  "item": [
    {
      "name": "1. Login (Get Token)",
      "request": {
        "method": "POST",
        "header": [
          {"key": "Content-Type", "value": "application/json"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"identifier\": \"testuser@example.com\",\n  \"masterPassword\": \"YourSecurePassword123!\"\n}"
        },
        "url": "{{baseUrl}}/api/auth/login"
      }
    },
    {
      "name": "2. Get All Categories",
      "request": {
        "method": "GET",
        "header": [
          {"key": "Authorization", "value": "Bearer {{token}}"}
        ],
        "url": "{{baseUrl}}/api/categories"
      }
    },
    {
      "name": "3. Get Category by ID",
      "request": {
        "method": "GET",
        "header": [
          {"key": "Authorization", "value": "Bearer {{token}}"}
        ],
        "url": "{{baseUrl}}/api/categories/1"
      }
    },
    {
      "name": "4. Create Category",
      "request": {
        "method": "POST",
        "header": [
          {"key": "Authorization", "value": "Bearer {{token}}"},
          {"key": "Content-Type", "value": "application/json"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"name\": \"Gaming\",\n  \"icon\": \"gamepad\"\n}"
        },
        "url": "{{baseUrl}}/api/categories"
      }
    },
    {
      "name": "5. Update Category",
      "request": {
        "method": "PUT",
        "header": [
          {"key": "Authorization", "value": "Bearer {{token}}"},
          {"key": "Content-Type", "value": "application/json"}
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"name\": \"Video Games\",\n  \"icon\": \"joystick\"\n}"
        },
        "url": "{{baseUrl}}/api/categories/1"
      }
    },
    {
      "name": "6. Delete Category",
      "request": {
        "method": "DELETE",
        "header": [
          {"key": "Authorization", "value": "Bearer {{token}}"}
        ],
        "url": "{{baseUrl}}/api/categories/1"
      }
    },
    {
      "name": "7. Get Entries in Category",
      "request": {
        "method": "GET",
        "header": [
          {"key": "Authorization", "value": "Bearer {{token}}"}
        ],
        "url": "{{baseUrl}}/api/categories/1/entries"
      }
    }
  ]
}
```

---

# ✅ Expected Test Flow

1. **Login** → Get JWT token
2. **Create** → POST multiple categories
3. **Read All** → GET all categories (verify created ones appear)
4. **Read One** → GET specific category by ID
5. **Update** → PUT to modify a category
6. **Read Updated** → GET to verify update worked
7. **Delete** → DELETE a category
8. **Verify Deleted** → GET should return 404
