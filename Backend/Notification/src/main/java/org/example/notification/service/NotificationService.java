package org.example.notification.service;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.notification.dto.NotificationDto;
import org.example.notification.mapper.NotificationMapper;
import org.example.notification.model.Notification;
import org.example.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationDto> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationMapper::toDto)
                .toList();
    }

    public List<NotificationDto> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndReadFalse(userId)
                .stream()
                .map(NotificationMapper::toDto)
                .toList();
    }


    public NotificationDto getNotificationById(UUID notificationId, UUID userId) throws AccessDeniedException {
        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not allowed to access this notification");
        }

        return NotificationMapper.toDto(notification);
    }

    public void markAllAsRead(UUID userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndReadFalse(userId);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    public void markAsRead(List<UUID> ids, UUID userId) {
        List<Notification> notifs = notificationRepository.findByIdInAndUserId(ids, userId);

        if (notifs.isEmpty()) {
            throw new EntityNotFoundException("No notifications found for user");
        }

        notifs.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifs);
    }

    public void markAsRead(UUID id, UUID userId) {
        Notification notif = notificationRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found for user"));

        notif.setRead(true);
        notificationRepository.save(notif);
    }

    public void deleteNotification(UUID id, UUID userId) {
        Notification notif = notificationRepository
                .findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));

        notificationRepository.delete(notif);
    }

    public void deleteNotifications(List<UUID> ids, UUID userId) {
        List<Notification> notifs = notificationRepository.findAllById(ids)
                .stream()
                .filter(n -> n.getUserId().equals(userId))
                .toList();

        if (notifs.isEmpty()) {
            throw new EntityNotFoundException("No notifications found for user");
        }

        notificationRepository.deleteAll(notifs);
    }


}