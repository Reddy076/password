package com.revature.passwordmanager.repository;

import com.revature.passwordmanager.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByUsername(String username);

  Optional<User> findByEmail(String email);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  List<User> findByDeletionScheduledAtBefore(LocalDateTime dateTime);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 WHERE u.username = :username OR u.email = :username")
  void incrementFailedLoginAttempts(@Param("username") String username);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE User u SET u.failedLoginAttempts = 0, u.lockedUntil = null WHERE u.username = :username OR u.email = :username")
  void resetFailedLoginAttempts(@Param("username") String username);
}
