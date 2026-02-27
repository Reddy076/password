# 📂 Feature 10: Categories

## 🎯 What is This Feature?

Categories help you **organize your vault entries** by type. Think of them like folders for different kinds of accounts:

- 🌐 **Social Media** (Facebook, Twitter, Instagram)
- 🏦 **Banking** (Bank accounts, Credit cards)
- 📧 **Email** (Gmail, Outlook, Yahoo)
- 🛒 **Shopping** (Amazon, eBay)
- 🎮 **Entertainment** (Netflix, Spotify, Gaming)

---

## 📁 Files in This Feature

| File | Location | Purpose |
|------|----------|---------|
| `Category.java` | `model/vault/` | Database entity for category |
| `CategoryRepository.java` | `repository/` | Database operations |
| `CategoryDTO.java` | `dto/` | Response data transfer object |
| `CreateCategoryRequest.java` | `dto/request/` | Request validation |
| `CategoryService.java` | `service/vault/` | Business logic |
| `CategoryController.java` | `controller/` | REST API endpoints |

---

## 🌐 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/categories` | Get all categories |
| GET | `/api/categories/{id}` | Get specific category |
| POST | `/api/categories` | Create new category |
| PUT | `/api/categories/{id}` | Update category |
| DELETE | `/api/categories/{id}` | Delete category |
| GET | `/api/categories/{id}/entries` | Get entries in category |

---

## 📝 Key Code Explained

### 1. Category Entity (`Category.java`)

```java
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;  // Each category belongs to a user

    private String name;     // "Social Media", "Banking", etc.
    private String icon;     // Icon name for UI
    private Boolean isDefault;  // System default categories
}
```

**What it does:** Stores category information in the database. Each category belongs to one user (except defaults).

---

### 2. Category Repository (`CategoryRepository.java`)

```java
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    
    // Find all categories for a specific user
    List<Category> findByUserId(Long userId);
    
    // Check if category name already exists
    boolean existsByUserIdAndName(Long userId, String name);
    
    // Find by ID AND user (ensures user owns it)
    Optional<Category> findByIdAndUserId(Long id, Long userId);
}
```

**What it does:** Provides database queries without writing SQL. Spring Data JPA generates the implementation automatically.

---

### 3. Category Service (`CategoryService.java`)

```java
@Service
public class CategoryService {

    // Create a new category
    public CategoryDTO createCategory(CreateCategoryRequest request, String username) {
        User user = getUserByUsername(username);
        
        // Check for duplicate name
        if (categoryRepository.existsByUserIdAndName(user.getId(), request.getName())) {
            throw new IllegalArgumentException("Category already exists");
        }
        
        Category category = Category.builder()
                .user(user)
                .name(request.getName())
                .icon(request.getIcon())
                .isDefault(false)
                .build();
        
        return toDTO(categoryRepository.save(category));
    }
    
    // Delete a category (only custom ones)
    public void deleteCategory(Long categoryId, String username) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        
        if (Boolean.TRUE.equals(category.getIsDefault())) {
            throw new IllegalArgumentException("Cannot delete default categories");
        }
        
        categoryRepository.delete(category);
    }
}
```

**What it does:** Contains business logic - validates input, prevents duplicates, and protects default categories.

---

### 4. Category Controller (`CategoryController.java`)

```java
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        String username = getCurrentUsername();
        return ResponseEntity.ok(categoryService.getAllCategories(username));
    }

    @PostMapping
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        String username = getCurrentUsername();
        CategoryDTO category = categoryService.createCategory(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        String username = getCurrentUsername();
        categoryService.deleteCategory(id, username);
        return ResponseEntity.noContent().build();
    }
}
```

**What it does:** Exposes REST API endpoints. Each request gets the current user from the JWT token.

---

## 🔄 How It Works Together

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────┐
│   Frontend   │────>│  Controller  │────>│   Service    │────>│ Database │
│ POST /api/   │     │ validates    │     │ business     │     │ stores   │
│ categories   │     │ JWT token    │     │ logic        │     │ data     │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────┘
       ↑                                         │
       │                                         │
       └─────────── CategoryDTO ─────────────────┘
```

---

## 📋 Example Usage

### Create a Category
```http
POST /api/categories
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
    "name": "Social Media",
    "icon": "social-icon"
}
```

### Response
```json
{
    "id": 1,
    "name": "Social Media",
    "icon": "social-icon",
    "isDefault": false,
    "createdAt": "2026-02-09T11:50:00",
    "entryCount": 0
}
```

---

## ✅ Summary

| Component | File | Key Responsibility |
|-----------|------|-------------------|
| **Entity** | `Category.java` | Database table mapping |
| **Repository** | `CategoryRepository.java` | Database queries |
| **DTO** | `CategoryDTO.java` | API response format |
| **Request** | `CreateCategoryRequest.java` | Input validation |
| **Service** | `CategoryService.java` | Business logic |
| **Controller** | `CategoryController.java` | REST endpoints |
