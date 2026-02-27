package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.FolderDTO;
import com.revature.passwordmanager.service.vault.FolderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FolderControllerTest {

  @Mock
  private FolderService folderService;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private Authentication authentication;

  @InjectMocks
  private FolderController folderController;

  private FolderDTO folderDTO;

  @BeforeEach
  void setUp() {
    folderDTO = new FolderDTO();
    folderDTO.setId(1L);
    folderDTO.setName("Test Folder");

    SecurityContextHolder.setContext(securityContext);
  }

  @Test
  void getFolders_Success() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("testuser");
    when(folderService.getFolders("testuser")).thenReturn(Collections.singletonList(folderDTO));

    ResponseEntity<List<FolderDTO>> response = folderController.getFolders();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1, response.getBody().size());
  }

  @Test
  void createFolder_Success() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("testuser");
    when(folderService.createFolder(anyString(), any(), anyString())).thenReturn(folderDTO);

    ResponseEntity<FolderDTO> response = folderController.createFolder("Test Folder", null);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals("Test Folder", response.getBody().getName());
  }

  @Test
  void updateFolder_Success() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("testuser");
    when(folderService.updateFolder(anyLong(), anyString(), anyString())).thenReturn(folderDTO);

    ResponseEntity<FolderDTO> response = folderController.updateFolder(1L, "Updated Name");

    assertEquals(HttpStatus.OK, response.getStatusCode());
  }

  @Test
  void deleteFolder_Success() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("testuser");
    doNothing().when(folderService).deleteFolder(anyLong(), anyString());

    ResponseEntity<Void> response = folderController.deleteFolder(1L);

    assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
  }
}
