package com.revature.passwordmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.revature.passwordmanager.dto.request.VaultEntryRequest;
import com.revature.passwordmanager.dto.response.VaultEntryDetailResponse;
import com.revature.passwordmanager.dto.response.VaultEntryResponse;
import com.revature.passwordmanager.service.vault.VaultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VaultController.class)
class VaultControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private VaultService vaultService;

        @Autowired
        private ObjectMapper objectMapper;

        // Mock security beans to satisfy filter chain
        @MockBean
        private com.revature.passwordmanager.security.JwtTokenProvider jwtTokenProvider;
        @MockBean
        private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
        @MockBean
        private com.revature.passwordmanager.service.auth.SessionService sessionService;

        private VaultEntryResponse entryResponse;
        private VaultEntryDetailResponse detailResponse;
        private VaultEntryRequest entryRequest;

        @BeforeEach
        void setUp() {
                entryResponse = VaultEntryResponse.builder()
                                .id(1L)
                                .title("Test Entry")
                                .username("******")
                                .build();

                detailResponse = VaultEntryDetailResponse.builder()
                                .id(1L)
                                .title("Test Entry")
                                .username("user")
                                .password("pass")
                                .build();

                entryRequest = VaultEntryRequest.builder()
                                .title("Test Entry")
                                .username("user")
                                .password("pass")
                                .categoryId(1L)
                                .build();
        }

        @Test
        @WithMockUser(username = "testuser")
        void createEntry_ShouldReturnCreated() throws Exception {
                when(vaultService.createEntry(eq("testuser"), any(VaultEntryRequest.class)))
                                .thenReturn(entryResponse);

                mockMvc.perform(post("/api/vault")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(entryRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.title").value("Test Entry"));
        }

        @Test
        @WithMockUser(username = "testuser")
        void getAllEntries_ShouldReturnList() throws Exception {
                when(vaultService.getAllEntries("testuser"))
                                .thenReturn(List.of(entryResponse));

                mockMvc.perform(get("/api/vault"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Test Entry"));
        }

        @Test
        @WithMockUser(username = "testuser")
        void getEntry_ShouldReturnDetail() throws Exception {
                when(vaultService.getEntry("testuser", 1L))
                                .thenReturn(detailResponse);

                mockMvc.perform(get("/api/vault/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.password").value("pass"));
        }

        @Test
        @WithMockUser(username = "testuser")
        void updateEntry_ShouldReturnUpdated() throws Exception {
                when(vaultService.updateEntry(eq("testuser"), eq(1L), any(VaultEntryRequest.class)))
                                .thenReturn(entryResponse);

                mockMvc.perform(put("/api/vault/1")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(entryRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Test Entry"));
        }

        @Test
        @WithMockUser(username = "testuser")
        void deleteEntry_ShouldReturnNoContent() throws Exception {
                doNothing().when(vaultService).deleteEntry("testuser", 1L);

                mockMvc.perform(delete("/api/vault/1")
                                .with(csrf())
                                .accept(MediaType.APPLICATION_JSON)) // Fix for 415 error if any
                                .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "testuser")
        void toggleFavorite_ShouldReturnUpdatedEntry() throws Exception {
                VaultEntryResponse favoriteResponse = VaultEntryResponse.builder()
                                .id(1L)
                                .title("Test Entry")
                                .isFavorite(true)
                                .build();

                when(vaultService.toggleFavorite("testuser", 1L))
                                .thenReturn(favoriteResponse);

                mockMvc.perform(put("/api/vault/1/favorite")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isFavorite").value(true));
        }

        @Test
        @WithMockUser(username = "testuser")
        void getFavorites_ShouldReturnList() throws Exception {
                VaultEntryResponse favoriteResponse = VaultEntryResponse.builder()
                                .id(1L)
                                .title("Test Entry")
                                .isFavorite(true)
                                .build();

                when(vaultService.getFavorites("testuser"))
                                .thenReturn(List.of(favoriteResponse));

                mockMvc.perform(get("/api/vault/favorites"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].isFavorite").value(true));
        }

        @Test
        @WithMockUser(username = "testuser")
        void bulkDelete_ShouldReturnNoContent() throws Exception {
                doNothing().when(vaultService).bulkDelete(eq("testuser"), any());

                mockMvc.perform(post("/api/vault/entries/bulk-delete")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(List.of(1L, 2L))))
                                .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(username = "testuser")
        void viewPassword_ShouldReturnPassword() throws Exception {
                when(vaultService.getPassword("testuser", 1L)).thenReturn("secret");

                mockMvc.perform(post("/api/vault/entries/1/view-password")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)) // Empty body is fine if no params
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.password").value("secret"));
        }

        @Test
        @WithMockUser(username = "testuser")
        void accessSensitiveEntry_ShouldReturnDecryptedDetails() throws Exception {
                com.revature.passwordmanager.dto.request.SensitiveAccessRequest request = new com.revature.passwordmanager.dto.request.SensitiveAccessRequest();
                request.setMasterPassword("password");

                when(vaultService.accessSensitiveEntry(eq("testuser"), eq(1L),
                                any(com.revature.passwordmanager.dto.request.SensitiveAccessRequest.class)))
                                .thenReturn(detailResponse);

                mockMvc.perform(post("/api/vault/1/sensitive-view")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.password").value("pass"));
        }
}
