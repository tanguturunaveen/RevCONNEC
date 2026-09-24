package org.revature.revconnect.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.revature.revconnect.dto.response.ConnectionResponse;
import org.revature.revconnect.dto.response.ConnectionStatsResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.service.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConnectionController.class)
@AutoConfigureMockMvc(addFilters = false) // Bypasses security filters for unit testing controllers
public class ConnectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConnectionService connectionService;

    @MockBean
    private org.revature.revconnect.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void followUser_ShouldReturnCreated() throws Exception {
        doNothing().when(connectionService).followUser(anyLong());

        mockMvc.perform(post("/api/users/{userId}/follow", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Successfully followed user"));
    }

    @Test
    void unfollowUser_ShouldReturnOk() throws Exception {
        doNothing().when(connectionService).unfollowUser(anyLong());

        mockMvc.perform(delete("/api/users/{userId}/follow", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Successfully unfollowed user"));
    }

    @Test
    void getFollowers_ShouldReturnPagedResponse() throws Exception {
        PagedResponse<ConnectionResponse> response = new PagedResponse<>(
                Collections.singletonList(new ConnectionResponse()), 1, 1, 1, 1, true, true);
        when(connectionService.getFollowers(anyLong(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/users/{userId}/followers", 1L)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getFollowing_ShouldReturnPagedResponse() throws Exception {
        PagedResponse<ConnectionResponse> response = new PagedResponse<>(
                Collections.singletonList(new ConnectionResponse()), 1, 1, 1, 1, true, true);
        when(connectionService.getFollowing(anyLong(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/users/{userId}/following", 1L)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getConnectionStats_ShouldReturnOk() throws Exception {
        ConnectionStatsResponse response = new ConnectionStatsResponse();
        when(connectionService.getConnectionStats(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/users/{userId}/connection-stats", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getPendingRequests_ShouldReturnPagedResponse() throws Exception {
        PagedResponse<ConnectionResponse> response = new PagedResponse<>(
                Collections.singletonList(new ConnectionResponse()), 1, 1, 1, 1, true, true);
        when(connectionService.getPendingRequests(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/connections/pending")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getSentPendingRequests_ShouldReturnPagedResponse() throws Exception {
        PagedResponse<ConnectionResponse> response = new PagedResponse<>(
                Collections.singletonList(new ConnectionResponse()), 1, 1, 1, 1, true, true);
        when(connectionService.getSentPendingRequests(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/connections/pending/sent")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void acceptRequest_ShouldReturnOk() throws Exception {
        doNothing().when(connectionService).acceptRequest(anyLong());

        mockMvc.perform(post("/api/connections/{connectionId}/accept", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Connection request accepted"));
    }

    @Test
    void rejectRequest_ShouldReturnOk() throws Exception {
        doNothing().when(connectionService).rejectRequest(anyLong());

        mockMvc.perform(delete("/api/connections/{connectionId}/reject", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Connection request rejected"));
    }

    @Test
    void isFollowing_ShouldReturnBoolean() throws Exception {
        when(connectionService.isFollowing(anyLong())).thenReturn(true);

        mockMvc.perform(get("/api/users/{userId}/is-following", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void removeConnection_ShouldReturnOk() throws Exception {
        doNothing().when(connectionService).removeConnection(anyLong());

        mockMvc.perform(delete("/api/users/{userId}/connection", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Connection removed"));
    }
}
