# 🟡 Feature 4: User Registration

## 🎯 What is This Feature?

This feature handles **new user sign-up**. When someone wants to use the password manager, they need to create an account first. This feature:
- Collects email, username, and master password
- Validates everything is correct
- Securely stores the user in the database

---

## 🔄 Registration Flow (Simple)

```
User fills out form:
┌─────────────────────────┐
│ Email: john@test.com    │
│ Username: johndoe       │
│ Password: MySecure@123  │
│ [Register Button]       │
└───────────┬─────────────┘
            ↓
    ┌───────────────┐
    │ Is email      │──No──→ "Email already in use!"
    │ unique?       │
    └───────┬───────┘
            │ Yes
            ↓
    ┌───────────────┐
    │ Is username   │──No──→ "Username already taken!"
    │ unique?       │
    └───────┬───────┘
            │ Yes
            ↓
    ┌───────────────┐
    │ Is password   │──No──→ "Password too weak!"
    │ strong enough?│
    └───────┬───────┘
            │ Yes
            ↓
    ┌───────────────┐
    │ Hash password │
    │ Generate salt │
    │ Save to DB    │
    └───────┬───────┘
            ↓
    "Account created! ✓"
```

---

## 📁 Files in This Feature

| File | Simple Explanation |
|------|-------------------|
| `RegistrationRequest.java` | The "sign-up form" - what data users must provide |
| `RegistrationService.java` | The "processor" - validates and creates the account |
| `AuthController.java` | The "front door" - receives the HTTP request |
| `UserController.java` | The "account manager" - handles account deletion |

---

## 📄 File 1: RegistrationRequest.java

### What Does It Do?
This defines the **shape of the sign-up form**. What fields are required? What validation rules apply?

### Think of it like...
A paper form at a doctor's office. Some fields are marked with asterisks (*required) and have specific formats (email must look like an email).

### Fields & Validation

| Field | Rules | Error Message |
|-------|-------|---------------|
| `email` | Required, must be valid email format | "Email is required" / "Invalid email format" |
| `username` | Required, 3-50 characters | "Username must be between 3 and 50 characters" |
| `masterPassword` | Required | "Master password is required" |

### Important Code

```java
@Data  // Lombok: auto-generates getters/setters
public class RegistrationRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotBlank(message = "Master password is required")
    private String masterPassword;
}
```

### 🔍 What These Annotations Do

```java
@NotBlank   → Field cannot be null, empty, or just whitespace
@Email      → Must match email pattern (xxx@xxx.xxx)
@Size       → Length must be within specified range
```

---

## 📄 File 2: RegistrationService.java

### What Does It Do?
This is the **brain** of registration. It takes the request, validates everything, and creates the user account.

### Think of it like...
A bank employee who processes your new account application. They check your ID, verify you don't already have an account, and then set everything up.

### What Happens Step by Step

```
Step 1: Check if email is taken        → Throw error if yes
Step 2: Check if username is taken     → Throw error if yes
Step 3: Validate password strength     → Throw error if weak
Step 4: Generate random salt           → For encryption later
Step 5: Hash the password with BCrypt  → Secure storage
Step 6: Create User entity             → Prepare for database
Step 7: Save to database               → Persist the user
Step 8: Return safe response           → No password in response!
```

### Important Code

```java
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MasterPasswordValidator masterPasswordValidator;

    @Transactional  // If anything fails, rollback all changes
    public UserResponse registerUser(RegistrationRequest request) {
        
        // Step 1: Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthenticationException("Email is already in use");
        }
        
        // Step 2: Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthenticationException("Username is already taken");
        }

        // Step 3: Validate password strength
        if (!masterPasswordValidator.isValid(request.getMasterPassword())) {
            throw new AuthenticationException(
                "Weak master password: " + masterPasswordValidator.getRequirementsMessage()
            );
        }

        // Step 4: Generate a unique salt for this user
        String salt = UUID.randomUUID().toString();

        // Step 5 & 6: Create user with hashed password
        User newUser = User.builder()
            .email(request.getEmail())
            .username(request.getUsername())
            .masterPasswordHash(passwordEncoder.encode(request.getMasterPassword()))  // BCrypt hash
            .salt(salt)
            .is2faEnabled(false)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // Step 7: Save to database
        User savedUser = userRepository.save(newUser);

        // Step 8: Return safe response (no password hash!)
        return UserResponse.builder()
            .id(savedUser.getId())
            .email(savedUser.getEmail())
            .username(savedUser.getUsername())
            .is2faEnabled(savedUser.is2faEnabled())
            .createdAt(savedUser.getCreatedAt())
            .build();
    }
}
```

### 🔑 Key Concept: Password Hashing

```
User enters: "MySecure@Pass123"
                   ↓
         ┌─────────────────┐
         │ BCrypt Encoder  │
         └────────┬────────┘
                  ↓
Stored: "$2a$10$N9qo8uLOickgx2ZMRZoMyeKj9.3N6X8GJk9kY..."

Why BCrypt?
✅ One-way function (can't reverse it)
✅ Includes built-in salt
✅ Slow on purpose (prevents brute force)
```

