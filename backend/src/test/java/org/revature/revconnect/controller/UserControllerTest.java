package org.revature.revconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.revature.revconnect.dto.request.ProfileUpdateRequest;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.UserResponse;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private org.revature.revconnect.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getMyProfile_ShouldReturnOk() throws Exception {
        UserResponse response = new UserResponse();
        response.setUsername("testuser");
        when(userService.getMyProfile()).thenReturn(response);

        mockMvc.perform(get("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));
    }

    @Test
    void getUserById_ShouldReturnOk() throws Exception {
        UserResponse response = new UserResponse();
        when(userService.getUserById(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/users/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getUserByUsername_ShouldReturnOk() throws Exception {
        UserResponse response = new UserResponse();
        when(userService.getUserByUsername(anyString())).thenReturn(response);

        mockMvc.perform(get("/api/users/username/{username}", "testuser")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void updateProfile_ShouldReturnOk() throws Exception {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        UserResponse response = new UserResponse();
        response.setUsername("testuser");
        when(userService.updateProfile(any(ProfileUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"));
    }

    @Test
    void searchUsers_ShouldReturnOk() throws Exception {
        PagedResponse<UserResponse> response = new PagedResponse<>(
                Collections.singletonList(new UserResponse()), 1, 1, 1, 1, true, true);
        when(userService.searchUsers(anyString(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/users/search")
                        .param("query", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void updatePrivacy_ShouldReturnOk() throws Exception {
        UserResponse response = new UserResponse();
        when(userService.updatePrivacy(any(Privacy.class))).thenReturn(response);

        mockMvc.perform(patch("/api/users/me/privacy")
                        .param("privacy", "PUBLIC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void blockUser_ShouldReturnOk() throws Exception {
        doNothing().when(userService).blockUser(anyLong());

        mockMvc.perform(post("/api/users/{userId}/block", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User blocked successfully"));
    }

    @Test
    void reportUser_ShouldReturnOk() throws Exception {
        doNothing().when(userService).reportUser(anyLong(), anyString());

        mockMvc.perform(post("/api/users/{userId}/report", 1L)
                        .param("reason", "spam")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User reported successfully"));
    }
}
