package org.example.notification.dto;

import lombok.*;
import org.example.notification.model.RequestStatus;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestRespondedEvent {
    private UUID reservationRequestId;
    private UUID accommodationId;
    private UUID guestId;
    private String hostName;
    private String hostLastName;
    private LocalDateTime respondedAt;
    private RequestStatus status;
}
