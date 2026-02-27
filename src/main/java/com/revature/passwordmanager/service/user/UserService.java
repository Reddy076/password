package com.revature.passwordmanager.service.user;

import com.revature.passwordmanager.dto.request.ChangePasswordRequest;
import com.revature.passwordmanager.dto.request.UpdateProfileRequest;
import com.revature.passwordmanager.dto.response.UserResponse;
import com.revature.passwordmanager.exception.AuthenticationException;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.repository.UserRepository;
import com.revature.passwordmanager.repository.VaultEntryRepository;
import com.revature.passwordmanager.repository.CategoryRepository;
import com.revature.passwordmanager.repository.FolderRepository;
import com.revature.passwordmanager.repository.NotificationRepository;
import com.revature.passwordmanager.dto.response.DashboardResponse;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final VaultEntryRepository vaultEntryRepository;
  private final CategoryRepository categoryRepository;
  private final FolderRepository folderRepository;
  private final NotificationRepository notificationRepository;
  private final PasswordEncoder passwordEncoder;
  private final com.revature.passwordmanager.service.security.SecurityAuditService securityAuditService;

  @Transactional(readOnly = true)
  public UserResponse getUserProfile(String username) {
    User user = userRepository.findByUsernameOrThrow(username);

    return UserResponse.builder()
        .id(user.getId())
        .email(user.getEmail())
        .username(user.getUsername())
        .name(user.getName())
        .phoneNumber(user.getPhoneNumber())
        .is2faEnabled(user.is2faEnabled())
        .createdAt(user.getCreatedAt())
        .deletionScheduledAt(user.getDeletionScheduledAt())
        .build();
  }

  @Transactional
  public UserResponse updateProfile(String username, UpdateProfileRequest request) {
    User user = userRepository.findByUsernameOrThrow(username);

    if (request.getName() != null) {
      user.setName(request.getName());
    }
    if (request.getPhoneNumber() != null) {
      user.setPhoneNumber(request.getPhoneNumber());
    }
    if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
      if (userRepository.existsByEmail(request.getEmail())) {
        throw new IllegalArgumentException("Email address is already in use by another account.");
      }
      user.setEmail(request.getEmail());
    }

    User savedUser = userRepository.save(user);

    return UserResponse.builder()
        .id(savedUser.getId())
        .email(savedUser.getEmail())
        .username(savedUser.getUsername())
        .name(savedUser.getName())
        .phoneNumber(savedUser.getPhoneNumber())
        .is2faEnabled(savedUser.is2faEnabled())
        .createdAt(savedUser.getCreatedAt())
        .build();
  }

  @Transactional
  public void changeMasterPassword(String username, ChangePasswordRequest request) {
    User user = userRepository.findByUsernameOrThrow(username);

    if (!passwordEncoder.matches(request.getOldPassword(), user.getMasterPasswordHash())) {
      throw new AuthenticationException("Invalid old password");
    }

    user.setMasterPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);

  }

  @Transactional
  public DashboardResponse getDashboardData(String username) {
    User user = userRepository.findByUsernameOrThrow(username);

    int vaultEntries = (int) vaultEntryRepository.countByUserAndIsDeletedFalse(user);
    int favorites = (int) vaultEntryRepository.countByUserAndIsFavoriteTrue(user);
    int categories = (int) categoryRepository.countByUser(user);
    int folders = (int) folderRepository.countByUser(user);
    int unreadNotifications = (int) notificationRepository.countByUserAndIsReadFalse(user);

    var auditReport = securityAuditService.generateAuditReport(username);

    return DashboardResponse.builder()
        .totalVaultEntries(vaultEntries)
        .totalCategories(categories)
        .totalFolders(folders)
        .totalFavorites(favorites)
        .weakPasswordsCount(auditReport.getWeakCount())
        .reusedPasswordsCount(auditReport.getReusedCount())
        .oldPasswordsCount(auditReport.getOldCount())
        .unreadNotifications(unreadNotifications)
        .build();
  }
}
