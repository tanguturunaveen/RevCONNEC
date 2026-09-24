package org.revature.revconnect.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.revature.revconnect.service.AnalyticsService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc(addFilters = false) // Bypasses security filters for unit testing controllers
public class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private org.revature.revconnect.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getOverview_ShouldReturnOk() throws Exception {
        Map<String, Object> map = new HashMap<>();
        when(analyticsService.getOverview()).thenReturn(map);

        mockMvc.perform(get("/api/analytics/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getProfileViews_ShouldReturnOk() throws Exception {
        List<Map<String, Object>> list = Collections.singletonList(new HashMap<>());
        when(analyticsService.getProfileViews(anyInt())).thenReturn(list);

        mockMvc.perform(get("/api/analytics/profile-views")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getPostPerformance_ShouldReturnOk() throws Exception {
        List<Map<String, Object>> list = Collections.singletonList(new HashMap<>());
        when(analyticsService.getPostPerformance(anyInt())).thenReturn(list);

        mockMvc.perform(get("/api/analytics/post-performance")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getPostAnalytics_ShouldReturnOk() throws Exception {
        Map<String, Object> map = new HashMap<>();
        when(analyticsService.getPostAnalytics(anyLong())).thenReturn(map);

        mockMvc.perform(get("/api/analytics/posts/{postId}/analytics", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getFollowerGrowth_ShouldReturnOk() throws Exception {
        List<Map<String, Object>> list = Collections.singletonList(new HashMap<>());
        when(analyticsService.getFollowerGrowth(anyInt())).thenReturn(list);

        mockMvc.perform(get("/api/analytics/followers/growth")
                        .param("days", "30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getEngagement_ShouldReturnOk() throws Exception {
        Map<String, Object> map = new HashMap<>();
        when(analyticsService.getEngagement(anyInt())).thenReturn(map);

        mockMvc.perform(get("/api/analytics/engagement")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getAudienceDemographics_ShouldReturnOk() throws Exception {
        Map<String, Object> map = new HashMap<>();
        when(analyticsService.getAudienceDemographics()).thenReturn(map);

        mockMvc.perform(get("/api/analytics/audience")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getReach_ShouldReturnOk() throws Exception {
        List<Map<String, Object>> list = Collections.singletonList(new HashMap<>());
        when(analyticsService.getReach(anyInt())).thenReturn(list);

        mockMvc.perform(get("/api/analytics/reach")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getImpressions_ShouldReturnOk() throws Exception {
        List<Map<String, Object>> list = Collections.singletonList(new HashMap<>());
        when(analyticsService.getImpressions(anyInt())).thenReturn(list);

        mockMvc.perform(get("/api/analytics/impressions")
                        .param("days", "7")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getBestTimeToPost_ShouldReturnOk() throws Exception {
        List<Map<String, Object>> list = Collections.singletonList(new HashMap<>());
        when(analyticsService.getBestTimeToPost()).thenReturn(list);

        mockMvc.perform(get("/api/analytics/best-time")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getTopPosts_ShouldReturnOk() throws Exception {
        List<Map<String, Object>> list = Collections.singletonList(new HashMap<>());
        when(analyticsService.getTopPosts(anyInt())).thenReturn(list);

        mockMvc.perform(get("/api/analytics/top-posts")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getHashtagPerformance_ShouldReturnOk() throws Exception {
        List<Map<String, Object>> list = Collections.singletonList(new HashMap<>());
        when(analyticsService.getHashtagPerformance()).thenReturn(list);

        mockMvc.perform(get("/api/analytics/hashtag-performance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getContentTypePerformance_ShouldReturnOk() throws Exception {
        Map<String, Object> map = new HashMap<>();
        when(analyticsService.getContentTypePerformance()).thenReturn(map);

        mockMvc.perform(get("/api/analytics/content-type")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void exportAnalytics_ShouldReturnOk() throws Exception {
        Map<String, String> map = new HashMap<>();
        when(analyticsService.exportAnalytics(anyInt(), anyString())).thenReturn(map);

        mockMvc.perform(get("/api/analytics/export")
                        .param("days", "30")
                        .param("format", "csv")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }
}
