package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.vault.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

  /**
   * Find all categories belonging to a specific user
   */
  List<Category> findByUserId(Long userId);

  /**
   * Find all categories for a user, ordered by name
   */
  List<Category> findByUserIdOrderByNameAsc(Long userId);

  /**
   * Find a category by user ID and category name (for duplicate checking)
   */
  Optional<Category> findByUserIdAndName(Long userId, String name);

  /**
   * Find all default categories (user_id is null for system defaults)
   */
  List<Category> findByUserIdIsNullAndIsDefaultTrue();

  /**
   * Find a specific category by ID and user ID (ensures user owns the category)
   */
  Optional<Category> findByIdAndUserId(Long id, Long userId);

  /**
   * Check if a category with the given name exists for the user
   */
  boolean existsByUserIdAndName(Long userId, String name);

  /**
   * Count categories for a specific user
   */
  long countByUserId(Long userId);
}
