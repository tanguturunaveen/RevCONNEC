package org.revature.revconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.revature.revconnect.dto.response.ApiResponse;
import org.revature.revconnect.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettingsController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SettingsService settingsService;

    @MockBean
    private org.revature.revconnect.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getSettings_ShouldReturnOk() throws Exception {
        Map<String, Object> settings = new HashMap<>();
        when(settingsService.getSettings()).thenReturn(settings);

        mockMvc.perform(get("/api/settings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void updateSettings_ShouldReturnOk() throws Exception {
        Map<String, Object> settings = new HashMap<>();
        when(settingsService.updateSettings(anyMap())).thenReturn(settings);

        mockMvc.perform(put("/api/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settings)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Settings updated"));
    }

    @Test
    void getPrivacySettings_ShouldReturnOk() throws Exception {
        Map<String, Object> settings = new HashMap<>();
        when(settingsService.getPrivacySettings()).thenReturn(settings);

        mockMvc.perform(get("/api/settings/privacy")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void updatePrivacySettings_ShouldReturnOk() throws Exception {
        Map<String, Object> settings = new HashMap<>();
        doNothing().when(settingsService).updatePrivacySettings(anyMap());

        mockMvc.perform(put("/api/settings/privacy")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settings)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Privacy settings updated"));
    }

    @Test
    void changePassword_ShouldReturnOk() throws Exception {
        doNothing().when(settingsService).changePassword(anyString(), anyString());

        mockMvc.perform(post("/api/settings/password/change")
                        .param("currentPassword", "old")
                        .param("newPassword", "new")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed"));
    }

    @Test
    void deleteAccount_ShouldReturnOk() throws Exception {
        doNothing().when(settingsService).deleteAccount(anyString());

        mockMvc.perform(delete("/api/settings/account")
                        .param("password", "pass")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Account deleted"));
    }
}
