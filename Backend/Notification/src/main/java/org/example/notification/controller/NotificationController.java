package org.example.notification.controller;

import lombok.RequiredArgsConstructor;
import org.example.notification.dto.NotificationDto;
import org.example.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {


    private final NotificationService notificationService;

    @PreAuthorize("hasAnyRole('guest','host')")
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getUserNotifications(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaim("sub"));
        return ResponseEntity.ok(notificationService.getUserNotifications(userId));
    }

    @PreAuthorize("hasAnyRole('guest','host')")
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDto>> getUnread(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaim("sub"));
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId));
    }

    @PreAuthorize("hasAnyRole('guest','host')")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaim("sub"));
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('guest','host')")
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

}