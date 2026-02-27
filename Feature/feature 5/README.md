# 🔴 Feature 5: User Login (Authentication)

## 🎯 What is This Feature?

This feature handles **user login**. When a registered user wants to access their passwords, they need to prove who they are. This feature:
- Verifies username/email and password
- Generates **JWT tokens** for session management
- Protects all subsequent API calls

---

## 🔑 What is JWT (JSON Web Token)?

Think of JWT like a **wristband at a concert**:
1. You show your ticket at the entrance (login with username/password)
2. You get a wristband (JWT token)
3. Now you can access VIP areas just by showing the wristband (no need to show ticket again)

```
Login successful → Get JWT token → Use token for all future requests

Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huZG9lIiwia...
       \_____________/ \____________________/ \_________________/
         Header           Payload (data)        Signature
```

---

## 📁 Files in This Feature

| File | Simple Explanation |
|------|-------------------|
| `LoginRequest.java` | The "login form" - username + password |
| `AuthResponse.java` | The "response package" - contains JWT tokens |
| `AuthenticationService.java` | The "verifier" - checks credentials and creates tokens |
| `JwtTokenProvider.java` | The "token factory" - generates and validates JWT |
| `JwtAuthenticationFilter.java` | The "security guard" - checks token on every request |
| `CustomUserDetailsService.java` | The "user finder" - loads user for Spring Security |
| `SecurityConfig.java` | The "rulebook" - configures which endpoints need auth |
| `AuthController.java` | The "front door" - REST endpoint for login |

---

## 📄 File 1: LoginRequest.java

### What Does It Do?
Defines what users need to provide to login - just **username and password**.

### Important Code

```java
@Data
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;  // Can also be email!

    @NotBlank(message = "Master password is required")
    private String masterPassword;
}
```

### 📨 Sample Login Request
```json
{
    "username": "johndoe",        // OR "john@example.com"
    "masterPassword": "MySecure@Pass123"
}
```

---

## 📄 File 2: AuthResponse.java

### What Does It Do?
The **response package** after successful login. Contains:
- Access Token (short-lived, for API calls)
- Refresh Token (long-lived, to get new access tokens)
- User info

### Important Code

```java
@Data
@Builder
public class AuthResponse {
    private String accessToken;    // Use this for API calls
    private String refreshToken;   // Use to get new access token
    private String tokenType;      // Always "Bearer"
    private UserResponse user;     // User details
}
```

### 📨 Sample Login Response
```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "user": {
        "id": 1,
        "email": "john@example.com",
        "username": "johndoe",
        "is2faEnabled": false
    }
}
```

### ⏰ Token Lifetimes

| Token | Lifetime | Purpose |
|-------|----------|---------|
| Access Token | 15 min - 1 hour | Used for API calls |
| Refresh Token | Days - Weeks | Get new access token without re-login |

---

## 📄 File 3: AuthenticationService.java

### What Does It Do?
The **brain of login**. It verifies credentials and generates tokens.

### Login Flow

```
User: "johndoe" + "MySecure@Pass123"
            ↓
   ┌────────────────────┐
   │ Spring Security    │
   │ AuthenticationMgr  │ ← Verifies password with BCrypt
   └─────────┬──────────┘
             ↓
   Password correct? ──No──→ "Invalid username or password"
             │
             │ Yes
             ↓
   ┌────────────────────┐
   │ Find user in DB    │ ← Get user details
   └─────────┬──────────┘
             ↓
   ┌────────────────────┐
   │ Generate tokens    │ ← Access + Refresh tokens
   └─────────┬──────────┘
             ↓
   Return AuthResponse with tokens ✓
```

### Important Code

