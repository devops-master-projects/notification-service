package org.example.notification.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.notification.dto.NotificationDto;
import org.example.notification.model.Notification;
import org.example.notification.model.NotificationType;
import org.example.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationServiceIntegrationTest {

    @Autowired
    private NotificationService service;

    @Autowired
    private NotificationRepository repo;

    private UUID userA;
    private UUID userB;

    @BeforeEach
    void setUp() {
        userA = UUID.randomUUID();
        userB = UUID.randomUUID();
    }

    private Notification n(
            UUID userId,
            String msg,
            boolean read,
            LocalDateTime createdAt,
            NotificationType type
    ) {
        Notification x = new Notification();
        x.setUserId(userId);
        x.setMessage(msg);
        x.setRead(read);
        x.setCreatedAt(createdAt);
        x.setNotifType(type);
        return repo.save(x);
    }

    @Test
    @DisplayName("getUserNotifications(): returns user's notifications sorted by createdAt DESC")
    void getUserNotifications_sortedDesc() {
        LocalDateTime now = LocalDateTime.now();


        n(userA, "old",    false, now.minusDays(2), NotificationType.RESERVATION_CREATED);
        n(userA, "newest", true,  now,              NotificationType.RESERVATION_CANCELED);
        n(userA, "mid",    false, now.minusDays(1), NotificationType.RESERVATION_CREATED);
        n(userB, "other-user", false, now,          NotificationType.RESERVATION_CREATED);

        List<NotificationDto> out = service.getUserNotifications(userA);
        assertThat(out).extracting(NotificationDto::getMessage)
                .containsExactly("newest", "mid", "old");
        assertThat(out).noneMatch(d -> "other-user".equals(d.getMessage()));
    }

    @Test
    @DisplayName("getUnreadNotifications(): returns only unread for user")
    void getUnreadNotifications_onlyUnread() {
        LocalDateTime now = LocalDateTime.now();

        n(userA, "u1", false, now, NotificationType.RESERVATION_CREATED);
        n(userA, "r1", true,  now, NotificationType.RESERVATION_CREATED);
        n(userB, "u-other", false, now, NotificationType.RESERVATION_CANCELED);

        List<NotificationDto> out = service.getUnreadNotifications(userA);
        assertThat(out).extracting(NotificationDto::isRead).containsOnly(false);
        assertThat(out).extracting(NotificationDto::getMessage).containsExactly("u1");
    }

    @Test
    @DisplayName("getNotificationById(): returns DTO when (id,userId) match; throws on missing")
    void getNotificationById_ok_and_notFound() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Notification owned = n(userA, "mine", false, now, NotificationType.RESERVATION_CREATED);

        NotificationDto dto = service.getNotificationById(owned.getId(), userA);
        assertThat(dto.getId()).isEqualTo(owned.getId());
        assertThat(dto.getMessage()).isEqualTo("mine");

        assertThatThrownBy(() -> service.getNotificationById(owned.getId(), userB))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Notification not found");

        assertThatThrownBy(() -> service.getNotificationById(UUID.randomUUID(), userA))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Notification not found");
    }

    @Test
    @DisplayName("markAllAsRead(): marks only user's unread notifications as read")
    void markAllAsRead_marksOnlyUser() {
        LocalDateTime now = LocalDateTime.now();

        Notification a1 = n(userA, "a1", false, now, NotificationType.RESERVATION_CREATED);
        Notification a2 = n(userA, "a2", false, now, NotificationType.RESERVATION_CANCELED);
        Notification a3 = n(userA, "a3", true,  now, NotificationType.RESERVATION_CREATED);
        Notification b1 = n(userB, "b1", false, now, NotificationType.RESERVATION_CREATED);

        service.markAllAsRead(userA);

        assertThat(repo.findById(a1.getId()).orElseThrow().isRead()).isTrue();
        assertThat(repo.findById(a2.getId()).orElseThrow().isRead()).isTrue();
        assertThat(repo.findById(a3.getId()).orElseThrow().isRead()).isTrue();
        assertThat(repo.findById(b1.getId()).orElseThrow().isRead()).isFalse();
    }

    @Test
    @DisplayName("markAsRead(ids,user): marks subset that belongs to user; throws when none found")
    void markAsRead_bulk_ok_and_emptyThrows() {
        LocalDateTime now = LocalDateTime.now();

        Notification a1 = n(userA, "a1", false, now, NotificationType.RESERVATION_CREATED);
        Notification a2 = n(userA, "a2", false, now, NotificationType.RESERVATION_CANCELED);
        Notification b1 = n(userB, "b1", false, now, NotificationType.RESERVATION_CANCELED);

        service.markAsRead(List.of(a1.getId(), a2.getId(), b1.getId()), userA);

        assertThat(repo.findById(a1.getId()).orElseThrow().isRead()).isTrue();
        assertThat(repo.findById(a2.getId()).orElseThrow().isRead()).isTrue();
        assertThat(repo.findById(b1.getId()).orElseThrow().isRead()).isFalse();

        assertThatThrownBy(() -> service.markAsRead(List.of(b1.getId()), userA))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No notifications found for user");
    }

    @Test
    @DisplayName("markAsRead(id,user): marks single notification; throws when not found for user")
    void markAsRead_single_ok_and_notFound() {
        LocalDateTime now = LocalDateTime.now();

        Notification a1 = n(userA, "a1", false, now, NotificationType.RESERVATION_CREATED);
        Notification b1 = n(userB, "b1", false, now, NotificationType.RESERVATION_CREATED);

        service.markAsRead(a1.getId(), userA);
        assertThat(repo.findById(a1.getId()).orElseThrow().isRead()).isTrue();

        assertThatThrownBy(() -> service.markAsRead(b1.getId(), userA))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Notification not found for user");
    }

    @Test
    @DisplayName("deleteNotification(): deletes when (id,userId) match; throws when not found")
    void deleteNotification_ok_and_notFound() {
        LocalDateTime now = LocalDateTime.now();

        Notification a1 = n(userA, "a1", false, now, NotificationType.RESERVATION_CREATED);
        Notification b1 = n(userB, "b1", false, now, NotificationType.RESERVATION_CANCELED);

        service.deleteNotification(a1.getId(), userA);
        assertThat(repo.findById(a1.getId())).isEmpty();

        assertThatThrownBy(() -> service.deleteNotification(b1.getId(), userA))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Notification not found");
        assertThat(repo.findById(b1.getId())).isPresent();
    }

    @Test
    @DisplayName("deleteNotifications(): deletes only owned; throws when none belong to user")
    void deleteNotifications_bulk_ok_and_emptyThrows() {
        LocalDateTime now = LocalDateTime.now();

        Notification a1 = n(userA, "a1", false, now, NotificationType.RESERVATION_CREATED);
        Notification a2 = n(userA, "a2", false, now, NotificationType.RESERVATION_CANCELED);
        Notification b1 = n(userB, "b1", false, now, NotificationType.RESERVATION_CREATED);

        service.deleteNotifications(List.of(a1.getId(), a2.getId(), b1.getId()), userA);
        assertThat(repo.findById(a1.getId())).isEmpty();
        assertThat(repo.findById(a2.getId())).isEmpty();
        assertThat(repo.findById(b1.getId())).isPresent();

        assertThatThrownBy(() -> service.deleteNotifications(List.of(b1.getId()), userA))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No notifications found for user");
    }
}
