package org.example.notification.mapper;

import org.example.notification.dto.NotificationDto;
import org.example.notification.model.Notification;

public class NotificationMapper {
    public static NotificationDto toDto(Notification entity) {
        return NotificationDto.builder()
                .id(entity.getId())
                .notifType(entity.getNotifType())
                .message(entity.getMessage())
                .createdAt(entity.getCreatedAt())
                .read(entity.isRead())
                .build();
    }
}