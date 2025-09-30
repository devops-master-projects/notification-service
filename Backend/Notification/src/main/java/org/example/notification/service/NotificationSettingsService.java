package org.example.notification.service;

import lombok.RequiredArgsConstructor;
import org.example.notification.dto.NotificationSettingsDto;
import org.example.notification.mapper.NotificationSettingsMapper;
import org.example.notification.model.NotificationSettings;
import org.example.notification.model.NotificationType;
import org.example.notification.repository.NotificationSettingsRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationSettingsService {

    private final NotificationSettingsRepository settingsRepository;

    public void initUserSettings(UUID userId, Collection<String> roles) {
        if (settingsRepository.findByUserId(userId).isEmpty()) {
            List<NotificationType> toInit;

            if (roles.contains("host")) {
                toInit = Arrays.stream(NotificationType.values())
                        .filter(type -> type != NotificationType.RESERVATION_RESPONDED)
                        .toList();
            } else if (roles.contains("guest")) {
                toInit = List.of(NotificationType.RESERVATION_RESPONDED);
            } else {
                toInit = List.of(); // fallback: nema notifikacija
            }

            toInit.forEach(type -> {
                settingsRepository.save(NotificationSettings.builder()
                        .userId(userId)
                        .notifType(type)
                        .enabled(true)
                        .build()
                );
            });

            System.out.printf("Initialized notification settings for userId=%s, roles=%s%n", userId, roles);
        } else {
            System.out.printf("Notification settings already exist for userId=%s%n", userId);
        }
    }


    public List<NotificationSettingsDto> getUserSettings(UUID userId) {
        return settingsRepository.findByUserId(userId)
                .stream()
                .map(NotificationSettingsMapper::toDto)
                .toList();
    }

    public NotificationSettingsDto updateSetting(UUID userId, NotificationType notifType, boolean enabled) {
        NotificationSettings setting = settingsRepository.findByUserIdAndNotifType(userId, notifType)
                .orElseThrow(() -> new IllegalArgumentException("Notification setting not found"));

        setting.setEnabled(enabled);
        return NotificationSettingsMapper.toDto(settingsRepository.save(setting));
    }
}