# Features 10 and 11: Categories vs. Folders

This document explains the conceptual difference and relationship between **Feature 10 (Categories)** and **Feature 11 (Folders)** in the Rev-PasswordManager application.

## 1. Core Differences

| Feature | Concept | Structure | Purpose | Example |
| :--- | :--- | :--- | :--- | :--- |
| **Feature 10: Categories** | **"What is it?"** | **Flat** (List) | Defines the **type** of item. | Login, Secure Note, Credit Card, Identity |
| **Feature 11: Folders** | **"Where is it?"** | **Hierarchical** (Tree) | Defines the **organization** or location to group items. | Work, Personal, Social Media, Finance/Banking |

## 2. In-Depth Explanation

### Feature 10: Categories (The "Type")
*   **Definition**: Categories represent the *template* or *schema* of the data being stored.
*   **Behavior**:
    *   They are typically a flat list (no sub-categories).
    *   They often dictate what fields are available (e.g., a "Credit Card" category has fields for Number, CVV, Expiry, while a "Login" category has Username, Password, URL).
    *   Every Vault Item usually belongs to **exactly one** Category.
*   **User Question**: *"What kind of data am I storing?"*
*   **System Usage**: Used for filtering items by type (e.g., "Show me all my Credit Cards").

### Feature 11: Folders (The "Organization")
*   **Definition**: Folders represent a user-defined organizational structure to keep the vault tidy.
*   **Behavior**:
    *   They are hierarchical (Folders can contain Subfolders).
    *   They work like the file system on your computer.
    *   A Vault Item can be placed inside a specific Folder.
*   **User Question**: *"Where did I put that item?"*
*   **System Usage**: Used for browsing and grouping related items (e.g., "Show me everything related to my Job").

## 3. The Relationship (How they work together)

When Feature 12 (Vault Items) is implemented, an Item will likely link to **both**:

1.  **Category ID**: To know *how* to display the item (Type).
2.  **Folder ID**: To know *where* to display the item in the navigation tree (Location).

### Example Scenario
Imagine you have a **Work Email Login**.

*   **Category**: `Login` (Because it has a username/password).
*   **Folder**: `Work/Emails` (Because that's where you want to find it).

You can search for it in two ways:
1.  **By Category**: Click "Logins" -> See all logins (Work Email, Netflix, Amazon).
2.  **By Folder**: Click "Work" -> "Emails" -> See all work emails.

## 4. Technical Implementation

*   **Category Entity**: Independent. Linked to User (for custom categories) or System (for default ones).
*   **Folder Entity**: Recursive (Parent ID). Linked to User.
*   **Vault Item (Future)**:
    ```java
    class VaultItem {
        Category category; // Many-to-One
        Folder folder;     // Many-to-One (Optional, null means "root")
    }
    ```
