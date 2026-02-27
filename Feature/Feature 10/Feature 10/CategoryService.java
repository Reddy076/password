package com.revature.passwordmanager.service.vault;

import com.revature.passwordmanager.dto.CategoryDTO;
import com.revature.passwordmanager.dto.request.CreateCategoryRequest;
import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.Category;
import com.revature.passwordmanager.repository.CategoryRepository;
import com.revature.passwordmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final UserRepository userRepository;

  /**
   * Get all categories for a user (including default system categories)
   */
  public List<CategoryDTO> getAllCategories(String username) {
    User user = getUserByUsername(username);

    // Get user's custom categories
    List<Category> userCategories = categoryRepository.findByUserIdOrderByNameAsc(user.getId());

    // Get default system categories
    List<Category> defaultCategories = categoryRepository.findByUserIdIsNullAndIsDefaultTrue();

    // Combine and convert to DTOs
    List<CategoryDTO> allCategories = defaultCategories.stream()
        .map(this::toDTO)
        .collect(Collectors.toList());

    allCategories.addAll(userCategories.stream()
        .map(this::toDTO)
        .collect(Collectors.toList()));

    return allCategories;
  }

  /**
   * Get a specific category by ID
   */
  public CategoryDTO getCategoryById(Long categoryId, String username) {
    User user = getUserByUsername(username);

    // First try to find as user's category
    Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId())
        .orElseGet(() ->
        // If not found, check if it's a default category
        categoryRepository.findById(categoryId)
            .filter(c -> Boolean.TRUE.equals(c.getIsDefault()) && c.getUser() == null)
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId)));

    return toDTO(category);
  }

  /**
   * Create a new custom category for a user
   */
  @Transactional
  public CategoryDTO createCategory(CreateCategoryRequest request, String username) {
    User user = getUserByUsername(username);

    // Check for duplicate category name
    if (categoryRepository.existsByUserIdAndName(user.getId(), request.getName())) {
      throw new IllegalArgumentException("Category with name '" + request.getName() + "' already exists");
    }

    Category category = Category.builder()
        .user(user)
        .name(request.getName())
        .icon(request.getIcon())
        .isDefault(false)
        .build();

    Category savedCategory = categoryRepository.save(category);
    return toDTO(savedCategory);
  }

  /**
   * Update an existing category
   */
  @Transactional
  public CategoryDTO updateCategory(Long categoryId, CreateCategoryRequest request, String username) {
    User user = getUserByUsername(username);

    Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

    // Check if this is a default category (cannot be edited)
    if (Boolean.TRUE.equals(category.getIsDefault())) {
      throw new IllegalArgumentException("Cannot modify default categories");
    }

    // Check for duplicate name (excluding current category)
    categoryRepository.findByUserIdAndName(user.getId(), request.getName())
        .ifPresent(existing -> {
          if (!existing.getId().equals(categoryId)) {
            throw new IllegalArgumentException("Category with name '" + request.getName() + "' already exists");
          }
        });

    category.setName(request.getName());
    category.setIcon(request.getIcon());

    Category updatedCategory = categoryRepository.save(category);
    return toDTO(updatedCategory);
  }

  /**
   * Delete a category (only custom categories owned by the user)
   */
  @Transactional
  public void deleteCategory(Long categoryId, String username) {
    User user = getUserByUsername(username);

    Category category = categoryRepository.findByIdAndUserId(categoryId, user.getId())
        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

    // Check if this is a default category (cannot be deleted)
    if (Boolean.TRUE.equals(category.getIsDefault())) {
      throw new IllegalArgumentException("Cannot delete default categories");
    }

    // Note: Vault entries with this category will have category_id set to NULL
    // due to ON DELETE SET NULL in database schema
    categoryRepository.delete(category);
  }

  /**
   * Get count of categories for a user
   */
  public long getCategoryCount(String username) {
    User user = getUserByUsername(username);
    return categoryRepository.countByUserId(user.getId());
  }

  // --------------------- Helper Methods ---------------------

  private User getUserByUsername(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
  }

  private CategoryDTO toDTO(Category category) {
    return CategoryDTO.builder()
        .id(category.getId())
        .name(category.getName())
        .icon(category.getIcon())
        .isDefault(category.getIsDefault())
        .createdAt(category.getCreatedAt())
        .entryCount(0) // Will be populated when VaultEntry is implemented
        .build();
  }
}
