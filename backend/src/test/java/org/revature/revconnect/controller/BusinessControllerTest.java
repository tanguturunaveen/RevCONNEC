package org.revature.revconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.revature.revconnect.dto.request.BusinessProfileRequest;
import org.revature.revconnect.dto.response.AnalyticsResponse;
import org.revature.revconnect.dto.response.BusinessProfileResponse;
import org.revature.revconnect.dto.response.CommentResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.enums.BusinessCategory;
import org.revature.revconnect.service.BusinessService;

import org.revature.revconnect.service.InteractionService;
import org.revature.revconnect.service.PostService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BusinessController.class)
@AutoConfigureMockMvc(addFilters = false) // Bypasses security filters for unit testing controllers
public class BusinessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BusinessService businessService;

    @MockBean
    private InteractionService interactionService;

    @MockBean
    private PostService postService;

    @MockBean
    private org.revature.revconnect.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void createProfile_ShouldReturnCreated() throws Exception {
        BusinessProfileRequest request = new BusinessProfileRequest();
        request.setBusinessName("Test Business");
        request.setCategory(BusinessCategory.TECHNOLOGY);

        BusinessProfileResponse response = new BusinessProfileResponse();
        response.setBusinessName("Test Business");

        when(businessService.createBusinessProfile(any(BusinessProfileRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/business/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Business profile created successfully"))
                .andExpect(jsonPath("$.data.businessName").value("Test Business"));
    }

    @Test
    void getMyProfile_ShouldReturnOk() throws Exception {
        BusinessProfileResponse response = new BusinessProfileResponse();
        when(businessService.getMyBusinessProfile()).thenReturn(response);

        mockMvc.perform(get("/api/business/profile/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getProfile_ShouldReturnOk() throws Exception {
        BusinessProfileResponse response = new BusinessProfileResponse();
        when(businessService.getBusinessProfile(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/business/profile/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void updateProfile_ShouldReturnOk() throws Exception {
        BusinessProfileRequest request = new BusinessProfileRequest();
        request.setBusinessName("Updated Business");
        request.setCategory(BusinessCategory.TECHNOLOGY);

        BusinessProfileResponse response = new BusinessProfileResponse();
        response.setBusinessName("Updated Business");

        when(businessService.updateBusinessProfile(any(BusinessProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/business/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Business profile updated successfully"))
                .andExpect(jsonPath("$.data.businessName").value("Updated Business"));
    }

    @Test
    void deleteProfile_ShouldReturnOk() throws Exception {
        doNothing().when(businessService).deleteBusinessProfile();

        mockMvc.perform(delete("/api/business/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Business profile deleted successfully"));
    }

    @Test
    void getByCategory_ShouldReturnPagedResponse() throws Exception {
        PagedResponse<BusinessProfileResponse> response = new PagedResponse<>(
                Collections.singletonList(new BusinessProfileResponse()), 1, 1, 1, 1, true, true);
        when(businessService.getBusinessesByCategory(any(BusinessCategory.class), anyInt(), anyInt()))
                .thenReturn(response);

        mockMvc.perform(get("/api/business/category/{category}", "TECHNOLOGY")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void searchBusinesses_ShouldReturnPagedResponse() throws Exception {
        PagedResponse<BusinessProfileResponse> response = new PagedResponse<>(
                Collections.singletonList(new BusinessProfileResponse()), 1, 1, 1, 1, true, true);
        when(businessService.searchBusinesses(anyString(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/business/search")
                        .param("query", "test")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getAnalytics_ShouldReturnOk() throws Exception {
        AnalyticsResponse response = new AnalyticsResponse();
        when(businessService.getAnalytics(anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/business/analytics")
                        .param("days", "30")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void recordView_ShouldReturnOk() throws Exception {
        doNothing().when(businessService).recordPostView(anyLong());

        mockMvc.perform(post("/api/business/posts/{postId}/view", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("View recorded"));
    }

    @Test
    void recordImpression_ShouldReturnOk() throws Exception {
        doNothing().when(businessService).recordPostImpression(anyLong());

        mockMvc.perform(post("/api/business/posts/{postId}/impression", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Impression recorded"));
    }

    @Test
    void replyToComment_ShouldReturnCreated() throws Exception {
        CommentResponse response = new CommentResponse();
        when(interactionService.addComment(anyLong(),
                any(org.revature.revconnect.dto.request.CommentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/business/posts/{postId}/comments/{commentId}/reply", 1L, 2L)
                        .param("message", "Test reply")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reply posted"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getShowcase_ShouldReturnOk() throws Exception {
        List<Map<String, Object>> response = Collections.singletonList(new HashMap<>());
        when(businessService.getShowcase()).thenReturn(response);

        mockMvc.perform(get("/api/business/showcase")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void addShowcaseItem_ShouldReturnCreated() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("title", "Test Item");

        List<Map<String, Object>> response = Collections.singletonList(new HashMap<>(request));
        when(businessService.addShowcaseItem(any())).thenReturn(response);

        mockMvc.perform(post("/api/business/showcase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Showcase item added"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void updateShowcaseItem_ShouldReturnOk() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("title", "Updated Item");

        List<Map<String, Object>> response = Collections.singletonList(new HashMap<>(request));
        when(businessService.updateShowcaseItem(anyInt(), any())).thenReturn(response);

        mockMvc.perform(put("/api/business/showcase/{index}", 0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Showcase item updated"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void removeShowcaseItem_ShouldReturnOk() throws Exception {
        List<Map<String, Object>> response = Collections.emptyList();
        when(businessService.removeShowcaseItem(anyInt())).thenReturn(response);

        mockMvc.perform(delete("/api/business/showcase/{index}", 0)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Showcase item removed"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getMyPage_ShouldReturnOk() throws Exception {
        BusinessProfileResponse response = new BusinessProfileResponse();
        when(businessService.getMyBusinessProfile()).thenReturn(response);

        mockMvc.perform(get("/api/business/pages/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getPage_ShouldReturnOk() throws Exception {
        BusinessProfileResponse response = new BusinessProfileResponse();
        when(businessService.getBusinessProfile(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/business/pages/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void updateMyPage_ShouldReturnOk() throws Exception {
        BusinessProfileRequest request = new BusinessProfileRequest();
        request.setBusinessName("Updated Page");
        request.setCategory(BusinessCategory.TECHNOLOGY);

        BusinessProfileResponse response = new BusinessProfileResponse();
        response.setBusinessName("Updated Page");

        when(businessService.updateBusinessProfile(any(BusinessProfileRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/business/pages/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Business page updated"))
                .andExpect(jsonPath("$.data.businessName").value("Updated Page"));
    }

    @Test
    void getPagePosts_ShouldReturnPagedResponse() throws Exception {
        PagedResponse<PostResponse> response = new PagedResponse<>(
                Collections.singletonList(new PostResponse()), 1, 1, 1, 1, true, true);
        when(postService.getUserPosts(anyLong(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/business/pages/{userId}/posts", 1L)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }
}
