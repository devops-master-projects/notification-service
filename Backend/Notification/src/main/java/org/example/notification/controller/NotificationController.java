package org.example.notification.controller;

import lombok.RequiredArgsConstructor;
import org.example.notification.dto.NotificationDto;
import org.example.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
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
    @GetMapping("/{id}")
    public ResponseEntity<NotificationDto> getNotificationById(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) throws AccessDeniedException {
        UUID userId = UUID.fromString(jwt.getClaim("sub"));
        return ResponseEntity.ok(notificationService.getNotificationById(id, userId));
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

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaim("sub"));
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }


    @PreAuthorize("hasAnyRole('guest','host')")
    @PatchMapping("/read")
    public ResponseEntity<Void> markAsReadBulk(
            @RequestParam List<UUID> ids,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getClaim("sub"));
        notificationService.markAsRead(ids, userId);
        return ResponseEntity.ok().build();
    }


    @PreAuthorize("hasAnyRole('guest','host')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getClaim("sub"));
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('guest','host')")
    @DeleteMapping
    public ResponseEntity<Void> deleteNotifications(
            @RequestParam List<UUID> ids,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getClaim("sub"));
        notificationService.deleteNotifications(ids, userId);
        return ResponseEntity.noContent().build();
    }


}