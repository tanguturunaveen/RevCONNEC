package org.revature.revconnect.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.revature.revconnect.dto.response.ApiResponse;
import org.revature.revconnect.dto.response.NotificationResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private org.revature.revconnect.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getNotifications_ShouldReturnOk() throws Exception {
        PagedResponse<NotificationResponse> response = new PagedResponse<>(
                Collections.singletonList(new NotificationResponse()), 1, 1, 1, 1, true, true);
        when(notificationService.getNotifications(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/notifications")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getUnreadNotifications_ShouldReturnOk() throws Exception {
        PagedResponse<NotificationResponse> response = new PagedResponse<>(
                Collections.singletonList(new NotificationResponse()), 1, 1, 1, 1, true, true);
        when(notificationService.getUnreadNotifications(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/notifications/unread")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getUnreadCount_ShouldReturnOk() throws Exception {
        when(notificationService.getUnreadCount()).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(5));
    }

    @Test
    void markAsRead_ShouldReturnOk() throws Exception {
        doNothing().when(notificationService).markAsRead(anyLong());

        mockMvc.perform(patch("/api/notifications/{notificationId}/read", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Notification marked as read"));
    }

    @Test
    void markAllAsRead_ShouldReturnOk() throws Exception {
        when(notificationService.markAllAsRead()).thenReturn(5);

        mockMvc.perform(patch("/api/notifications/read-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("All notifications marked as read"))
                .andExpect(jsonPath("$.data.markedCount").value(5));
    }

    @Test
    void deleteNotification_ShouldReturnOk() throws Exception {
        doNothing().when(notificationService).deleteNotification(anyLong());

        mockMvc.perform(delete("/api/notifications/{notificationId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Notification deleted successfully"));
    }
}
