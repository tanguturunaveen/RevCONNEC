package org.revature.revconnect.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.revature.revconnect.dto.response.BookmarkResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.service.BookmarkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BookmarkController.class)
@AutoConfigureMockMvc(addFilters = false) // Bypasses security filters for unit testing controllers
public class BookmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookmarkService bookmarkService;

    @MockBean
    private org.revature.revconnect.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void bookmarkPost_ShouldReturnCreated() throws Exception {
        doNothing().when(bookmarkService).bookmarkPost(anyLong());

        mockMvc.perform(post("/api/bookmarks/posts/{postId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Post bookmarked successfully"));
    }

    @Test
    void removeBookmark_ShouldReturnOk() throws Exception {
        doNothing().when(bookmarkService).removeBookmark(anyLong());

        mockMvc.perform(delete("/api/bookmarks/posts/{postId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Bookmark removed successfully"));
    }

    @Test
    void getBookmarks_ShouldReturnPagedResponse() throws Exception {
        PagedResponse<BookmarkResponse> response = new PagedResponse<>(
                Collections.singletonList(new BookmarkResponse()), 1, 1, 1, 1, true, true);
        when(bookmarkService.getBookmarks(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/bookmarks")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void isBookmarked_ShouldReturnBoolean() throws Exception {
        when(bookmarkService.isBookmarked(anyLong())).thenReturn(true);

        mockMvc.perform(get("/api/bookmarks/posts/{postId}/status", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));
    }
}
