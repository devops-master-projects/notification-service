package org.example.notification.service;

import org.example.notification.dto.NotificationSettingsDto;
import org.example.notification.model.NotificationSettings;
import org.example.notification.model.NotificationType;
import org.example.notification.repository.NotificationSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationSettingsServiceIntegrationTest {

    @Autowired
    private NotificationSettingsService service;

    @Autowired
    private NotificationSettingsRepository repo;

    private UUID userHost;
    private UUID userGuest;
    private UUID userOther;

    @BeforeEach
    void setUp() {
        userHost = UUID.randomUUID();
        userGuest = UUID.randomUUID();
        userOther = UUID.randomUUID();
    }

    private void makeSetting(UUID userId, NotificationType type, boolean enabled) {
        repo.save(NotificationSettings.builder()
                .userId(userId)
                .notifType(type)
                .enabled(enabled)
                .build());
    }

    @Test
    @DisplayName("initUserSettings(host): initializes all types except RESERVATION_RESPONDED")
    void init_host_initializesExpectedSet() {
        service.initUserSettings(userHost, List.of("host"));

        List<NotificationSettings> saved = repo.findByUserId(userHost);
        assertThat(saved).isNotEmpty();

        Set<NotificationType> all = EnumSet.allOf(NotificationType.class);
        Set<NotificationType> expected = all.stream()
                .filter(t -> t != NotificationType.RESERVATION_RESPONDED)
                .collect(Collectors.toSet());

        assertThat(saved).extracting(NotificationSettings::getNotifType)
                .containsExactlyInAnyOrderElementsOf(expected);

        assertThat(saved).allMatch(NotificationSettings::isEnabled);
    }

    @Test
    @DisplayName("initUserSettings(guest): initializes only RESERVATION_RESPONDED")
    void init_guest_initializesOnlyResponded() {
        service.initUserSettings(userGuest, List.of("guest"));

        List<NotificationSettings> saved = repo.findByUserId(userGuest);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getNotifType()).isEqualTo(NotificationType.RESERVATION_RESPONDED);
        assertThat(saved.get(0).isEnabled()).isTrue();
    }

    @Test
    @DisplayName("initUserSettings(other): initializes nothing")
    void init_other_initializesNothing() {
        service.initUserSettings(userOther, List.of("something-else"));

        List<NotificationSettings> saved = repo.findByUserId(userOther);
        assertThat(saved).isEmpty();
    }

    @Test
    @DisplayName("initUserSettings(): does nothing if settings already exist")
    void init_doesNothing_ifAlreadyExists() {
        makeSetting(userHost, NotificationType.RESERVATION_CREATED, false);

        service.initUserSettings(userHost, List.of("host"));

        List<NotificationSettings> after = repo.findByUserId(userHost);
        assertThat(after).hasSize(1);
        assertThat(after.get(0).getNotifType()).isEqualTo(NotificationType.RESERVATION_CREATED);
        assertThat(after.get(0).isEnabled()).isFalse();
    }

    @Test
    @DisplayName("getUserSettings(): returns mapped DTOs for user")
    void getUserSettings_returnsDtos() {
        makeSetting(userGuest, NotificationType.RESERVATION_RESPONDED, true);
        makeSetting(userGuest, NotificationType.RESERVATION_CREATED, false);

        List<NotificationSettingsDto> out = service.getUserSettings(userGuest);
        assertThat(out).hasSize(2);
        assertThat(out).extracting(NotificationSettingsDto::getNotifType)
                .containsExactlyInAnyOrder(NotificationType.RESERVATION_RESPONDED, NotificationType.RESERVATION_CREATED);
        Map<NotificationType, Boolean> byType = out.stream()
                .collect(Collectors.toMap(NotificationSettingsDto::getNotifType, NotificationSettingsDto::isEnabled));
        assertThat(byType.get(NotificationType.RESERVATION_RESPONDED)).isTrue();
        assertThat(byType.get(NotificationType.RESERVATION_CREATED)).isFalse();
    }

    @Test
    @DisplayName("updateSetting(): toggles enabled and persists")
    void updateSetting_toggles() {
        makeSetting(userGuest, NotificationType.RESERVATION_RESPONDED, true);

        NotificationSettingsDto updated = service.updateSetting(userGuest, NotificationType.RESERVATION_RESPONDED, false);
        assertThat(updated.getNotifType()).isEqualTo(NotificationType.RESERVATION_RESPONDED);
        assertThat(updated.isEnabled()).isFalse();

        NotificationSettings reloaded = repo.findByUserIdAndNotifType(userGuest, NotificationType.RESERVATION_RESPONDED)
                .orElseThrow();
        assertThat(reloaded.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("updateSetting(): throws when setting does not exist for user/type")
    void updateSetting_notFound_throws() {
        assertThatThrownBy(() ->
                service.updateSetting(userGuest, NotificationType.RESERVATION_RESPONDED, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Notification setting not found");
    }
}
