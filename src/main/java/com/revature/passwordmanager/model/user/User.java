package com.revature.passwordmanager.model.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(name = "master_password_hash", nullable = false)
  private String masterPasswordHash;

  @Column(nullable = false)
  private String salt; // Unique salt for this user's encryption

  @Column(name = "is_2fa_enabled")
  private boolean is2faEnabled = false;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "deletion_requested_at")
  private LocalDateTime deletionRequestedAt;

  @Column(name = "deletion_scheduled_at")
  private LocalDateTime deletionScheduledAt;
}
