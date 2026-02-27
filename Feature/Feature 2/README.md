# 🔵 Feature 2: User Entity & Repository

## 🎯 What is This Feature?

Think of this as creating a **user profile system** for your password manager. Before users can store passwords, login, or do anything, we need to define:
- **What information we store about each user** (email, username, password hash)
- **How we save and retrieve users from the database**

This is the **foundation** - every other feature depends on it!

---

## 📁 Files in This Feature

| File | Simple Explanation |
|------|-------------------|
| `User.java` | The "blueprint" for a user - defines what data each user has |
| `UserRepository.java` | The "database helper" - finds, saves, and deletes users |
| `UserResponse.java` | A "safe version" of user data to send back to the client |
| `SecurityConfig.java` | The "security guard" - decides who can access what |

---

## 📄 File 1: User.java

### What Does It Do?
This file is like a **form template** for user information. It tells the database exactly what columns to create and what data type each field should be.

### Think of it like...
When you sign up for any website, they ask for your email, username, and password. This file defines those fields and how they're stored.

### Key Fields Explained

| Field | What It Stores | Why We Need It |
|-------|---------------|----------------|
| `id` | Unique number (1, 2, 3...) | Every user needs a unique ID |
| `email` | user@example.com | For account recovery & notifications |
| `username` | johndoe123 | What users type to login |
| `masterPasswordHash` | Scrambled password | We NEVER store actual passwords! |
| `salt` | Random text | Makes password hashing more secure |
| `is2faEnabled` | true/false | Is two-factor auth turned on? |
| `createdAt` | 2024-02-09 10:30:00 | When the account was created |

### Important Code

```java
@Entity  // This tells Java: "This class maps to a database table"
@Table(name = "users")  // The table will be called "users"
public class User {

    @Id  // This field is the unique identifier
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto-increment (1, 2, 3...)
    private Long id;

    @Column(nullable = false, unique = true)  // Can't be empty, must be unique
    private String email;

    @Column(name = "master_password_hash", nullable = false)
    private String masterPasswordHash;  // NEVER store plain passwords!

    private String salt;  // Random string to make password more secure
}
```

### 🔑 Key Concept: Why We Hash Passwords
```
User enters: "MyPassword123!"
     ↓
We store: "$2a$10$N9qo8uLOickgx2ZMRZoMy..."  ← This is a HASH
```
Even if hackers steal our database, they can't read the passwords!

---

## 📄 File 2: UserRepository.java

### What Does It Do?
This is your **database assistant**. Instead of writing complex SQL queries, you just call simple methods like `findByEmail("john@test.com")`.

### Think of it like...
A librarian who finds books for you. You say "find me a book by this author" and they handle all the searching.

### Key Methods Explained

| Method | What It Does | When To Use |
|--------|-------------|-------------|
| `findByUsername("john")` | Searches for user with that username | During login |
| `findByEmail("a@b.com")` | Searches for user with that email | During password reset |
| `existsByUsername("john")` | Checks if username is taken | During registration |
| `existsByEmail("a@b.com")` | Checks if email is taken | During registration |

### Important Code

```java
@Repository  // Tells Spring: "This handles database operations"
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring automatically creates the SQL for you!
    Optional<User> findByUsername(String username);
    // Generates: SELECT * FROM users WHERE username = ?

    Optional<User> findByEmail(String email);
    // Generates: SELECT * FROM users WHERE email = ?

    boolean existsByUsername(String username);
    // Generates: SELECT COUNT(*) > 0 FROM users WHERE username = ?
}
```

### 🪄 The Magic of Spring Data JPA
You just write the method name, and Spring creates the SQL automatically:
- `findByEmail` → `WHERE email = ?`
- `existsByUsername` → `WHERE username = ?`
- `findByDeletionScheduledAtBefore` → `WHERE deletion_scheduled_at < ?`

---

## 📄 File 3: UserResponse.java

### What Does It Do?
This is a **safe version** of user data. When someone asks "give me user info", we don't want to accidentally send the password hash!

### Think of it like...
If someone asks for your ID, you show them a photocopy without your social security number - only the safe parts.

### Important Code

```java
public class UserResponse {
    private Long id;           // ✅ Safe to share
    private String email;       // ✅ Safe to share
    private String username;    // ✅ Safe to share
    private boolean is2faEnabled;  // ✅ Safe to share
    private LocalDateTime createdAt;  // ✅ Safe to share
    
    // ❌ NO password hash
    // ❌ NO salt
    // ❌ NO sensitive data
}
```

---

## 📄 File 4: SecurityConfig.java

### What Does It Do?
This is the **security guard** of your application. It decides:
- Which pages/APIs anyone can access (public)
- Which pages/APIs require login (protected)

### Think of it like...
A nightclub bouncer:
- "Registration? Go right in, no ID needed."
- "View my passwords? Show me your JWT token first!"

### Key Configuration Explained

```java
@Configuration  // This is a configuration file
@EnableWebSecurity  // Turn on Spring Security
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            // Disable CSRF (we use JWT tokens instead)
            .csrf(AbstractHttpConfigurer::disable)
            
            // No sessions - we use JWT tokens
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Define who can access what
            .authorizeHttpRequests(auth -> auth
                // Anyone can access these (no login required)
                .requestMatchers("/api/auth/**").permitAll()    // Register, Login
                .requestMatchers("/api/health/**").permitAll()  // Health checks
                .requestMatchers("/swagger-ui/**").permitAll()  // API docs
                
                // Everything else needs authentication
                .anyRequest().authenticated())
                
            // Add our JWT filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // Use BCrypt for hashing
    }
}
```

### 🛡️ What's Protected vs Public

| Endpoint | Access | Why? |
|----------|--------|------|
| `/api/auth/register` | 🔓 Public | Users need to register without being logged in |
| `/api/auth/login` | 🔓 Public | Users need to login without being logged in |
| `/api/vault/**` | 🔒 Protected | Only logged-in users should see their passwords |
| `/api/users/**` | 🔒 Protected | Only logged-in users should manage their account |

---

## 🔄 How These Files Work Together

```
┌──────────────────────────────────────────────────────────────┐
│                    User Registration Flow                     │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  1. User submits: email, username, password                  │
│                        ↓                                     │
│  2. SecurityConfig: "/api/auth/register" is public ✓         │
│                        ↓                                     │
│  3. UserRepository.existsByEmail() - Check if email taken   │
│                        ↓                                     │
│  4. UserRepository.existsByUsername() - Check if name taken │
│                        ↓                                     │
│  5. Create User object (password is hashed)                  │
│                        ↓                                     │
│  6. UserRepository.save(user) - Save to database             │
│                        ↓                                     │
│  7. Return UserResponse (safe data only)                     │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

---

## ✅ Summary

| File | One-Line Summary |
|------|-----------------|
| `User.java` | Defines what user data we store in the database |
| `UserRepository.java` | Provides easy methods to find/save/delete users |
| `UserResponse.java` | Safe DTO that hides sensitive user data |
| `SecurityConfig.java` | Controls which endpoints need authentication |

This feature is the **foundation** - Features 3, 4, and 5 all build on top of it!
