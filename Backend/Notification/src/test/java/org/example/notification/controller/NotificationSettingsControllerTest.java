package org.example.notification.controller;

import org.example.notification.dto.NotificationSettingsDto;
import org.example.notification.model.NotificationType;
import org.example.notification.service.NotificationSettingsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationSettingsControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean
    private NotificationSettingsService settingsService;

    private NotificationSettingsDto dto(UUID id, NotificationType t, boolean enabled) {
        return NotificationSettingsDto.builder()
                .id(id)
                .notifType(t)
                .enabled(enabled)
                .build();
    }


    @Test
    @DisplayName("GET /api/notification-settings returns 200 with list for role guest")
    void getSettings_ok_guest() throws Exception {
        UUID userId = UUID.randomUUID();
        var s1 = dto(UUID.randomUUID(), NotificationType.RESERVATION_CREATED, true);
        var s2 = dto(UUID.randomUUID(), NotificationType.RESERVATION_CANCELED, false);

        given(settingsService.getUserSettings(eq(userId))).willReturn(List.of(s1, s2));

        mockMvc.perform(get("/api/notification-settings")
                        .with(jwt().jwt(j -> j.claim("sub", userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_guest"))))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)));

        verify(settingsService).getUserSettings(eq(userId));
    }

    @Test
    @DisplayName("GET /api/notification-settings without JWT returns 401")
    void getSettings_unauth_401() throws Exception {
        mockMvc.perform(get("/api/notification-settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/notification-settings forbidden for wrong role")
    void getSettings_wrongRole_403() throws Exception {
        mockMvc.perform(get("/api/notification-settings")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin"))))
                .andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("PATCH /api/notification-settings/{type}?enabled=bool updates and returns DTO (guest)")
    void updateSetting_ok_guest() throws Exception {
        UUID userId = UUID.randomUUID();
        NotificationType type = NotificationType.RESERVATION_CREATED;

        var updated = dto(UUID.randomUUID(), type, true);
        given(settingsService.updateSetting(eq(userId), eq(type), eq(true))).willReturn(updated);

        mockMvc.perform(patch("/api/notification-settings/{type}", type)
                        .with(jwt().jwt(j -> j.claim("sub", userId.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_guest")))
                        .with(csrf())
                        .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notifType").value(type.name()))
                .andExpect(jsonPath("$.enabled").value(true));

        verify(settingsService).updateSetting(eq(userId), eq(type), eq(true));
    }

    @Test
    @DisplayName("PATCH /api/notification-settings/{type} without JWT returns 401; wrong role returns 403")
    void updateSetting_security() throws Exception {
        NotificationType type = NotificationType.RESERVATION_CANCELED;

        mockMvc.perform(patch("/api/notification-settings/{type}", type)
                        .with(csrf())
                        .param("enabled", "false"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/notification-settings/{type}", type)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_admin")))
                        .with(csrf())
                        .param("enabled", "false"))
                .andExpect(status().isForbidden());
    }


    @Test
    @DisplayName("POST /api/notification-settings/init initializes settings and returns 200")
    void initUserSettings_ok() throws Exception {
        UUID userId = UUID.randomUUID();
        String role = "guest";

        doNothing().when(settingsService).initUserSettings(eq(userId), anyList());

        mockMvc.perform(post("/api/notification-settings/init")
                        .with(jwt())
                        .with(csrf())
                        .param("userId", userId.toString())
                        .param("role", role))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> rolesCap = ArgumentCaptor.forClass(List.class);
        verify(settingsService).initUserSettings(eq(userId), rolesCap.capture());
        assertThat(rolesCap.getValue()).containsExactly(role);
    }

    @Test
    @DisplayName("POST /api/notification-settings/init without JWT is allowed and returns 200")
    void initUserSettings_public_200() throws Exception {
        UUID userId = UUID.randomUUID();
        String role = "guest";

        doNothing().when(settingsService).initUserSettings(eq(userId), anyList());

        mockMvc.perform(post("/api/notification-settings/init")
                        .with(csrf())
                        .param("userId", userId.toString())
                        .param("role", role))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> rolesCap = ArgumentCaptor.forClass(List.class);
        verify(settingsService).initUserSettings(eq(userId), rolesCap.capture());
        assertThat(rolesCap.getValue()).containsExactly(role);
    }

}
