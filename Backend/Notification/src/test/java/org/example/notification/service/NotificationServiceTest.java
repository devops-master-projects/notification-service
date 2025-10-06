package org.example.notification.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.notification.dto.NotificationDto;
import org.example.notification.model.Notification;
import org.example.notification.model.NotificationType;
import org.example.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repo;

    @InjectMocks
    private NotificationService service;

    private UUID userA;
    private UUID userB;

    @BeforeEach
    void setUp() {
        userA = UUID.randomUUID();
        userB = UUID.randomUUID();
    }

    private Notification n(UUID userId, String msg, boolean read, LocalDateTime ts, NotificationType type) {
        Notification x = new Notification();
        x.setId(UUID.randomUUID());
        x.setUserId(userId);
        x.setMessage(msg);
        x.setRead(read);
        x.setCreatedAt(ts);
        x.setNotifType(type);
        return x;
    }


    @Test
    @DisplayName("getUserNotifications(): maps repository results to DTOs preserving order")
    void getUserNotifications_ok() {
        var now = LocalDateTime.now();
        var n1 = n(userA, "newest", true, now, NotificationType.RESERVATION_CREATED);
        var n2 = n(userA, "older", false, now.minusDays(1), NotificationType.RESERVATION_CANCELED);

        given(repo.findByUserIdOrderByCreatedAtDesc(userA)).willReturn(List.of(n1, n2));

        List<NotificationDto> out = service.getUserNotifications(userA);

        assertThat(out).hasSize(2);
        assertThat(out.get(0).getMessage()).isEqualTo("newest");
        assertThat(out.get(1).getMessage()).isEqualTo("older");
        verify(repo).findByUserIdOrderByCreatedAtDesc(userA);
    }


    @Test
    @DisplayName("getUnreadNotifications(): returns only unread mapped to DTOs")
    void getUnreadNotifications_ok() {
        var now = LocalDateTime.now();
        var u1 = n(userA, "u1", false, now, NotificationType.RESERVATION_CREATED);
        var u2 = n(userA, "u2", false, now.minusHours(1), NotificationType.RESERVATION_CANCELED);

        given(repo.findByUserIdAndReadFalse(userA)).willReturn(List.of(u1, u2));

        List<NotificationDto> out = service.getUnreadNotifications(userA);

        assertThat(out).extracting(NotificationDto::isRead).containsOnly(false);
        assertThat(out).extracting(NotificationDto::getMessage).containsExactly("u1", "u2");
        verify(repo).findByUserIdAndReadFalse(userA);
    }


    @Test
    @DisplayName("getNotificationById(): returns DTO when (id,userId) matches")
    void getById_ok() throws Exception {
        UUID id = UUID.randomUUID();
        var n = n(userA, "mine", false, LocalDateTime.now(), NotificationType.RESERVATION_CREATED);
        n.setId(id);

        given(repo.findByIdAndUserId(id, userA)).willReturn(Optional.of(n));

        NotificationDto dto = service.getNotificationById(id, userA);

        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getMessage()).isEqualTo("mine");
        verify(repo).findByIdAndUserId(id, userA);
    }

    @Test
    @DisplayName("getNotificationById(): throws EntityNotFoundException when not found for (id,userId)")
    void getById_notFound() {
        UUID id = UUID.randomUUID();
        given(repo.findByIdAndUserId(id, userA)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNotificationById(id, userA))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Notification not found");
    }

    @Test
    @DisplayName("getNotificationById(): throws AccessDeniedException if repository returns record for another user")
    void getById_accessDenied() {
        UUID id = UUID.randomUUID();
        var other = n(userB, "foreign", false, LocalDateTime.now(), NotificationType.RESERVATION_CREATED);
        other.setId(id);
        given(repo.findByIdAndUserId(id, userA)).willReturn(Optional.of(other));

        assertThatThrownBy(() -> service.getNotificationById(id, userA))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed");
    }


    @Test
    @DisplayName("markAllAsRead(): marks only provided user's unread notifications and saves all")
    void markAllAsRead_ok() {
        var now = LocalDateTime.now();
        var a1 = n(userA, "a1", false, now, NotificationType.RESERVATION_CREATED);
        var a2 = n(userA, "a2", false, now, NotificationType.RESERVATION_CANCELED);

        given(repo.findByUserIdAndReadFalse(userA)).willReturn(List.of(a1, a2));

        service.markAllAsRead(userA);

        assertThat(a1.isRead()).isTrue();
        assertThat(a2.isRead()).isTrue();
        verify(repo).saveAll(argThat(
                (List<Notification> list) -> list.size() == 2 && list.stream().allMatch(Notification::isRead)
        ));
    }

    @Test
    @DisplayName("markAsRead(ids,user): marks found notifications and saves all")
    void markAsRead_bulk_ok() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        var now = LocalDateTime.now();

        var a1 = n(userA, "a1", false, now, NotificationType.RESERVATION_CREATED); a1.setId(id1);
        var a2 = n(userA, "a2", false, now, NotificationType.RESERVATION_CANCELED); a2.setId(id2);

        given(repo.findByIdInAndUserId(List.of(id1, id2), userA)).willReturn(List.of(a1, a2));

        service.markAsRead(List.of(id1, id2), userA);

        assertThat(a1.isRead()).isTrue();
        assertThat(a2.isRead()).isTrue();
        verify(repo).saveAll(argThat(
                (List<Notification> list) -> list.size() == 2 && list.stream().allMatch(Notification::isRead)
        ));
    }

    @Test
    @DisplayName("markAsRead(ids,user): throws when none found for user")
    void markAsRead_bulk_noneFound_throws() {
        UUID id = UUID.randomUUID();
        given(repo.findByIdInAndUserId(List.of(id), userA)).willReturn(List.of());

        assertThatThrownBy(() -> service.markAsRead(List.of(id), userA))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No notifications found for user");

        verify(repo, never()).saveAll(anyList());
    }


    @Test
    @DisplayName("markAsRead(id,user): marks one and saves")
    void markAsRead_single_ok() {
        UUID id = UUID.randomUUID();
        var n = n(userA, "one", false, LocalDateTime.now(), NotificationType.RESERVATION_CREATED);
        n.setId(id);

        given(repo.findByIdAndUserId(id, userA)).willReturn(Optional.of(n));

        service.markAsRead(id, userA);

        assertThat(n.isRead()).isTrue();
        verify(repo).save(argThat(saved -> saved.getId().equals(id) && saved.isRead()));
    }

    @Test
    @DisplayName("markAsRead(id,user): throws when not found for user")
    void markAsRead_single_notFound_throws() {
        UUID id = UUID.randomUUID();
        given(repo.findByIdAndUserId(id, userA)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsRead(id, userA))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Notification not found for user");

        verify(repo, never()).save(any());
    }


    @Test
    @DisplayName("deleteNotification(): deletes when (id,userId) match")
    void deleteNotification_ok() {
        UUID id = UUID.randomUUID();
        var n = n(userA, "del", false, LocalDateTime.now(), NotificationType.RESERVATION_CREATED);
        n.setId(id);

        given(repo.findByIdAndUserId(id, userA)).willReturn(Optional.of(n));

        service.deleteNotification(id, userA);

        verify(repo).delete(n);
    }

    @Test
    @DisplayName("deleteNotification(): throws when not found for (id,userId)")
    void deleteNotification_notFound_throws() {
        UUID id = UUID.randomUUID();
        given(repo.findByIdAndUserId(id, userA)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteNotification(id, userA))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Notification not found");

        verify(repo, never()).delete(any());
    }


    @Test
    @DisplayName("deleteNotifications(): deletes only those belonging to user; throws when none belong")
    void deleteNotifications_ok_and_noneBelong_throws() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        var a1 = n(userA, "a1", false, LocalDateTime.now(), NotificationType.RESERVATION_CREATED); a1.setId(id1);
        var a2 = n(userA, "a2", true,  LocalDateTime.now(), NotificationType.RESERVATION_CANCELED); a2.setId(id2);
        var b1 = n(userB, "b1", false, LocalDateTime.now(), NotificationType.RESERVATION_CREATED); b1.setId(id3);

        given(repo.findAllById(List.of(id1, id2, id3))).willReturn(List.of(a1, a2, b1));

        service.deleteNotifications(List.of(id1, id2, id3), userA);

        verify(repo).deleteAll(argThat(
                (List<Notification> list) -> list.size() == 2
                        && list.contains(a1)
                        && list.contains(a2)
                        && !list.contains(b1)
        ));

        given(repo.findAllById(List.of(id3))).willReturn(List.of(b1));

        assertThatThrownBy(() -> service.deleteNotifications(List.of(id3), userA))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No notifications found for user");

        verify(repo, never()).deleteAll(Collections.emptyList());
    }
}
