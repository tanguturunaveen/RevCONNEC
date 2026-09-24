package org.revature.revconnect.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.revature.revconnect.model.Story;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.UserRepository;
import org.revature.revconnect.service.AuthService;
import org.revature.revconnect.service.StoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StoryController.class)
@AutoConfigureMockMvc(addFilters = false)
public class StoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StoryService storyService;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private org.revature.revconnect.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void createStory_ShouldReturnCreated() throws Exception {
        Story story = new Story();
        story.setId(1L);
        when(storyService.createStory(anyString(), any())).thenReturn(story);

        mockMvc.perform(post("/api/stories")
                        .param("mediaUrl", "http://example.com/story.jpg")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Story created"));
    }

    @Test
    void getMyStories_ShouldReturnOk() throws Exception {
        User user = new User();
        user.setId(1L);
        when(authService.getCurrentUser()).thenReturn(user);
        when(storyService.getActiveStories(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/stories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getStoriesFeed_ShouldReturnOk() throws Exception {
        when(storyService.getStoriesFeedForCurrentUser()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/stories/feed")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getUserStories_ShouldReturnOk() throws Exception {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(storyService.getActiveStories(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/stories/user/{userId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getStory_ShouldReturnOk() throws Exception {
        User user = new User();
        user.setId(1L);
        Story story = new Story();
        story.setId(1L);
        story.setUser(user);
        when(storyService.getStory(anyLong())).thenReturn(story);

        mockMvc.perform(get("/api/stories/{storyId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void deleteStory_ShouldReturnOk() throws Exception {
        doNothing().when(storyService).deleteStory(anyLong());

        mockMvc.perform(delete("/api/stories/{storyId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Story deleted"));
    }

    @Test
    void viewStory_ShouldReturnOk() throws Exception {
        doNothing().when(storyService).incrementViewCount(anyLong());

        mockMvc.perform(post("/api/stories/{storyId}/view", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Story viewed"));
    }

    @Test
    void reactToStory_ShouldReturnOk() throws Exception {
        doNothing().when(storyService).reactToStory(anyLong(), anyString());

        mockMvc.perform(post("/api/stories/{storyId}/react", 1L)
                        .param("reaction", "like")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reaction added"));
    }
}
