package com.revature.passwordmanager;

import com.revature.passwordmanager.model.user.User;
import com.revature.passwordmanager.model.vault.VaultEntry;
import com.revature.passwordmanager.dto.response.VaultEntryDetailResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ManualModelTest {

  @Test
  void testVaultEntry() {
    VaultEntry v = new VaultEntry();
    v.setId(1L);
    v.setTitle("title");
    v.setUsername("user");
    v.setPassword("pass");
    v.setWebsiteUrl("url");
    v.setNotes("notes");
    v.setIsFavorite(true);
    v.setIsHighlySensitive(true);
    v.setIsDeleted(false);

    assertEquals(1L, v.getId());
    assertEquals("title", v.getTitle());
    assertEquals("user", v.getUsername());
    assertEquals("pass", v.getPassword());
    assertEquals("url", v.getWebsiteUrl());
    assertEquals("notes", v.getNotes());
    assertTrue(v.getIsFavorite());
    assertTrue(v.getIsHighlySensitive());
    assertFalse(v.getIsDeleted());

    // Builder
    VaultEntry v2 = VaultEntry.builder()
        .id(2L)
        .title("t2")
        .username("u2")
        .password("p2")
        .websiteUrl("w2")
        .notes("n2")
        .isFavorite(false)
        .isHighlySensitive(false)
        .isDeleted(true)
        .build();

    assertEquals(2L, v2.getId());
    assertNotNull(v2.toString());
    assertNotEquals(v, v2);

    // AllArgsConstructor covered by builder.build()
    VaultEntry.builder().build();
  }

  @Test
  void testUser() {
    User u = new User();
    u.setId(1L);
    u.setEmail("e");
    u.setUsername("u");
    u.setMasterPasswordHash("h");
    u.setSalt("s");
    u.set2faEnabled(true);
    u.setFailedLoginAttempts(1);
    u.setLockoutCount(1);
    u.setDuressPasswordHash("d");
    u.setPasswordHint("ph");

    assertEquals(1L, u.getId());
    assertEquals("e", u.getEmail());
    assertEquals("u", u.getUsername());
    assertEquals("h", u.getMasterPasswordHash());
    assertEquals("s", u.getSalt());
    assertTrue(u.is2faEnabled());
    assertEquals(1, u.getFailedLoginAttempts());
    assertEquals(1, u.getLockoutCount());
    assertEquals("d", u.getDuressPasswordHash());
    assertEquals("ph", u.getPasswordHint());

    // Builder
    User u2 = User.builder()
        .id(2L)
        .username("u2")
        .email("e2")
        .masterPasswordHash("h2")
        .salt("s2")
        .is2faEnabled(false)
        .failedLoginAttempts(0)
        .lockoutCount(0)
        .duressPasswordHash("d2")
        .passwordHint("ph2")
        .build();

    assertEquals(2L, u2.getId());
    assertNotNull(u2.toString());
    assertNotEquals(u, u2);
  }

  @Test
  void testVaultEntryDetailResponse() {
    VaultEntryDetailResponse r = new VaultEntryDetailResponse();
    r.setId(1L);
    r.setTitle("t");
    r.setUsername("u");
    r.setPassword("p");
    r.setWebsiteUrl("w");
    r.setNotes("n");
    r.setIsFavorite(true);
    r.setIsHighlySensitive(true);

    assertEquals(1L, r.getId());
    assertEquals("t", r.getTitle());

    // Builder
    VaultEntryDetailResponse r2 = VaultEntryDetailResponse.builder()
        .id(2L)
        .title("t2")
        .username("u2")
        .password("p2")
        .websiteUrl("w2")
        .notes("n2")
        .isFavorite(false)
        .isHighlySensitive(false)
        .categoryId(1L)
        .folderId(1L)
        .categoryName("c")
        .folderName("f")
        .createdAt(null)
        .updatedAt(null)
        .build();

    assertEquals(2L, r2.getId());
    assertNotNull(r2.toString());
    assertNotEquals(r, r2);
  }
}
