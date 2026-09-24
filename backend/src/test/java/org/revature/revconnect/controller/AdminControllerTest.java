package org.revature.revconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.UserResponse;
import org.revature.revconnect.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false) // Bypasses security filters for unit testing controllers
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private org.revature.revconnect.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
        // Any setup if needed
    }

    @Test
    void getAllUsers_ShouldReturnListOfUsers() throws Exception {
        // Arrange
        PagedResponse<UserResponse> mockResponse = new PagedResponse<>(
                Collections.singletonList(new UserResponse()), 1, 1, 1, 1, true, true);
        when(adminService.getAllUsers(anyInt(), anyInt())).thenReturn(mockResponse);

        // Act & Assert
        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void suspendUser_ShouldReturnSuccessMessage() throws Exception {
        // Act & Assert
        mockMvc.perform(patch("/api/admin/users/{userId}/suspend", 1L)
                        .param("reason", "Violation of terms")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User suspended"));
    }

    @Test
    void unsuspendUser_ShouldReturnSuccessMessage() throws Exception {
        // Act & Assert
        mockMvc.perform(patch("/api/admin/users/{userId}/unsuspend", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User unsuspended"));
    }

    @Test
    void deleteUser_ShouldReturnSuccessMessage() throws Exception {
        // Arrange
        doNothing().when(adminService).deleteUser(anyLong());

        // Act & Assert
        mockMvc.perform(delete("/api/admin/users/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User deleted"));
    }

    @Test
    void verifyUser_ShouldReturnSuccessMessage() throws Exception {
        // Arrange
        doNothing().when(adminService).verifyUser(anyLong());

        // Act & Assert
        mockMvc.perform(patch("/api/admin/users/{userId}/verify", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User verified"));
    }

    @Test
    void unverifyUser_ShouldReturnSuccessMessage() throws Exception {
        // Arrange
        doNothing().when(adminService).unverifyUser(anyLong());

        // Act & Assert
        mockMvc.perform(patch("/api/admin/users/{userId}/unverify", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User unverified"));
    }

    @Test
    void getReports_ShouldReturnReportsList() throws Exception {
        // Arrange
        List<Map<String, Object>> mockReports = Collections.singletonList(new HashMap<>());
        when(adminService.getReports(anyInt(), anyInt())).thenReturn(mockReports);

        // Act & Assert
        mockMvc.perform(get("/api/admin/reports")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getReport_ShouldReturnReportDetails() throws Exception {
        // Arrange
        Map<String, Object> mockReport = new HashMap<>();
        when(adminService.getReport(anyLong())).thenReturn(mockReport);

        // Act & Assert
        mockMvc.perform(get("/api/admin/reports/{reportId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void resolveReport_ShouldReturnSuccessMessage() throws Exception {
        // Act & Assert
        mockMvc.perform(patch("/api/admin/reports/{reportId}/resolve", 1L)
                        .param("action", "ban_user")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Report resolved"));
    }

    @Test
    void dismissReport_ShouldReturnSuccessMessage() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/api/admin/reports/{reportId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Report dismissed"));
    }

    @Test
    void getFlaggedPosts_ShouldReturnFlaggedPostsList() throws Exception {
        // Arrange
        List<Map<String, Object>> mockFlaggedPosts = Collections.singletonList(new HashMap<>());
        when(adminService.getFlaggedPosts()).thenReturn(mockFlaggedPosts);

        // Act & Assert
        mockMvc.perform(get("/api/admin/posts/flagged")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void deletePost_ShouldReturnSuccessMessage() throws Exception {
        // Arrange
        doNothing().when(adminService).deletePost(anyLong());

        // Act & Assert
        mockMvc.perform(delete("/api/admin/posts/{postId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Post deleted"));
    }

    @Test
    void getPlatformStats_ShouldReturnStats() throws Exception {
        // Arrange
        Map<String, Object> mockStats = new HashMap<>();
        mockStats.put("totalUsers", 100);
        when(adminService.getPlatformStats()).thenReturn(mockStats);

        // Act & Assert
        mockMvc.perform(get("/api/admin/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(100));
    }

    @Test
    void getUserStats_ShouldReturnUserStats() throws Exception {
        // Arrange
        Map<String, Object> mockStats = new HashMap<>();
        when(adminService.getUserStats()).thenReturn(mockStats);

        // Act & Assert
        mockMvc.perform(get("/api/admin/stats/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getPostStats_ShouldReturnPostStats() throws Exception {
        // Arrange
        Map<String, Object> mockStats = new HashMap<>();
        when(adminService.getPostStats()).thenReturn(mockStats);

        // Act & Assert
        mockMvc.perform(get("/api/admin/stats/posts")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getEngagementStats_ShouldReturnEngagementStats() throws Exception {
        // Arrange
        Map<String, Object> mockStats = new HashMap<>();
        when(adminService.getEngagementStats()).thenReturn(mockStats);

        // Act & Assert
        mockMvc.perform(get("/api/admin/stats/engagement")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getAuditLogs_ShouldReturnAuditLogs() throws Exception {
        // Arrange
        List<Map<String, Object>> mockLogs = Collections.singletonList(new HashMap<>());
        when(adminService.getAuditLogs(anyInt(), anyInt())).thenReturn(mockLogs);

        // Act & Assert
        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("page", "0")
                        .param("size", "50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }
}
