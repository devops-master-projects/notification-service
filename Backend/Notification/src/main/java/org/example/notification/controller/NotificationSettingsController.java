package org.example.notification.controller;

import lombok.RequiredArgsConstructor;

import org.example.notification.dto.NotificationSettingsDto;
import org.example.notification.model.NotificationType;
import org.example.notification.service.NotificationSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notification-settings")
@RequiredArgsConstructor
public class NotificationSettingsController {

    private final NotificationSettingsService settingsService;

    @PreAuthorize("hasAnyRole('guest','host')")
    @GetMapping
    public ResponseEntity<List<NotificationSettingsDto>> getSettings(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getClaim("sub"));
        return ResponseEntity.ok(settingsService.getUserSettings(userId));
    }

    @PreAuthorize("hasAnyRole('guest','host')")
    @PatchMapping("/{notifType}")
    public ResponseEntity<NotificationSettingsDto> updateSetting(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable NotificationType notifType,
            @RequestParam boolean enabled) {

        UUID userId = UUID.fromString(jwt.getClaim("sub"));
        return ResponseEntity.ok(settingsService.updateSetting(userId, notifType, enabled));
    }

    @PostMapping("/init")
    public ResponseEntity<Void> initUserSettings(
            @RequestParam UUID userId,
            @RequestParam String role
    ) {
        settingsService.initUserSettings(userId, List.of(role));
        return ResponseEntity.ok().build();
    }



}