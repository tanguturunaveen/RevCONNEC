package org.revature.revconnect.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.revature.revconnect.dto.response.ApiResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.dto.response.UserResponse;
import org.revature.revconnect.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @MockBean
    private org.revature.revconnect.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void searchAll_ShouldReturnOk() throws Exception {
        Map<String, Object> response = Map.of("users", Collections.emptyList());
        when(searchService.searchAll(anyString(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/search/all")
                        .param("query", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void searchUsers_ShouldReturnOk() throws Exception {
        PagedResponse<UserResponse> response = new PagedResponse<>(
                Collections.singletonList(new UserResponse()), 1, 1, 1, 1, true, true);
        when(searchService.searchUsers(anyString(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/search/users")
                        .param("query", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void searchPosts_ShouldReturnOk() throws Exception {
        PagedResponse<PostResponse> response = new PagedResponse<>(
                Collections.singletonList(new PostResponse()), 1, 1, 1, 1, true, true);
        when(searchService.searchPosts(anyString(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/search/posts")
                        .param("query", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getRecentSearches_ShouldReturnOk() throws Exception {
        when(searchService.getRecentSearches()).thenReturn(Collections.singletonList("test"));

        mockMvc.perform(get("/api/search/recent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void clearRecentSearches_ShouldReturnOk() throws Exception {
        doNothing().when(searchService).clearRecentSearches();

        mockMvc.perform(delete("/api/search/recent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Recent searches cleared"));
    }

    @Test
    void removeRecentSearch_ShouldReturnOk() throws Exception {
        doNothing().when(searchService).removeRecentSearch(anyString());

        mockMvc.perform(delete("/api/search/recent/{query}", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Search removed"));
    }

    @Test
    void getSearchSuggestions_ShouldReturnOk() throws Exception {
        when(searchService.getSearchSuggestions(anyString())).thenReturn(Collections.singletonList("test"));

        mockMvc.perform(get("/api/search/suggestions")
                        .param("query", "te")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getTrendingSearches_ShouldReturnOk() throws Exception {
        when(searchService.getTrendingSearches()).thenReturn(Collections.singletonList("test"));

        mockMvc.perform(get("/api/search/trending")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }
}
