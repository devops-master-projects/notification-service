package org.example.notification.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
        name = "notification_settings",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "notif_type"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationSettings {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "notif_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationType notifType;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @PrePersist
    public void generateId() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
