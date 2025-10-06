package org.example.notification.controller;

import org.example.notification.dto.NotificationDto;
import org.example.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    private NotificationDto dto(UUID id, String msg, boolean read) {
        return new NotificationDto(id, null, msg, LocalDateTime.parse("2024-01-01T12:00:00"), read);
    }


    @Test
    @DisplayName("GET /api/notifications returns 200 with list for role guest")
    void getUserNotifications_ok_guest() throws Exception {
        UUID userId = UUID.randomUUID();
        var n1 = dto(UUID.randomUUID(), "hello", false);
        var n2 = dto(UUID.randomUUID(), "world", true);

        given(notificationService.getUserNotifications(eq(userId))).willReturn(List.of(n1, n2));

        mockMvc.perform(get("/api/notifications")
                        .with(jwt().jwt(j -> j.claim("sub", userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_guest"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)));
        verify(notificationService).getUserNotifications(eq(userId));
    }

    @Test
    @DisplayName("GET /api/notifications without JWT returns 401")
    void getUserNotifications_unauth_401() throws Exception {
        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/notifications forbidden for unsupported role (e.g., admin)")
    void getUserNotifications_wrongRole_403() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("GET /api/notifications/{id} returns 200 with DTO when notification belongs to user")
    void getNotificationById_ok() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        var n = dto(notifId, "message", false);

        given(notificationService.getNotificationById(eq(notifId), eq(userId))).willReturn(n);

        mockMvc.perform(get("/api/notifications/{id}", notifId)
                        .with(jwt().jwt(j -> j.claim("sub", userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_host"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notifId.toString()))
                .andExpect(jsonPath("$.message").value("message"));
        verify(notificationService).getNotificationById(eq(notifId), eq(userId));
    }


    @Test
    @DisplayName("GET /api/notifications/unread returns 200 with unread list")
    void getUnread_ok() throws Exception {
        UUID userId = UUID.randomUUID();
        var n1 = dto(UUID.randomUUID(), "u1", false);
        var n2 = dto(UUID.randomUUID(), "u2", false);

        given(notificationService.getUnreadNotifications(eq(userId))).willReturn(List.of(n1, n2));

        mockMvc.perform(get("/api/notifications/unread")
                        .with(jwt().jwt(j -> j.claim("sub", userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_guest"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        verify(notificationService).getUnreadNotifications(eq(userId));
    }


    @Test
    @DisplayName("PATCH /api/notifications/read-all marks all as read and returns 200")
    void markAllAsRead_ok() throws Exception {
        UUID userId = UUID.randomUUID();
        Mockito.doNothing().when(notificationService).markAllAsRead(eq(userId));

        mockMvc.perform(patch("/api/notifications/read-all")
                        .with(jwt().jwt(j -> j.claim("sub", userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_host")))
                        .with(csrf()))
                .andExpect(status().isOk());
        verify(notificationService).markAllAsRead(eq(userId));
    }


    @Test
    @DisplayName("PATCH /api/notifications/{id}/read marks one as read and returns 200")
    void markAsRead_ok() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        doNothing().when(notificationService).markAsRead(eq(notifId), eq(userId));

        mockMvc.perform(patch("/api/notifications/{id}/read", notifId)
                        .with(jwt().jwt(j -> j.claim("sub", userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_guest")))
                        .with(csrf()))
                .andExpect(status().isOk());
        verify(notificationService).markAsRead(eq(notifId), eq(userId));
    }


    @Test
    @DisplayName("PATCH /api/notifications/read marks a list of ids as read and returns 200")
    void markAsReadBulk_ok() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        doNothing().when(notificationService).markAsRead(eq(List.of(id1, id2)), eq(userId));

        mockMvc.perform(patch("/api/notifications/read")
                        .with(jwt().jwt(j -> j.claim("sub", userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_guest")))
                        .with(csrf())
                        .param("ids", id1.toString())
                        .param("ids", id2.toString()))
                .andExpect(status().isOk());

        verify(notificationService).markAsRead(eq(List.of(id1, id2)), eq(userId));
    }


    @Test
    @DisplayName("DELETE /api/notifications/{id} deletes and returns 204")
    void deleteNotification_ok() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID notifId = UUID.randomUUID();
        doNothing().when(notificationService).deleteNotification(eq(notifId), eq(userId));

        mockMvc.perform(delete("/api/notifications/{id}", notifId)
                        .with(jwt().jwt(j -> j.claim("sub", userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_host")))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(notificationService).deleteNotification(eq(notifId), eq(userId));
    }


    @Test
    @DisplayName("DELETE /api/notifications with ids deletes list and returns 204")
    void deleteNotifications_ok() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        doNothing().when(notificationService).deleteNotifications(eq(List.of(id1, id2)), eq(userId));

        mockMvc.perform(delete("/api/notifications")
                        .with(jwt().jwt(j -> j.claim("sub", userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_guest")))
                        .with(csrf())
                        .param("ids", id1.toString())
                        .param("ids", id2.toString()))
                .andExpect(status().isNoContent());

        verify(notificationService).deleteNotifications(eq(List.of(id1, id2)), eq(userId));
    }


    @Test
    @DisplayName("PATCH /api/notifications/read-all without JWT returns 401")
    void markAllAsRead_unauth_401() throws Exception {
        mockMvc.perform(patch("/api/notifications/read-all").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/notifications with wrong role returns 403")
    void getUserNotifications_wrongRole_403_again() throws Exception {
        mockMvc.perform(get("/api/notifications")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isForbidden());
    }
}
