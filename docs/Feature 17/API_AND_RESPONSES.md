# Feature 17: Trash & Restore — API Reference

## Endpoints

### `GET /api/vault/trash`
List all trashed entries for the current user.

**Authentication:** Required (JWT Bearer Token)

**Response: `200 OK`**
```json
[
  {
    "id": 1,
    "title": "Old Google Account",
    "websiteUrl": "https://accounts.google.com",
    "categoryName": "Email",
    "folderName": "Personal",
    "deletedAt": "2026-02-01T10:30:00",
    "expiresAt": "2026-03-03T10:30:00",
    "daysRemaining": 19
  }
]
```

---

### `GET /api/vault/trash/count`
Get the number of trashed entries.

**Response: `200 OK`**
```json
{ "count": 3 }
```

---

### `POST /api/vault/trash/{id}/restore`
Restore a single entry from trash.

**Response: `200 OK`** — returns the restored entry as `TrashEntryResponse`

---

### `POST /api/vault/trash/restore-all`
Restore all trashed entries.

**Response: `204 No Content`**

---

### `DELETE /api/vault/trash/{id}`
Permanently delete a single trashed entry.

**Response: `204 No Content`**

---

### `DELETE /api/vault/trash/empty`
Permanently delete all trashed entries (empty trash).

**Response: `204 No Content`**

---

## Soft-Delete Behavior

- `DELETE /api/vault/{id}` now performs **soft delete** (moves to trash) instead of permanent deletion
- `POST /api/vault/entries/bulk-delete` also performs soft delete
- Trashed entries are excluded from all regular queries (`GET /api/vault`, search, favorites, etc.)
- Entries in trash auto-expire after **30 days** via the `TrashCleanupScheduler`
