# VaultTrashService: Explanation and Architecture

The `VaultTrashService` class is the central manager for the "Recycle Bin" functionality of the Password Manager application. When a user deletes a sensitive item (a `VaultEntry`) from their vault, the application does not immediately erase it from the database. Instead, it enters a 30-day "grace period" within this system.

## 1. Core Responsibilities

The `VaultTrashService` handles the following operations:
*   **Listing:** Fetching all entries currently sitting in a user's trash.
*   **Counting:** Returning the total number of items in a user's trash (often used for UI badges).
*   **Restoration:** Un-marking a trashed item, effectively moving it back to the active vault.
*   **Forced Deletion:** Allowing a user to manually empty their trash or permanently delete a specific item before the 30-day timer expires.
*   **Automated Cleanup:** Running the daily sweep to permanently erase items that have exceeded their 30-day retention period.

---

## 2. Connections to Other Files/Components

The `VaultTrashService` does not operate in isolation. It relies on a web of database entities, repositories, schedulers, and Data Transfer Objects (DTOs) to function.

### A. The Database Repositories
*   **`VaultTrashRepository.java`**: This is the most direct connection. `VaultTrashService` uses this repository definition to talk to the SQL database. It relies on custom methods defined here, such as `findByUserIdAndIsDeletedTrue()` and `findExpiredTrashEntries(LocalDateTime expiry)`, to specifically filter for "trashed" items.
*   **`UserRepository.java`**: The service uses this to quickly look up a `User` entity based on the provided `username` string from the security context, ensuring a user only manipulates their own trash.

### B. The Internal Entities (Backend Workers)
*   **`VaultEntry.java`**: This is the core raw data model. It represents a single row in the database. When an item is "trashed", this entity is retrieved, its `isDeleted` flag is flipped to `true`, and its `deletedAt` timestamp is recorded. It contains all encrypted sensitive information.
*   **`User.java`**: Connected to `VaultEntry`, representing the owner of the trashed item.

### C. The DTOs (Frontend Communicators)
*   **`TrashEntryResponse.java`**: Instead of sending the heavy, sensitive `VaultEntry` object to the Angular frontend, the `VaultTrashService` maps the needed data into this clean shipping container. 
    *   **The Connection:** The `mapToTrashResponse()` method builds this DTO. It safely extracts the `id`, `title`, and `websiteUrl`. It also performs "on-the-fly" math to calculate `expiresAt` (when the 30 days are up) and `daysRemaining` (the countdown), so the frontend doesn't have to parse complex dates.

### D. The Automated Scheduler (The Janitor)
*   **`TrashCleanupScheduler.java`**: This is a completely separate Spring `@Component` that acts as an independent background worker.
    *   **The Connection:** It contains a method annotated with `@Scheduled(cron = "0 0 0 * * ?")`. Every night at exactly midnight, this scheduler wakes up and directly calls `vaultTrashService.cleanupExpired()`. Without this scheduler, items would sit in the trash forever.

### E. The API Controller (The Entry point)
*   *(Likely `VaultController.java` or `VaultTrashController.java`)*: While not explicitly shown in the service file, Spring REST controllers serve as the entry points for HTTP requests (like `GET /api/vault/trash`). These controllers inject the `VaultTrashService` to handle the actual business logic requested by the user's browser.

---

## 3. Summary of the 30-Day Lifecycle

1.  **User Deletes Item:** The Controller calls the vault service. The `VaultEntry` flag `isDeleted` is set to `true`, and `deletedAt` is set to `LocalDateTime.now()`.
2.  **User Views Trash:** The Controller calls `VaultTrashService.getTrashEntries()`. The service fetches the raw items via the Repository, packs them into `TrashEntryResponse` DTOs (calculating the days remaining), and returns them.
3.  **Time Passes:** For up to 30 days, the item remains in this state. The user can call the `restoreEntry()` method to clear the flags and save it back to the active vault.
4.  **Midnight Cleanup:** Every day at `00:00:00`, `TrashCleanupScheduler` calls `VaultTrashService.cleanupExpired()`.
5.  **Execution:** The service looks for any entries where `deletedAt` is older than `TRASH_RETENTION_DAYS` (30 days). If found, it permanently deletes the row from the database using the repository.

---

## 4. Methods and Response Mapping

Here is a breakdown of every method inside `VaultTrashService`, what it does, and how it responds:

### `getTrashEntries(String username)`
*   **Purpose:** Fetches a list of all items currently in the user's recycle bin.
*   **Response Mapping:** Returns a `List<TrashEntryResponse>`. It iterates through the raw `VaultEntry` database rows and uses `mapToTrashResponse` to convert them into secure Frontend DTOs.

### `getTrashCount(String username)`
*   **Purpose:** Quickly calculates how many items are currently in the trash without loading all the data.
*   **Response Mapping:** Returns a primitive `long`. The frontend typically uses this to display a notification badge (e.g., a little red "3" next to the Trash icon).

### `restoreEntry(String username, Long entryId)`
*   **Purpose:** Takes a single item out of the trash and puts it back into the user's active vault. It does this by setting `isDeleted` back to `false` and clearing the `deletedAt` timestamp.
*   **Response Mapping:** Returns a single `TrashEntryResponse` of the restored item. This allows the frontend to confirm the exact item that was restored and confidently remove it from the local UI list.

### `restoreAll(String username)`
*   **Purpose:** A bulk-action method. It finds every trashed item for that user and restores all of them at once.
*   **Response Mapping:** `void` (no response). The frontend simply assumes success upon receiving an HTTP 200 OK and refreshes its state.

### `permanentDelete(String username, Long entryId)`
*   **Purpose:** Manually forcefully deletes a single vault item forever, bypassing the rest of the 30-day waiting period. This physically removes the row from the SQL database.
*   **Response Mapping:** `void` (no response).

### `emptyTrash(String username)`
*   **Purpose:** A bulk-action method for "Empty Recycle Bin". It immediately and permanently drops every item currently residing in the user's trash from the database.
*   **Response Mapping:** `void` (no response). 

### `cleanupExpired()`
*   **Purpose:** The automated janitor method. It is triggered by the `TrashCleanupScheduler`, not by a user directly. It calculates the 30-day cutoff and permanently deletes all expired items.
*   **Response Mapping:** `void` (It doesn't talk to the frontend at all; it only logs its actions to the server console).

### `mapToTrashResponse(VaultEntry entry)` (Private Helper)
*   **Purpose:** The central DTO mapping engine. It takes heavy, sensitive database objects and strips them down for the public API.
*   **Mapping Logic:**
    *   Exposes safe fields: `id`, `title`, `websiteUrl`, `categoryName`, `folderName`.
    *   Hides dangerous fields: `username`, `encryptedPassword`, `iv`.
    *   Calculates `expiresAt` (adding 30 days to the deletion date) and `daysRemaining` (math between now and expiration).
