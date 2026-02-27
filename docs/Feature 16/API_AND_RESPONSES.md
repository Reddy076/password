# Feature 16: Search & Filter — API Reference

## Endpoint

### `GET /api/vault/search`
Search and filter vault entries by keyword, category, folder, favorites, and sensitivity.

**Authentication:** Required (JWT Bearer Token)

### Query Parameters

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| `keyword` | String | No | – | Search in title and website URL (case-insensitive) |
| `categoryId` | Long | No | – | Filter by category ID |
| `folderId` | Long | No | – | Filter by folder ID |
| `isFavorite` | Boolean | No | – | Filter by favorite status |
| `isHighlySensitive` | Boolean | No | – | Filter by sensitivity flag |
| `sortBy` | String | No | `title` | Sort field: `title`, `createdAt`, `updatedAt` |
| `sortDir` | String | No | `asc` | Sort direction: `asc` or `desc` |

### Response: `200 OK`

```json
[
  {
    "id": 1,
    "title": "Google Account",
    "username": "******",
    "websiteUrl": "https://accounts.google.com",
    "categoryId": 2,
    "categoryName": "Email",
    "folderId": 1,
    "folderName": "Personal",
    "isFavorite": true,
    "createdAt": "2026-01-15T10:30:00",
    "updatedAt": "2026-02-10T14:20:00"
  }
]
```

### Example Requests

```
# Search by keyword
GET /api/vault/search?keyword=google

# Filter by category
GET /api/vault/search?categoryId=2

# Favorites only, sorted by newest
GET /api/vault/search?isFavorite=true&sortBy=createdAt&sortDir=desc

# Combined search + filter
GET /api/vault/search?keyword=bank&categoryId=3&isFavorite=true&sortBy=createdAt&sortDir=desc

# All entries (no filters)
GET /api/vault/search
```

### Notes
- `username` and `notes` are encrypted in the database and **cannot** be searched via keyword
- All parameters are optional — omitting all returns all entries sorted by title ascending
- An empty result returns `200 OK` with `[]`
