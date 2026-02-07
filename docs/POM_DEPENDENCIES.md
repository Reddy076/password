# 🛠️ Technology Stack & Dependencies Guide

## Password Manager Application

This document lists all technologies, frameworks, and Maven dependencies required for the Password Manager application with explanations for each.

---

## 📊 Technology Stack Overview

| Category | Technology | Version | Purpose |
|----------|------------|---------|---------|
| **Language** | Java | 21 (LTS) | Core programming language |
| **Framework** | Spring Boot | 3.2.x | Application framework |
| **Database** | MySQL | 8.x | Data persistence |
| **ORM** | Hibernate/JPA | 6.x | Object-relational mapping |
| **Security** | Spring Security | 6.x | Authentication & Authorization |
| **Testing** | JUnit 5 | 5.10.x | Unit testing framework |
| **Mocking** | Mockito | 5.x | Mock objects for testing |
| **Logging** | SLF4J + Logback | 2.x | Application logging |
| **Build Tool** | Maven | 3.9.x | Dependency management |

---

## 📦 Complete pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    
    <modelVersion>4.0.0</modelVersion>
    
    <!-- ==================== PROJECT INFO ==================== -->
    <groupId>com.revature</groupId>
    <artifactId>password-manager</artifactId>
    <version>1.0.0</version>
    <name>Rev-PasswordManager</name>
    <description>Secure Password Manager Application</description>
    <packaging>jar</packaging>
    
    <!-- ==================== PARENT ==================== -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.3</version>
        <relativePath/>
    </parent>
    
    <!-- ==================== PROPERTIES ==================== -->
    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <jjwt.version>0.12.5</jjwt.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <springdoc.version>2.3.0</springdoc.version>
    </properties>
    
    <!-- ==================== DEPENDENCIES ==================== -->
    <dependencies>
        
        <!-- ========== SPRING BOOT CORE ========== -->
        
        <!-- Spring Boot Web Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Spring Boot Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <!-- Spring Boot Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- Spring Boot Security -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        
        <!-- Spring Boot AOP (Aspect-Oriented Programming) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        
        <!-- Spring Boot Actuator (Health Monitoring) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- Spring Boot Mail -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
        
        <!-- ========== DATABASE ========== -->
        
        <!-- MySQL Connector -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- HikariCP (Connection Pool) - Included in spring-boot-starter-data-jpa -->
        
        <!-- ========== SECURITY & ENCRYPTION ========== -->
        
        <!-- JWT (JSON Web Token) - JJWT Library -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jjwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jjwt.version}</version>
            <scope>runtime</scope>
        </dependency>
        
        <!-- BCrypt Password Encoder - Included in Spring Security -->
        
        <!-- TOTP (Two-Factor Authentication) -->
        <dependency>
            <groupId>dev.samstevens.totp</groupId>
            <artifactId>totp</artifactId>
            <version>1.7.1</version>
        </dependency>
        
        <!-- ========== UTILITIES ========== -->
        
        <!-- Lombok (Reduce Boilerplate) -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- MapStruct (DTO Mapping) -->
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>${mapstruct.version}</version>
        </dependency>
        
        <!-- Apache Commons Lang -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-lang3</artifactId>
        </dependency>
        
        <!-- Apache Commons Codec (Encoding/Hashing) -->
        <dependency>
            <groupId>commons-codec</groupId>
            <artifactId>commons-codec</artifactId>
        </dependency>
        
        <!-- Google Guava -->
        <dependency>
            <groupId>com.google.guava</groupId>
            <artifactId>guava</artifactId>
            <version>33.0.0-jre</version>
        </dependency>
        
        <!-- ========== API DOCUMENTATION ========== -->
        
        <!-- SpringDoc OpenAPI (Swagger UI) -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>
        
        <!-- ========== LOGGING ========== -->
        
        <!-- SLF4J + Logback - Included in Spring Boot Starter -->
        
        <!-- ========== TESTING ========== -->
        
        <!-- Spring Boot Test Starter (JUnit 5 + Mockito + AssertJ) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        
        <!-- Spring Security Test -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        
        <!-- H2 Database (In-Memory Testing) -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
        
        <!-- Testcontainers (Integration Testing with Docker) -->
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <scope>test</scope>
        </dependency>
        
        <!-- ========== DEVELOPMENT ========== -->
        
        <!-- Spring Boot DevTools (Hot Reload) -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-devtools</artifactId>
            <scope>runtime</scope>
            <optional>true</optional>
        </dependency>
        
        <!-- Spring Boot Configuration Processor -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
        
    </dependencies>
    
    <!-- ==================== DEPENDENCY MANAGEMENT ==================== -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>1.19.5</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <!-- ==================== BUILD ==================== -->
    <build>
        <plugins>
            <!-- Spring Boot Maven Plugin -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
            
            <!-- Maven Compiler Plugin -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            
            <!-- Surefire Plugin (Unit Tests) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
            </plugin>
            
            <!-- Failsafe Plugin (Integration Tests) -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-failsafe-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
    