```java
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public AuthResponse login(LoginRequest request) {
        try {
            // Step 1: Authenticate with Spring Security
            // This checks if password matches the BCrypt hash in database
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getMasterPassword()
                )
            );

            // Step 2: Find user (supports both username and email)
            User user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElseThrow(() -> new AuthenticationException("User not found"));

            // Step 3: Generate tokens
            String accessToken = jwtTokenProvider.generateAccessToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

            // Step 4: Build response
            UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .is2faEnabled(user.is2faEnabled())
                .createdAt(user.getCreatedAt())
                .build();

            return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(userResponse)
                .build();

        } catch (org.springframework.security.core.AuthenticationException e) {
            throw new AuthenticationException("Invalid username or password");
        }
    }
}
```

---

## 📄 File 4: JwtTokenProvider.java

### What Does It Do?
The **token factory**. Creates new tokens and validates existing ones.

### Think of it like...
A government agency that issues passports (creates tokens) and also verifies them at airports (validates tokens).

### Key Methods

| Method | What It Does |
|--------|-------------|
| `generateAccessToken()` | Creates short-lived access token |
| `generateRefreshToken()` | Creates long-lived refresh token |
| `getUsernameFromToken()` | Extracts username from token |
| `validateToken()` | Checks if token is valid and not expired |

### Important Code - Token Generation

```java
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    public String generateAccessToken(Authentication authentication) {
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        return generateToken(new HashMap<>(), userPrincipal, jwtConfig.getAccessTokenExpiration());
    }

    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
            .claims(extraClaims)                                      // Extra data (optional)
            .subject(userDetails.getUsername())                       // Who this token is for
            .issuedAt(new Date(System.currentTimeMillis()))          // When created
            .expiration(new Date(System.currentTimeMillis() + expiration))  // When it expires
            .signWith(getSignInKey())                                 // Sign with secret key
            .compact();                                               // Build the token
    }
}
```

### Important Code - Token Validation

```java
public boolean validateToken(String token) {
    try {
        Jwts.parser()
            .verifyWith(getSignInKey())    // Use same key that signed it
            .build()
            .parseSignedClaims(token);     // Parse and verify
        return true;
    } catch (MalformedJwtException ex) {
        // Invalid JWT token format
    } catch (ExpiredJwtException ex) {
        // Token has expired
    } catch (UnsupportedJwtException ex) {
        // JWT format not supported
    } catch (IllegalArgumentException ex) {
        // Token is empty
    }
    return false;
}
```

### 🔍 JWT Token Structure

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJqb2huZG9lIiwiaWF0IjoxNjE2MjM5MDIyLCJleHAiOjE2MTYyNDI2MjJ9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c

Part 1 (Header):     eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9
Decoded: {"alg":"HS256","typ":"JWT"}

Part 2 (Payload):    eyJzdWIiOiJqb2huZG9lIiwiaWF0IjoxNjE2MjM5MDIyLCJleHAiOjE2MTYyNDI2MjJ9
Decoded: {"sub":"johndoe","iat":1616239022,"exp":1616242622}
         subject (username), issued at, expires at

Part 3 (Signature):  SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
This proves the token wasn't tampered with
```

---

## 📄 File 5: JwtAuthenticationFilter.java

### What Does It Do?
The **security guard** that checks every incoming request. "Do you have a valid wristband?"

### Think of it like...
Security at a building entrance. They check your badge on EVERY entry, not just the first time.

### Filter Flow

```
Incoming Request: GET /api/vault/entries
                        ↓
    ┌───────────────────────────────────────┐
    │ Check "Authorization" header          │
    └───────────────────┬───────────────────┘
                        ↓
    Header exists and starts with "Bearer "?
                        ↓
         No ──────────────────────── Yes
          │                           │
          ↓                           ↓
    Continue without auth    Extract token from header
    (Will fail on protected   "Bearer xyz..." → "xyz..."
     endpoints)                       │
                                     ↓
                         ┌───────────────────────┐
                         │ Token valid?          │
                         └───────────┬───────────┘
                                     ↓
                              No ─────── Yes
                               │          │
                               ↓          ↓
                         Continue    Extract username
                         without     Load user from DB
                         auth        Set in SecurityContext
                                           │
                                           ↓
                                     Continue to
                                     Controller ✓
