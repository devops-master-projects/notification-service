package org.example.notification.repository;


import org.example.notification.model.NotificationSettings;
import org.example.notification.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {

    List<NotificationSettings> findByUserId(UUID userId);

    Optional<NotificationSettings> findByUserIdAndNotifType(UUID userId, NotificationType notifType);

    boolean existsByUserIdAndNotifType(UUID userId, NotificationType notifType);

    boolean existsByUserIdAndNotifTypeAndEnabledTrue(UUID userId, NotificationType notifType);

}