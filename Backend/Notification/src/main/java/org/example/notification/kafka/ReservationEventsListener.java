package org.example.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.notification.dto.RequestRespondedEvent;
import org.example.notification.dto.ReservationCreatedEvent;
import org.example.notification.model.Notification;
import org.example.notification.model.NotificationSettings;
import org.example.notification.model.NotificationType;
import org.example.notification.model.RequestStatus;
import org.example.notification.repository.NotificationRepository;
import org.example.notification.repository.NotificationSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationEventsListener {

    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository settingsRepository;
    private final RestTemplate restTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${accommodation.service.url}")
    private String accommodationServiceUrl;


    @KafkaListener(topics = "reservation-created", groupId = "notification-service")
    public void handleReservationCreated(ConsumerRecord<String, String> record) {
        try {
            ReservationCreatedEvent event =
                    objectMapper.readValue(record.value(), ReservationCreatedEvent.class);

            // save and send the notification only if the user wants to receive notifications of this type
            UUID hostId = getHostIdForAccommodation(event.getAccommodationId());
            if (settingsRepository.existsByUserIdAndNotifTypeAndEnabledTrue(hostId, NotificationType.RESERVATION_CREATED))
            {

                Notification notif = Notification.builder()
                        .userId(hostId)
                        .notifType(NotificationType.RESERVATION_CREATED)
                        .message("New reservation request created by " + event.getGuestName() + "  " + event.getGuestLastName())
                        .createdAt(event.getCreatedAt())
                        .read(false)
                        .build();

                notificationRepository.save(notif);
                messagingTemplate.convertAndSend(
                        "/topic/notifications/" + hostId,
                        notif
                );
            }
        } catch (Exception e) {
            log.error("Failed to process ReservationCreatedEvent", e);
        }
    }

    @KafkaListener(topics = "reservation-cancelled", groupId = "notification-service")
    public void handleReservationCancelled(ConsumerRecord<String, String> record) {
        try {
            ReservationCreatedEvent event =
                    objectMapper.readValue(record.value(), ReservationCreatedEvent.class);

            UUID hostId = getHostIdForAccommodation(event.getAccommodationId());
            // save and send the notification only if the user wants to receive notifications of this type
            if (settingsRepository.existsByUserIdAndNotifTypeAndEnabledTrue(hostId, NotificationType.RESERVATION_CANCELED)) {

                Notification notif = Notification.builder()
                        .userId(hostId)
                        .notifType(NotificationType.RESERVATION_CANCELED)
                        .message("Reservation cancelled by guest " + event.getGuestName() + "  " + event.getGuestLastName())
                        .createdAt(event.getCreatedAt())
                        .read(false)
                        .build();

                notificationRepository.save(notif);
                messagingTemplate.convertAndSend(
                        "/topic/notifications/" + hostId,
                        notif
                );
            }

        } catch (Exception e) {
            log.error("Failed to process ReservationCreatedEvent", e);
        }
    }

    @KafkaListener(topics = "request-responded", groupId = "notification-service")
    public void handleRequestResponded(ConsumerRecord<String, String> record) {
        try {
            RequestRespondedEvent event =
                    objectMapper.readValue(record.value(), RequestRespondedEvent.class);
            
            // save and send the notification only if the user wants to receive notifications of this type
            if (settingsRepository.existsByUserIdAndNotifTypeAndEnabledTrue(event.getGuestId(), NotificationType.RESERVATION_RESPONDED)) {

                String message;
                if ((event.getHostName() == null || event.getHostName().isBlank()) &&
                        (event.getHostLastName() == null || event.getHostLastName().isBlank())) {
                    message = "Your reservation request was automatically approved by the system.";
                } else {
                    if (event.getStatus() == RequestStatus.APPROVED) {
                        message = String.format("Host %s %s has approved your reservation request.",
                                event.getHostName(), event.getHostLastName());
                    } else if (event.getStatus() == RequestStatus.REJECTED) {
                        message = String.format("Host %s %s has rejected your reservation request.",
                                event.getHostName(), event.getHostLastName());
                    } else if (event.getStatus() == RequestStatus.CANCELLED) {
                        message = String.format("Host %s %s has cancelled your reservation request.",
                                event.getHostName(), event.getHostLastName());
                    } else {
                        message = "Your reservation request is pending host review.";
                    }
                }

                Notification notif = Notification.builder()
                        .userId(event.getGuestId())
                        .notifType(NotificationType.RESERVATION_RESPONDED)
                        .message(message)
                        .createdAt(event.getRespondedAt())
                        .read(false)
                        .build();

                notificationRepository.save(notif);
                messagingTemplate.convertAndSend(
                        "/topic/notifications/" + event.getGuestId(),
                        notif
                );
            }
        } catch (Exception e) {
            log.error("Failed to process RequestRespondedEvent", e);
        }
    }



    private UUID getHostIdForAccommodation(UUID accommodationId) {
        try {
            return restTemplate.getForObject(
                    accommodationServiceUrl + "/api/accommodations/" + accommodationId + "/host",
                    UUID.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve hostId", e);
        }
    }
}
