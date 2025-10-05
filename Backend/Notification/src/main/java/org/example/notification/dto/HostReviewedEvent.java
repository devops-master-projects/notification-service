package org.example.notification.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class HostReviewedEvent {
    private UUID reviewId;
    private UUID hostId;
    private UUID guestId;
    private String guestFirstName;
    private String guestLastName;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}