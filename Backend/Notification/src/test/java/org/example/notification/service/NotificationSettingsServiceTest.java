package org.example.notification.service;

import org.example.notification.dto.NotificationSettingsDto;
import org.example.notification.model.NotificationSettings;
import org.example.notification.model.NotificationType;
import org.example.notification.repository.NotificationSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationSettingsServiceTest {

    @Mock
    private NotificationSettingsRepository repo;

    @InjectMocks
    private NotificationSettingsService service;

    private UUID userHost;
    private UUID userGuest;
    private UUID userOther;

    @BeforeEach
    void setUp() {
        userHost = UUID.randomUUID();
        userGuest = UUID.randomUUID();
        userOther = UUID.randomUUID();
    }

    private NotificationSettings setting(UUID userId, NotificationType type, boolean enabled) {
        return NotificationSettings.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .notifType(type)
                .enabled(enabled)
                .build();
    }


    @Test
    @DisplayName("initUserSettings(host): saves all types except RESERVATION_RESPONDED when user has no settings")
    void init_host_savesAllExceptResponded() {
        given(repo.findByUserId(userHost)).willReturn(List.of());

        given(repo.save(any(NotificationSettings.class)))
                .willAnswer(inv -> inv.getArgument(0));

        service.initUserSettings(userHost, List.of("host"));

        Set<NotificationType> all = EnumSet.allOf(NotificationType.class);
        Set<NotificationType> expected = all.stream()
                .filter(t -> t != NotificationType.RESERVATION_RESPONDED)
                .collect(Collectors.toSet());

        ArgumentCaptor<NotificationSettings> cap = ArgumentCaptor.forClass(NotificationSettings.class);
        verify(repo, times(expected.size())).save(cap.capture());

        Set<NotificationType> savedTypes = cap.getAllValues().stream()
                .map(NotificationSettings::getNotifType)
                .collect(Collectors.toSet());

        assertThat(savedTypes).isEqualTo(expected);
        assertThat(cap.getAllValues()).allMatch(NotificationSettings::isEnabled);
        assertThat(cap.getAllValues()).allMatch(s -> s.getUserId().equals(userHost));
    }

    @Test
    @DisplayName("initUserSettings(guest): saves only RESERVATION_RESPONDED when user has no settings")
    void init_guest_savesOnlyResponded() {
        given(repo.findByUserId(userGuest)).willReturn(List.of());
        given(repo.save(any(NotificationSettings.class))).willAnswer(inv -> inv.getArgument(0));

        service.initUserSettings(userGuest, List.of("guest"));

        ArgumentCaptor<NotificationSettings> cap = ArgumentCaptor.forClass(NotificationSettings.class);
        verify(repo, times(1)).save(cap.capture());

        NotificationSettings saved = cap.getValue();
        assertThat(saved.getUserId()).isEqualTo(userGuest);
        assertThat(saved.getNotifType()).isEqualTo(NotificationType.RESERVATION_RESPONDED);
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("initUserSettings(other): saves nothing for unknown roles")
    void init_other_savesNothing() {
        given(repo.findByUserId(userOther)).willReturn(List.of());

        service.initUserSettings(userOther, List.of("admin", "moderator"));

        verify(repo, never()).save(any(NotificationSettings.class));
    }

    @Test
    @DisplayName("initUserSettings(): does nothing when settings already exist")
    void init_existing_settings_noop() {
        given(repo.findByUserId(userHost))
                .willReturn(List.of(setting(userHost, NotificationType.RESERVATION_CREATED, false)));

        service.initUserSettings(userHost, List.of("host"));

        verify(repo, never()).save(any(NotificationSettings.class));
    }


    @Test
    @DisplayName("getUserSettings(): maps entities to DTOs")
    void getUserSettings_mapsToDtos() {
        var s1 = setting(userGuest, NotificationType.RESERVATION_RESPONDED, true);
        var s2 = setting(userGuest, NotificationType.RESERVATION_CREATED, false);
        given(repo.findByUserId(userGuest)).willReturn(List.of(s1, s2));

        List<NotificationSettingsDto> dtos = service.getUserSettings(userGuest);

        assertThat(dtos).hasSize(2);
        assertThat(dtos).extracting(NotificationSettingsDto::getNotifType)
                .containsExactlyInAnyOrder(NotificationType.RESERVATION_RESPONDED, NotificationType.RESERVATION_CREATED);

        Map<NotificationType, Boolean> flags = dtos.stream()
                .collect(Collectors.toMap(NotificationSettingsDto::getNotifType, NotificationSettingsDto::isEnabled));
        assertThat(flags.get(NotificationType.RESERVATION_RESPONDED)).isTrue();
        assertThat(flags.get(NotificationType.RESERVATION_CREATED)).isFalse();

        verify(repo).findByUserId(userGuest);
    }


    @Test
    @DisplayName("updateSetting(): finds by (user,type), toggles enabled and saves; returns DTO")
    void updateSetting_ok() {
        var existing = setting(userGuest, NotificationType.RESERVATION_RESPONDED, true);
        given(repo.findByUserIdAndNotifType(userGuest, NotificationType.RESERVATION_RESPONDED))
                .willReturn(Optional.of(existing));

        given(repo.save(any(NotificationSettings.class)))
                .willAnswer(inv -> inv.getArgument(0));

        NotificationSettingsDto out = service.updateSetting(userGuest, NotificationType.RESERVATION_RESPONDED, false);

        assertThat(out.getNotifType()).isEqualTo(NotificationType.RESERVATION_RESPONDED);
        assertThat(out.isEnabled()).isFalse();

        ArgumentCaptor<NotificationSettings> cap = ArgumentCaptor.forClass(NotificationSettings.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().isEnabled()).isFalse();
        assertThat(cap.getValue().getUserId()).isEqualTo(userGuest);
    }

    @Test
    @DisplayName("updateSetting(): throws when setting is missing for (user,type)")
    void updateSetting_notFound_throws() {
        given(repo.findByUserIdAndNotifType(userGuest, NotificationType.RESERVATION_RESPONDED))
                .willReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateSetting(userGuest, NotificationType.RESERVATION_RESPONDED, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Notification setting not found");

        verify(repo, never()).save(any(NotificationSettings.class));
    }
}
