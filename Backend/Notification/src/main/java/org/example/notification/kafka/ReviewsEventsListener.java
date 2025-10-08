package org.example.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.notification.dto.AccommodationReviewedEvent;
import org.example.notification.dto.HostReviewedEvent;
import org.example.notification.model.Notification;
import org.example.notification.model.NotificationType;
import org.example.notification.repository.NotificationRepository;
import org.example.notification.repository.NotificationSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewsEventsListener {

    private final NotificationRepository notificationRepository;
    private final NotificationSettingsRepository settingsRepository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;

    @Value("${accommodation.service.url}")
    private String accommodationServiceUrl;
    @KafkaListener(topics = "accommodation-reviewed", groupId = "notification-service")
    public void handleAccommodationReviewed(ConsumerRecord<String, String> record) {
        try {

            AccommodationReviewedEvent event =
                    objectMapper.readValue(record.value(), AccommodationReviewedEvent.class);

            UUID hostId = getHostIdForAccommodation(event.getAccommodationId());

            if (settingsRepository.existsByUserIdAndNotifTypeAndEnabledTrue(
                    hostId, NotificationType.ACCOMMODATION_RATED)) {

                Notification notif = Notification.builder()
                        .userId(hostId)
                        .notifType(NotificationType.ACCOMMODATION_RATED)
                        .message("Your accommodation was reviewed by "
                                + event.getGuestFirstName() + " " + event.getGuestLastName()
                                + " with rating " + event.getRating() + "/5.")
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
            log.error("Failed to process AccommodationReviewedEvent", e);
        }
    }

    @KafkaListener(topics = "host-reviewed", groupId = "notification-service")
    public void handleHostReviewed(ConsumerRecord<String, String> record) {
        try {
            HostReviewedEvent event =
                    objectMapper.readValue(record.value(), HostReviewedEvent.class);

            UUID hostId = event.getHostId();

            if (settingsRepository.existsByUserIdAndNotifTypeAndEnabledTrue(
                    hostId, NotificationType.HOST_RATED)) {


                Notification notif = Notification.builder()
                        .userId(hostId)
                        .notifType(NotificationType.HOST_RATED)
                        .message("You were reviewed by "
                                + event.getGuestFirstName() + " " + event.getGuestLastName()
                                + " with rating " + event.getRating() + "/5.")
                        .createdAt(event.getCreatedAt())
                        .read(false)
                        .build();

                notificationRepository.save(notif);
                messagingTemplate.convertAndSend(
                        "/topic/notifications/" + hostId,
                        notif
                );

                log.info("Sent HOST_REVIEWED notification to host {}", hostId);
            }

        } catch (Exception e) {
            log.error("Failed to process HostReviewedEvent", e);
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
