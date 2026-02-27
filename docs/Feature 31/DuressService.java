package com.revature.passwordmanager.service.security;

import com.revature.passwordmanager.exception.ResourceNotFoundException;
import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class DuressService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  private static final String[] DUMMY_TITLES = {
      "Email", "Social Media", "Banking", "Shopping", "Streaming",
      "Cloud Storage", "Gaming", "News", "Travel", "Fitness"
  };
  private static final String[] DUMMY_URLS = {
      "https://mail.example.com", "https://social.example.com",
      "https://bank.example.com", "https://shop.example.com",
      "https://stream.example.com", "https://cloud.example.com"
  };

  @Transactional
  public void setDuressPassword(String username, String duressPassword) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    user.setDuressPasswordHash(passwordEncoder.encode(duressPassword));
    userRepository.save(user);
  }

  public boolean isDuressLogin(String username, String password) {
    User user = userRepository.findByUsername(username)
        .or(() -> userRepository.findByEmail(username))
        .orElse(null);
    if (user == null || user.getDuressPasswordHash() == null) {
      return false;
    }
    return passwordEncoder.matches(password, user.getDuressPasswordHash());
  }

  public List<VaultEntry> generateDummyVault(User user) {
    List<VaultEntry> dummyEntries = new ArrayList<>();
    Random random = new Random();

    for (int i = 0; i < 5; i++) {
      VaultEntry entry = VaultEntry.builder()
          .id((long) -(i + 1))
          .user(user)
          .title(DUMMY_TITLES[random.nextInt(DUMMY_TITLES.length)])
          .username("user" + random.nextInt(1000) + "@example.com")
          .password("***encrypted***")
          .websiteUrl(DUMMY_URLS[random.nextInt(DUMMY_URLS.length)])
          .notes("")
          .createdAt(LocalDateTime.now().minusDays(random.nextInt(365)))
          .updatedAt(LocalDateTime.now().minusDays(random.nextInt(30)))
          .build();
      dummyEntries.add(entry);
    }
    return dummyEntries;
  }
}
