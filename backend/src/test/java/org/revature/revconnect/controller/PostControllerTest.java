package org.revature.revconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.revature.revconnect.dto.request.PostRequest;
import org.revature.revconnect.dto.request.SchedulePostRequest;
import org.revature.revconnect.dto.response.ApiResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.enums.PostType;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void createPost_ShouldReturnCreated() throws Exception {
        PostRequest request = new PostRequest();
        request.setContent("Test content");
        request.setPostType(PostType.TEXT);

        PostResponse response = new PostResponse();
        response.setId(1L);

        when(postService.createPost(any(PostRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Post created successfully"));
    }

    @Test
    void getPost_ShouldReturnOk() throws Exception {
        PostResponse response = new PostResponse();
        response.setId(1L);
        when(postService.getPostById(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/posts/{postId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getMyPosts_ShouldReturnOk() throws Exception {
        PagedResponse<PostResponse> response = new PagedResponse<>(
                Collections.singletonList(new PostResponse()), 1, 1, 1, 1, true, true);
        when(postService.getMyPosts(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/posts/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getUserPosts_ShouldReturnOk() throws Exception {
        PagedResponse<PostResponse> response = new PagedResponse<>(
                Collections.singletonList(new PostResponse()), 1, 1, 1, 1, true, true);
        when(postService.getUserPosts(anyLong(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/posts/user/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void updatePost_ShouldReturnOk() throws Exception {
        PostRequest request = new PostRequest();
        request.setContent("Updated content");

        PostResponse response = new PostResponse();
        response.setId(1L);

        when(postService.updatePost(anyLong(), any(PostRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/posts/{postId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Post updated successfully"));
    }

    @Test
    void deletePost_ShouldReturnOk() throws Exception {
        doNothing().when(postService).deletePost(anyLong());

        mockMvc.perform(delete("/api/posts/{postId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Post deleted successfully"));
    }

    @Test
    void togglePinPost_ShouldReturnOk() throws Exception {
        PostResponse response = new PostResponse();
        response.setPinned(true);
        when(postService.togglePinPost(anyLong())).thenReturn(response);

        mockMvc.perform(patch("/api/posts/{postId}/pin", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Post pinned"));
    }

    @Test
    void schedulePost_ShouldReturnCreated() throws Exception {
        SchedulePostRequest request = new SchedulePostRequest();
        request.setContent("Scheduled test content");
        request.setPublishAt(LocalDateTime.now().plusDays(1));
        Map<String, Object> response = Map.of("id", 1L);
        when(postService.schedulePost(any(SchedulePostRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/posts/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Post scheduled"));
    }
}
