package org.revature.revconnect.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.model.Hashtag;
import org.revature.revconnect.service.HashtagService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HashtagController.class)
@AutoConfigureMockMvc(addFilters = false) // Bypasses security filters for unit testing controllers
public class HashtagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HashtagService hashtagService;

    @MockBean
    private org.revature.revconnect.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getTrendingHashtags_ShouldReturnOk() throws Exception {
        Hashtag hashtag = new Hashtag();
        hashtag.setName("trending");
        when(hashtagService.getTrending(anyInt())).thenReturn(Collections.singletonList(hashtag));

        mockMvc.perform(get("/api/hashtags/trending")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getHashtag_ShouldReturnOk() throws Exception {
        Hashtag hashtag = new Hashtag();
        hashtag.setName("trending");
        when(hashtagService.getHashtag(anyString())).thenReturn(hashtag);

        mockMvc.perform(get("/api/hashtags/{hashtag}", "trending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getPostsByHashtag_ShouldReturnPagedResponse() throws Exception {
        PagedResponse<PostResponse> response = new PagedResponse<>(
                Collections.singletonList(new PostResponse()), 1, 1, 1, 1, true, true);
        when(hashtagService.getPostsByHashtag(anyString(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/hashtags/{hashtag}/posts", "trending")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void followHashtag_ShouldReturnOk() throws Exception {
        doNothing().when(hashtagService).followHashtag(anyString());

        mockMvc.perform(post("/api/hashtags/{hashtag}/follow", "trending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Hashtag followed"));
    }

    @Test
    void unfollowHashtag_ShouldReturnOk() throws Exception {
        doNothing().when(hashtagService).unfollowHashtag(anyString());

        mockMvc.perform(delete("/api/hashtags/{hashtag}/follow", "trending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Hashtag unfollowed"));
    }

    @Test
    void getFollowedHashtags_ShouldReturnOk() throws Exception {
        List<Map<String, Object>> response = Collections.singletonList(new HashMap<>());
        when(hashtagService.getFollowedHashtagsView()).thenReturn(response);

        mockMvc.perform(get("/api/hashtags/following")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getSuggestedHashtags_ShouldReturnOk() throws Exception {
        Hashtag hashtag = new Hashtag();
        hashtag.setName("trending");
        when(hashtagService.getSuggestedHashtags(anyInt())).thenReturn(Collections.singletonList(hashtag));

        mockMvc.perform(get("/api/hashtags/suggested")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void searchHashtags_ShouldReturnOk() throws Exception {
        Hashtag hashtag = new Hashtag();
        hashtag.setName("trending");
        when(hashtagService.search(anyString())).thenReturn(Collections.singletonList(hashtag));

        mockMvc.perform(get("/api/hashtags/search")
                        .param("query", "trend")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void autocompleteHashtags_ShouldReturnOk() throws Exception {
        Hashtag hashtag = new Hashtag();
        hashtag.setName("trending");
        when(hashtagService.search(anyString())).thenReturn(Collections.singletonList(hashtag));

        mockMvc.perform(get("/api/hashtags/autocomplete")
                        .param("prefix", "tre")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getRelatedHashtags_ShouldReturnOk() throws Exception {
        Hashtag hashtag = new Hashtag();
        hashtag.setName("trending");
        when(hashtagService.search(anyString())).thenReturn(Collections.singletonList(hashtag));

        mockMvc.perform(get("/api/hashtags/{hashtag}/related", "trending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }
}
