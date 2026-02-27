package com.revature.passwordmanager.controller;

import com.revature.passwordmanager.dto.NotificationDTO;
import com.revature.passwordmanager.service.notification.NotificationService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  public ResponseEntity<List<NotificationDTO>> getNotifications() {
    String username = getCurrentUsername();
    return ResponseEntity.ok(notificationService.getNotifications(username));
  }

  @GetMapping("/unread-count")
  public ResponseEntity<Map<String, Long>> getUnreadCount() {
    String username = getCurrentUsername();
    long count = notificationService.getUnreadCount(username);
    return ResponseEntity.ok(Map.of("unreadCount", count));
  }

  @PutMapping("/{id}/read")
  public ResponseEntity<String> markAsRead(@PathVariable Long id) {
    String username = getCurrentUsername();
    notificationService.markAsRead(username, id);
    return ResponseEntity.ok("Notification marked as read");
  }

  @PutMapping("/mark-all-read")
  public ResponseEntity<String> markAllAsRead() {
    String username = getCurrentUsername();
    notificationService.markAllAsRead(username);
    return ResponseEntity.ok("All notifications marked as read");
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteNotification(@PathVariable Long id) {
    String username = getCurrentUsername();
    notificationService.deleteNotification(username, id);
    return ResponseEntity.ok("Notification deleted");
  }

  private String getCurrentUsername() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}