</project>
```

---

## 📖 Dependency Explanations

### 🌱 Spring Boot Core Dependencies

| Dependency | Why It's Needed |
|------------|-----------------|
| **spring-boot-starter-web** | REST API development, embedded Tomcat server, JSON handling with Jackson |
| **spring-boot-starter-data-jpa** | Database operations with JPA/Hibernate ORM, HikariCP connection pool |
| **spring-boot-starter-validation** | Bean validation (`@NotNull`, `@Email`, `@Size`), request body validation |
| **spring-boot-starter-security** | Authentication, authorization, password encoding (BCrypt), CSRF protection |
| **spring-boot-starter-aop** | Aspect-Oriented Programming for audit logging (`@Aspect`, `@Around`) |
| **spring-boot-starter-actuator** | Health endpoints (`/actuator/health`), metrics, monitoring |
| **spring-boot-starter-mail** | Send emails for OTP, password reset, security alerts |

---

### 🗄️ Database Dependencies

| Dependency | Why It's Needed |
|------------|-----------------|
| **mysql-connector-j** | JDBC driver to connect to MySQL 8.x database |
| **HikariCP** | Fast connection pooling (included in JPA starter) |

---

### 🔐 Security & Encryption Dependencies

| Dependency | Why It's Needed |
|------------|-----------------|
| **jjwt-api/impl/jackson** | JWT token generation and validation for authentication |
| **totp** | TOTP (Time-based OTP) for Two-Factor Authentication with Google Authenticator |
| **BCrypt** | Password hashing (included in Spring Security) |

**JJWT Usage Example:**
```java
String token = Jwts.builder()
    .setSubject(user.getEmail())
    .setExpiration(new Date(System.currentTimeMillis() + 900000))
    .signWith(secretKey)
    .compact();
```

**TOTP Usage Example:**
```java
SecretGenerator secretGenerator = new DefaultSecretGenerator();
String secret = secretGenerator.generate();
// User scans QR code with Google Authenticator
boolean isValid = verifier.isValidCode(secret, userInputCode);
```

---

### 🛠️ Utility Dependencies

| Dependency | Why It's Needed |
|------------|-----------------|
| **lombok** | Reduce boilerplate code (`@Getter`, `@Setter`, `@Builder`, `@Slf4j`) |
| **mapstruct** | Auto-generate DTO ↔ Entity mappers |
| **commons-lang3** | Utility methods (`StringUtils`, `RandomStringUtils`) |
| **commons-codec** | Encoding utilities (Base64, Hex, DigestUtils) |
| **guava** | Google utilities (rate limiting, caching, collections) |

**Lombok Example:**
```java
@Data
@Builder
@Entity
public class User {
    private Long id;
    private String email;
    private String username;
}
// No need to write getters, setters, constructors!
```

**MapStruct Example:**
```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(User user);
    User toEntity(RegistrationRequest request);
}
// Auto-generates mapping code at compile time
```

---

### 📚 API Documentation

| Dependency | Why It's Needed |
|------------|-----------------|
| **springdoc-openapi** | Auto-generate Swagger UI at `/swagger-ui.html` for API testing |

---

### 📝 Logging Dependencies

| Dependency | Why It's Needed |
|------------|-----------------|
| **SLF4J** | Logging facade (API) |
| **Logback** | Logging implementation (included in Spring Boot) |

**Usage Example:**
```java
@Slf4j  // Lombok annotation
public class VaultService {
    public void viewPassword(Long id) {
        log.info("User {} viewing password for entry {}", userId, id);
        log.debug("Decrypting password...");
        log.error("Failed to decrypt", exception);
    }
}
```

**Logback Configuration (logback-spring.xml):**
```xml
<configuration>
    <appender name="FILE" class="ch.qos.logback.core.FileAppender">
        <file>logs/password-manager.log</file>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

