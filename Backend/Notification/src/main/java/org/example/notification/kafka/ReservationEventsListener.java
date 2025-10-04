package org.example.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.notification.dto.AccommodationInfo;
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
            AccommodationInfo accInfo = getAccommodationInfo(event.getAccommodationId());
            if (settingsRepository.existsByUserIdAndNotifTypeAndEnabledTrue(accInfo.getHostId(), NotificationType.RESERVATION_CREATED))
            {

                Notification notif = Notification.builder()
                        .userId(accInfo.getHostId())
                        .notifType(NotificationType.RESERVATION_CREATED)
                        .message(
                                "New reservation request for accommodation \"" + accInfo.getAccommodationName() + "\" " +
                                        "from " + event.getStartDate() + " to " + event.getEndDate() + " " +
                                        "created by guest " + event.getGuestName() + " " + event.getGuestLastName() +
                                        " (" + event.getGuestEmail() + ")."
                        )
                        .createdAt(event.getCreatedAt())
                        .read(false)
                        .build();


                notificationRepository.save(notif);
                messagingTemplate.convertAndSend(
                        "/topic/notifications/" + accInfo.getHostId(),
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

            AccommodationInfo accInfo = getAccommodationInfo(event.getAccommodationId());
            // save and send the notification only if the user wants to receive notifications of this type
            if (settingsRepository.existsByUserIdAndNotifTypeAndEnabledTrue(accInfo.getHostId(), NotificationType.RESERVATION_CANCELED)) {

                Notification notif = Notification.builder()
                        .userId(accInfo.getHostId())
                        .notifType(NotificationType.RESERVATION_CANCELED)
                        .message(
                                "Reservation for accommodation \"" + accInfo.getAccommodationName() + "\" " +
                                        "from " + event.getStartDate() + " to " + event.getEndDate() +
                                        " has been cancelled by guest " + event.getGuestName() + " " + event.getGuestLastName() +
                                        " (" + event.getGuestEmail() + ")."
                        )
                        .createdAt(event.getCreatedAt())
                        .read(false)
                        .build();

                notificationRepository.save(notif);
                messagingTemplate.convertAndSend(
                        "/topic/notifications/" + accInfo.getHostId(),
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
                AccommodationInfo accInfo = getAccommodationInfo(event.getAccommodationId());

                if ((event.getHostName() == null || event.getHostName().isBlank()) &&
                        (event.getHostLastName() == null || event.getHostLastName().isBlank())) {

                    message = String.format(
                            "Your reservation request for accommodation \"%s\" was automatically approved by the system on %s.",
                            accInfo.getAccommodationName(),
                            event.getRespondedAt().toLocalDate()
                    );

                } else {
                    switch (event.getStatus()) {
                        case APPROVED -> message = String.format(
                                "Good news! Host %s %s has approved your reservation request for accommodation \"%s\" on %s.",
                                event.getHostName(),
                                event.getHostLastName(),
                                accInfo.getAccommodationName(),
                                event.getRespondedAt().toLocalDate()
                        );

                        case REJECTED -> message = String.format(
                                "Unfortunately, host %s %s has rejected your reservation request for accommodation \"%s\" on %s.",
                                event.getHostName(),
                                event.getHostLastName(),
                                accInfo.getAccommodationName(),
                                event.getRespondedAt().toLocalDate()
                        );

                        case CANCELLED -> message = String.format(
                                "Your reservation request for accommodation \"%s\" was cancelled by host %s %s on %s.",
                                accInfo.getAccommodationName(),
                                event.getHostName(),
                                event.getHostLastName(),
                                event.getRespondedAt().toLocalDate()
                        );

                        default -> message = String.format(
                                "Your reservation request for accommodation \"%s\" is still pending host review.",
                                accInfo.getAccommodationName()
                        );
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



    private AccommodationInfo getAccommodationInfo(UUID accommodationId) {
        try {
            return restTemplate.getForObject(
                    accommodationServiceUrl + "/api/accommodations/" + accommodationId + "/host",
                    AccommodationInfo.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch accommodation info", e);
        }
    }


}