```

### Important Code

```java
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        
        // Step 1: Get Authorization header
        final String authHeader = request.getHeader("Authorization");

        // Step 2: Check if header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);  // Continue without authentication
            return;
        }

        // Step 3: Extract token (remove "Bearer " prefix)
        String jwt = authHeader.substring(7);
        
        // Step 4: Validate token
        if (jwtTokenProvider.validateToken(jwt)) {
            // Step 5: Extract username from token
            String username = jwtTokenProvider.getUsernameFromToken(jwt);

            // Step 6: Load user details
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // Step 7: Set authentication in security context
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        
        // Step 8: Continue to next filter/controller
        filterChain.doFilter(request, response);
    }
}
```

---

## 📄 File 6: CustomUserDetailsService.java

### What Does It Do?
Loads user information for Spring Security. When validating a token, Spring needs to load the full user details.

### Important Feature: Login by Username OR Email

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try to find by username first, then by email
        User user = userRepository.findByUsername(username)
            .or(() -> userRepository.findByEmail(username))  // ← Email fallback!
            .orElseThrow(() -> new UsernameNotFoundException(
                "User not found with username or email: " + username
            ));

        // Convert to Spring Security's UserDetails
        return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getMasterPasswordHash(),
            new ArrayList<>()  // No roles yet
        );
    }
}
```

---

## 🔄 Complete Login Flow

```
┌────────────────────────────────────────────────────────────────┐
│                      LOGIN FLOW                                 │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  1. Client sends: POST /api/auth/login                         │
│     Body: {"username": "johndoe", "masterPassword": "xxx"}     │
│                              ↓                                  │
│  2. AuthController receives request                             │
│                              ↓                                  │
│  3. LoginRequest validates input                                │
│                              ↓                                  │
│  4. AuthenticationService.login()                               │
│     - AuthenticationManager verifies password                   │
│     - CustomUserDetailsService loads user                       │
│     - BCrypt compares hashed passwords                          │
│                              ↓                                  │
│  5. Password correct? ──No──→ Return 401 Unauthorized          │
│                │                                                │
│                │ Yes                                            │
│                ↓                                                │
│  6. JwtTokenProvider generates tokens                           │
│                              ↓                                  │
│  7. Return AuthResponse with tokens                             │
│                                                                │
│  ════════════════════════════════════════════════════════════  │
│                                                                │
│  SUBSEQUENT REQUESTS:                                           │
│                                                                │
│  8. Client sends: GET /api/vault/entries                        │
│     Header: "Authorization: Bearer eyJhbG..."                   │
│                              ↓                                  │
│  9. JwtAuthenticationFilter intercepts request                  │
│     - Extracts token from header                                │
│     - Validates token signature and expiry                      │
│     - Loads user from token                                     │
│     - Sets SecurityContext                                      │
│                              ↓                                  │
│  10. Request continues to VaultController ✓                     │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## 📨 API Usage Examples

### Login Request
```http
POST /api/auth/login
Content-Type: application/json

{
    "username": "johndoe",
    "masterPassword": "MySecure@Pass123"
}
```

### Login Response
```json
{
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "tokenType": "Bearer",
    "user": {
        "id": 1,
        "email": "john@example.com",
        "username": "johndoe",
        "is2faEnabled": false,
        "createdAt": "2024-02-09T10:30:00"
    }
}
```

### Using Token for Protected Endpoints
```http
GET /api/vault/entries
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## ✅ Summary

| File | One-Line Summary |
|------|-----------------|
| `LoginRequest.java` | Defines login form (username + password) |
| `AuthResponse.java` | Response with JWT tokens and user info |
| `AuthenticationService.java` | Verifies credentials and generates tokens |
| `JwtTokenProvider.java` | Creates and validates JWT tokens |
| `JwtAuthenticationFilter.java` | Checks token on every protected request |
| `CustomUserDetailsService.java` | Loads user for Spring Security (supports email login) |
| `SecurityConfig.java` | Configures which endpoints need authentication |
| `AuthController.java` | REST endpoint for login |

After login, users can use their JWT token to access protected endpoints like vault passwords! 🔐
