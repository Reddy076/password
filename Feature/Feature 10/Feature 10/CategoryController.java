package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.CategoryDTO;
import com.revature.passwordmanager.dto.request.CreateCategoryRequest;
import com.revature.passwordmanager.service.vault.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

  private final CategoryService categoryService;

  /**
   * GET /api/categories - Get all categories for the authenticated user
   * Includes both default system categories and user's custom categories
   */
  @GetMapping
  public ResponseEntity<List<CategoryDTO>> getAllCategories() {
    String username = getCurrentUsername();
    List<CategoryDTO> categories = categoryService.getAllCategories(username);
    return ResponseEntity.ok(categories);
  }

  /**
   * GET /api/categories/{id} - Get a specific category by ID
   */
  @GetMapping("/{id}")
  public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
    String username = getCurrentUsername();
    CategoryDTO category = categoryService.getCategoryById(id, username);
    return ResponseEntity.ok(category);
  }

  /**
   * POST /api/categories - Create a new custom category
   */
  @PostMapping
  public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
    String username = getCurrentUsername();
    CategoryDTO category = categoryService.createCategory(request, username);
    return ResponseEntity.status(HttpStatus.CREATED).body(category);
  }

  /**
   * PUT /api/categories/{id} - Update an existing category
   */
  @PutMapping("/{id}")
  public ResponseEntity<CategoryDTO> updateCategory(
      @PathVariable Long id,
      @Valid @RequestBody CreateCategoryRequest request) {
    String username = getCurrentUsername();
    CategoryDTO category = categoryService.updateCategory(id, request, username);
    return ResponseEntity.ok(category);
  }

  /**
   * DELETE /api/categories/{id} - Delete a category
   * Vault entries in this category will have their category set to null
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
    String username = getCurrentUsername();
    categoryService.deleteCategory(id, username);
    return ResponseEntity.noContent().build();
  }

  /**
   * GET /api/categories/{id}/entries - Get vault entries in a category
   * Note: This endpoint will be fully implemented when VaultEntry feature is
   * added
   */
  @GetMapping("/{id}/entries")
  public ResponseEntity<String> getEntriesInCategory(@PathVariable Long id) {
    // Placeholder - will be implemented with Feature 12 (Vault Entry CRUD)
    return ResponseEntity.ok("This endpoint will return vault entries when the Vault feature is implemented");
  }

  // --------------------- Helper Methods ---------------------

  private String getCurrentUsername() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}
