package org.example.notification.service;


import lombok.RequiredArgsConstructor;
import org.example.notification.dto.NotificationDto;
import org.example.notification.mapper.NotificationMapper;
import org.example.notification.model.Notification;
import org.example.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationDto> getUserNotifications(UUID userId) {
        return notificationRepository.findByUserId(userId)
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

    public void markAllAsRead(UUID userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndReadFalse(userId);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }
}