---

### 🧪 Testing Dependencies

| Dependency | Why It's Needed |
|------------|-----------------|
| **spring-boot-starter-test** | JUnit 5 + Mockito + AssertJ + MockMvc |
| **spring-security-test** | Test security (`@WithMockUser`, `@WithUserDetails`) |
| **h2** | In-memory database for fast unit tests |
| **testcontainers** | Real MySQL in Docker for integration tests |

**JUnit 5 + Mockito Example:**
```java
@ExtendWith(MockitoExtension.class)
class VaultServiceTest {
    
    @Mock
    private VaultEntryRepository repository;
    
    @InjectMocks
    private VaultService vaultService;
    
    @Test
    void shouldCreateVaultEntry() {
        // Arrange
        VaultEntry entry = new VaultEntry();
        when(repository.save(any())).thenReturn(entry);
        
        // Act
        VaultEntry result = vaultService.create(entry);
        
        // Assert
        assertThat(result).isNotNull();
        verify(repository, times(1)).save(any());
    }
}
```

**MockMvc Example (Controller Test):**
```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldLoginSuccessfully() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@test.com\",\"password\":\"pass\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists());
    }
}
```

---

### 🔧 Development Dependencies

| Dependency | Why It's Needed |
|------------|-----------------|
| **spring-boot-devtools** | Hot reload on code changes during development |
| **spring-boot-configuration-processor** | IDE support for `application.properties` |

---

## 📊 Dependency Summary by Category

```
CORE (7)
├── spring-boot-starter-web
├── spring-boot-starter-data-jpa
├── spring-boot-starter-validation
├── spring-boot-starter-security
├── spring-boot-starter-aop
├── spring-boot-starter-actuator
└── spring-boot-starter-mail

DATABASE (1)
└── mysql-connector-j

SECURITY (4)
├── jjwt-api
├── jjwt-impl
├── jjwt-jackson
└── totp

UTILITIES (5)
├── lombok
├── mapstruct
├── commons-lang3
├── commons-codec
└── guava

DOCUMENTATION (1)
└── springdoc-openapi

TESTING (5)
├── spring-boot-starter-test (JUnit 5 + Mockito)
├── spring-security-test
├── h2
├── testcontainers-junit-jupiter
└── testcontainers-mysql

DEVELOPMENT (2)
├── spring-boot-devtools
└── spring-boot-configuration-processor

TOTAL: ~25 dependencies
```

---

## ⚙️ application.properties

```properties
# ==================== SERVER ====================
server.port=8080
spring.application.name=password-manager

# ==================== DATABASE ====================
spring.datasource.url=jdbc:mysql://localhost:3306/password_manager_db
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# ==================== JPA/HIBERNATE ====================
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.format_sql=true

# ==================== JWT ====================
jwt.secret=${JWT_SECRET}
jwt.access-token-expiration=900000
jwt.refresh-token-expiration=604800000

# ==================== SECURITY ====================
encryption.algorithm=AES/GCM/NoPadding
encryption.key-size=256
pbkdf2.iterations=100000

# ==================== LOGGING ====================
logging.level.root=INFO
logging.level.com.revature=DEBUG
logging.file.name=logs/password-manager.log

# ==================== ACTUATOR ====================
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when_authorized

# ==================== MAIL ====================
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}

# ==================== API DOCS ====================
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

---

## ✅ Version Compatibility Matrix

| Component | Version | Requires |
|-----------|---------|----------|
| Java | 21 | - |
| Spring Boot | 3.2.x | Java 17+ |
| Spring Framework | 6.1.x | Java 17+ |
| Hibernate | 6.4.x | Java 11+ |
| JUnit | 5.10.x | Java 8+ |
| MySQL | 8.x | - |

---

> **Note:** All version management is handled by `spring-boot-starter-parent`. You only need to specify versions for non-Spring dependencies.
