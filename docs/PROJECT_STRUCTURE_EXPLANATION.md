# Rev-PasswordManager Project Overview

## 🎯 Project Goal
The goal of **Rev-PasswordManager** is to build a secure, self-hosted password management system similar to Bitwarden or 1Password. It allows users to store sensitive information (like logins, credit cards, and secure notes) in an encrypted "Vault" that only they can unlock with their Master Password.

## 🏗️ Core Structure & Hierarchy

To organize data effectively, the application uses three main concepts: **Categories**, **Folders**, and **Vault Items**.

### 1. Categories (The "Type")
*   **Purpose**: Defines *what kind* of data is being stored.
*   **Examples**: `Login`, `Secure Note`, `Credit Card`, `Identity`.
*   **Behavior**:
    *   Categories are a **flat list**.
    *   They determine which fields are shown (e.g., a "Credit Card" category has fields for Number and CVV, while "Login" has Username and Password).
    *   Every item **must** belong to one Category.

### 2. Folders (The "Location")
*   **Purpose**: Defines *where* the item is organized user's personal structure.
*   **Examples**: `Work`, `Personal`, `Social Media`, `Finance`.
*   **Behavior**:
    *   Folders are **hierarchical** (like folders on your computer). You can have a `Work` folder with a `Projects` subfolder inside it.
    *   Folders are optional. An item doesn't *have* to be in a folder (it can just be in "All Items").

### 3. Vault Items (The "Data")
*   **Purpose**: These are the actual secrets you want to protect.
*   **Examples**: "Netflix Login", "Chase Bank Card", "WiFi Password".
*   **Behavior**:
    *   This is the core entity that holds your encrypted data (password, username, notes).
    *   An item links to **One Category** (Type) and **One Folder** (Location).

---

## 🔗 How It All Fits Together

Think of it like a physical file cabinet:

*   **The Cabinet** is your **Vault**.
*   **The Drawers** are your **Folders** (Personal, Work).
*   **The Colored Labels** on files are your **Categories** (Red for Bills, Blue for Contracts).
*   **The Papers** inside are your **Vault Items** (The actual information).

### Hierarchy Visualization
```mermaid
graph TD
    User[User] --> Vault[Encrypted Vault]
    Vault --> Folders[Folders (Organization)]
    Vault --> Categories[Categories (Types)]
    Vault --> Items[Vault Items (Passwords)]

    Folders --> Work[Folder: Work]
    Folders --> Personal[Folder: Personal]
    
    Categories --> Login[Type: Login]
    Categories --> Card[Type: Card]

    Items --> Item1[Item: GitHub Login]
    Item1 -.-> Work
    Item1 -.-> Login

    Items --> Item2[Item: Netflix]
    Item2 -.-> Personal
    Item2 -.-> Login
```

---

## 📝 The "Add Password" Process

When a user adds a password to the vault (Feature 13 - Vault Items), the process flows like this:

1.  **User acts**: Clicks "Add Item".
2.  **Select Category**: User chooses "Login" (Type).
3.  **Enter Data**: User fills in `Username`, `Password`, `Website URL`.
4.  **Select Folder**: User chooses "Personal / Social Media" (Location). *This is where the user "puts" the password in a folder.*
5.  **Encryption (Behind the Scenes)**:
    *   The application takes the sensitive fields (Password, Username).
    *   It encrypts them using the user's **Data Key** (AES-256).
    *   The server **never** sees the plain text password.
6.  **Save**: The encrypted data is sent to the API and stored in the database.

## 🚀 Current Project Status

We are building this system in phases:

1.  **Phase 1 & 2 (Complete)**: Users can register, login, and secure their account with 2FA.
2.  **Phase 3 (Current)**:
    *   **Feature 10 (Categories)**: ✅ Implemented. We have the "types".
    *   **Feature 11 (Folders)**: ✅ Implemented. We have the "structure".
    *   **Feature 12 (Generator)**: ✅ Implemented. We can generate strong passwords.
    *   **Feature 13 (Vault Items)**: 🚧 **NEXT STEP**. This is where we actually create the `VaultEntry` entity to store the passwords and link them to Categories and Folders.
