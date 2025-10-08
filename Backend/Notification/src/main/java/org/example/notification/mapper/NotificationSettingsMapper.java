package org.example.notification.mapper;

import org.example.notification.dto.NotificationSettingsDto;
import org.example.notification.model.NotificationSettings;

public class NotificationSettingsMapper {

    public static NotificationSettingsDto toDto(NotificationSettings entity) {
        return NotificationSettingsDto.builder()
                .id(entity.getId())
                .notifType(entity.getNotifType())
                .enabled(entity.isEnabled())
                .build();
    }
}