---

## 📄 File 3: AuthController.java

### What Does It Do?
This is the **front door** of authentication. It receives HTTP requests and routes them to the right service.

### Think of it like...
A restaurant host who greets you at the door and directs you to the right table (register → RegistrationService, login → AuthenticationService).

### Endpoints

| Method | Endpoint | What It Does |
|--------|----------|--------------|
| POST | `/api/auth/register` | Create new account |
| POST | `/api/auth/login` | Login to existing account |

### Important Code

```java
@RestController  // This handles HTTP requests
@RequestMapping("/api/auth")  // Base path: /api/auth
@RequiredArgsConstructor
public class AuthController {

    private final RegistrationService registrationService;
    private final AuthenticationService authenticationService;

    // POST /api/auth/register
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegistrationRequest request) {
        UserResponse response = registrationService.registerUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);  // 201 Created
    }

    // POST /api/auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);  // 200 OK
    }
}
```

### 📨 Sample Request & Response

**Request: POST /api/auth/register**
```json
{
    "email": "john@example.com",
    "username": "johndoe",
    "masterPassword": "MySecure@Pass123"
}
```

**Response: 201 Created**
```json
{
    "id": 1,
    "email": "john@example.com",
    "username": "johndoe",
    "is2faEnabled": false,
    "createdAt": "2024-02-09T10:30:00"
}
```

---

## 📄 File 4: UserController.java

### What Does It Do?
Handles **account management** operations, specifically account deletion with a 30-day grace period.

### Think of it like...
HR department that handles employee resignations. "We'll process your resignation in 30 days. Changed your mind? Just let us know before then!"

### Endpoints

| Method | Endpoint | What It Does |
|--------|----------|--------------|
| DELETE | `/api/users/account` | Schedule account for deletion (30 days) |
| POST | `/api/users/account/cancel-deletion` | Cancel pending deletion |

### Important Code

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AccountDeletionService accountDeletionService;

    // DELETE /api/users/account
    @DeleteMapping("/account")
    public ResponseEntity<String> deleteAccount(@Valid @RequestBody AccountDeletionRequest request) {
        // Get current logged-in user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        // Schedule deletion (doesn't delete immediately!)
        accountDeletionService.scheduleAccountDeletion(username, request);
        
        return ResponseEntity.ok(
            "Account scheduled for deletion in 30 days. " +
            "You can cancel this action by logging in and using the cancel endpoint."
        );
    }

    // POST /api/users/account/cancel-deletion
    @PostMapping("/account/cancel-deletion")
    public ResponseEntity<String> cancelDeletion() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        accountDeletionService.cancelAccountDeletion(username);
        return ResponseEntity.ok("Account deletion cancelled.");
    }
}
```

### ⏰ Why 30-Day Grace Period?

```
Day 1: User clicks "Delete Account"
        ↓
    Account marked for deletion
    User can still login
        ↓
Day 2-29: User can cancel anytime
        ↓
Day 30: Account permanently deleted
        ↓
    All passwords, settings, data → GONE
    Cannot be recovered!
```

This prevents accidental deletions and allows users to change their mind.

---

## 🔄 How These Files Work Together

```
┌──────────────────────────────────────────────────────────────┐
│                    Registration Flow                          │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Browser/App sends POST to /api/auth/register                │
│                        ↓                                     │
│  ┌─────────────────────────────────────────┐                │
│  │ AuthController.java                     │                │
│  │ - Receives request                      │                │
│  │ - @Valid triggers validation            │                │
│  │ - Calls RegistrationService             │                │
│  └───────────────────────┬─────────────────┘                │
│                          ↓                                   │
│  ┌─────────────────────────────────────────┐                │
│  │ RegistrationRequest.java                │                │
│  │ - Validates email format                │                │
│  │ - Validates username length             │                │
│  │ - Validates password not blank          │                │
│  └───────────────────────┬─────────────────┘                │
│                          ↓                                   │
│  ┌─────────────────────────────────────────┐                │
│  │ RegistrationService.java                │                │
│  │ - Checks email uniqueness               │                │
│  │ - Checks username uniqueness            │                │
│  │ - Validates password strength           │                │
│  │ - Generates salt                        │                │
│  │ - Hashes password                       │                │
│  │ - Saves user                            │                │
│  │ - Returns UserResponse                  │                │
│  └───────────────────────┬─────────────────┘                │
│                          ↓                                   │
│  Response: 201 Created with user data                        │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## ✅ Summary

| File | One-Line Summary |
|------|-----------------|
| `RegistrationRequest.java` | Defines and validates sign-up form data |
| `RegistrationService.java` | Processes registration: validates, hashes, saves |
| `AuthController.java` | REST endpoint that receives register/login requests |
| `UserController.java` | REST endpoint for account deletion management |

After registration, users can login using Features 5! 🔐
