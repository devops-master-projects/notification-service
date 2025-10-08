package org.example.notification.dto;


import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AccommodationReviewedEvent {
    private UUID reviewId;
    private UUID accommodationId;
    private UUID guestId;
    private String guestFirstName;
    private String guestLastName;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;
}
