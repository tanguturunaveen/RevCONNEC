package org.revature.revconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.revature.revconnect.dto.request.CommentRequest;
import org.revature.revconnect.dto.request.ShareRequest;
import org.revature.revconnect.dto.response.CommentResponse;
import org.revature.revconnect.dto.response.LikeResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.ShareResponse;
import org.revature.revconnect.service.InteractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InteractionController.class)
@AutoConfigureMockMvc(addFilters = false) // Bypasses security filters for unit testing controllers
public class InteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private InteractionService interactionService;

    @MockBean
    private org.revature.revconnect.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void likePost_ShouldReturnCreated() throws Exception {
        doNothing().when(interactionService).likePost(anyLong());

        mockMvc.perform(post("/api/posts/{postId}/like", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Post liked successfully"));
    }

    @Test
    void unlikePost_ShouldReturnOk() throws Exception {
        doNothing().when(interactionService).unlikePost(anyLong());

        mockMvc.perform(delete("/api/posts/{postId}/like", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Post unliked successfully"));
    }

    @Test
    void getPostLikes_ShouldReturnPagedResponse() throws Exception {
        PagedResponse<LikeResponse> response = new PagedResponse<>(
                Collections.singletonList(new LikeResponse()), 1, 1, 1, 1, true, true);
        when(interactionService.getPostLikes(anyLong(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/posts/{postId}/likes", 1L)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void hasUserLikedPost_ShouldReturnBoolean() throws Exception {
        when(interactionService.hasUserLikedPost(anyLong())).thenReturn(true);

        mockMvc.perform(get("/api/posts/{postId}/liked", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void addComment_ShouldReturnCreated() throws Exception {
        CommentRequest request = new CommentRequest();
        request.setContent("Great post!");

        CommentResponse response = new CommentResponse();
        response.setId(1L);

        when(interactionService.addComment(anyLong(), any(CommentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/posts/{postId}/comments", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Comment added successfully"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getPostComments_ShouldReturnPagedResponse() throws Exception {
        PagedResponse<CommentResponse> response = new PagedResponse<>(
                Collections.singletonList(new CommentResponse()), 1, 1, 1, 1, true, true);
        when(interactionService.getPostComments(anyLong(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/posts/{postId}/comments", 1L)
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void deleteComment_ShouldReturnOk() throws Exception {
        doNothing().when(interactionService).deleteComment(anyLong());

        mockMvc.perform(delete("/api/comments/{commentId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Comment deleted successfully"));
    }

    @Test
    void sharePost_ShouldReturnCreated() throws Exception {
        ShareRequest request = new ShareRequest();
        request.setComment("Check this out!");

        ShareResponse response = new ShareResponse();

        when(interactionService.sharePost(anyLong(), any())).thenReturn(response);

        mockMvc.perform(post("/api/posts/{postId}/share", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Post shared successfully"))
                .andExpect(jsonPath("$.data").exists());
    }
}
