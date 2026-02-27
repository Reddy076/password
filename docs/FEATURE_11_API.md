# 📂 Feature 11: Folders API Documentation

Base URL: `http://localhost:8080/api/folders`

## Security & Authorization
*   **Authentication**: `Bearer Token` (JWT) is **REQUIRED** for all endpoints.
*   **Authorization**: Access is strictly scoped to the authenticated user.
    *   The API automatically extracts the `username` from the JWT token.
    *   You can only view, update, or delete folders that **belong to you**.
    *   Attempting to access another user's folder ID will result in a `404 Not Found` (Resource Not Found) or `403 Forbidden`.


## 1. Create a Folder
**POST** `/api/folders`
*   **Params**:
    *   `name` (string): Name of the folder (e.g., "Work")
    *   `parentFolderId` (optional, long): ID of the parent folder to nest this under.
*   **Example 1 (Root Folder)**: `POST /api/folders?name=Finance`
*   **Example 2 (Subfolder)**: `POST /api/folders?name=Taxes&parentFolderId=1`
    *   *Creates "Taxes" inside the folder with ID 1.*

## 2. Get All Folders
**GET** `/api/folders`
*   **Description**: Returns a hierarchical list of all your folders.
*   **Response**:
    ```json
    [
      {
        "id": 1,
        "name": "Finance",
        "subfolders": []
      },
      {
        "id": 2,
        "name": "Social Media",
        "subfolders": [
             { "id": 3, "name": "Work Accounts", ... }
        ]
      }
    ]
    ```

## 3. Get Folder by ID
**GET** `/api/folders/{id}`
*   **Example**: `GET /api/folders/1`

## 4. Rename a Folder
**PUT** `/api/folders/{id}`
*   **Params**:
    *   `name` (string): New name for the folder.
*   **Example**: `PUT /api/folders/1?name=My%20Finances`

## 5. Move a Folder
**PUT** `/api/folders/{id}/move`
*   **Params**:
    *   `parentId` (optional, long): New parent folder ID. Omit to move to root.
*   **Example**: `PUT /api/folders/3/move?parentId=1` (Moves folder 3 inside folder 1)
*   *Note:* You cannot move a folder into itself or its own subfolders.

## 6. Delete a Folder
**DELETE** `/api/folders/{id}`
*   **Description**: Deletes the folder and cascades to delete all subfolders.
*   **Example**: `DELETE /api/folders/1`
