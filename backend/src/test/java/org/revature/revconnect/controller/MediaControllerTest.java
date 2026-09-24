package org.revature.revconnect.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.revature.revconnect.service.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MediaController.class)
@AutoConfigureMockMvc(addFilters = false) // Bypasses security filters for unit testing controllers
public class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaService mediaService;

    @MockBean
    private org.revature.revconnect.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void uploadFile_ShouldReturnCreated() throws Exception {
        Map<String, String> response = new HashMap<>();
        response.put("url", "http://example.com/image.jpg");
        when(mediaService.uploadFile(any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "image.jpg", MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes());

        mockMvc.perform(multipart("/api/media/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("File uploaded"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void uploadMultipleFiles_ShouldReturnCreated() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("url", "http://example.com/image.jpg");
        List<Map<String, String>> response = Collections.singletonList(map);
        when(mediaService.uploadMultipleFiles(any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("files", "image.jpg", MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes());

        mockMvc.perform(multipart("/api/media/upload/multiple").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Files uploaded"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void uploadProfilePicture_ShouldReturnOk() throws Exception {
        Map<String, String> response = new HashMap<>();
        response.put("url", "http://example.com/profile.jpg");
        when(mediaService.uploadProfilePicture(any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "profile.jpg", MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes());

        mockMvc.perform(multipart("/api/media/upload/profile-picture").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile picture updated"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void uploadCoverPhoto_ShouldReturnOk() throws Exception {
        Map<String, String> response = new HashMap<>();
        response.put("url", "http://example.com/cover.jpg");
        when(mediaService.uploadCoverPhoto(any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "cover.jpg", MediaType.IMAGE_JPEG_VALUE,
                "test image content".getBytes());

        mockMvc.perform(multipart("/api/media/upload/cover-photo").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cover photo updated"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void deleteMedia_ShouldReturnOk() throws Exception {
        doNothing().when(mediaService).deleteMedia(anyLong());

        mockMvc.perform(delete("/api/media/{mediaId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Media deleted"));
    }

    @Test
    void getMedia_ShouldReturnOk() throws Exception {
        Map<String, Object> response = new HashMap<>();
        when(mediaService.getMedia(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/media/{mediaId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getMyMedia_ShouldReturnOk() throws Exception {
        List<Map<String, Object>> response = Collections.singletonList(new HashMap<>());
        when(mediaService.getMyMedia(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/media/my")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void uploadVideo_ShouldReturnCreated() throws Exception {
        Map<String, String> response = new HashMap<>();
        response.put("url", "http://example.com/video.mp4");
        when(mediaService.uploadVideo(any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4",
                "test video content".getBytes());

        mockMvc.perform(multipart("/api/media/upload/video").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Video uploaded"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getThumbnail_ShouldReturnOk() throws Exception {
        Map<String, String> response = new HashMap<>();
        response.put("url", "http://example.com/thumbnail.jpg");
        when(mediaService.getThumbnail(anyLong())).thenReturn(response);

        mockMvc.perform(get("/api/media/{mediaId}/thumbnail", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void processMedia_ShouldReturnOk() throws Exception {
        Map<String, String> response = new HashMap<>();
        response.put("url", "http://example.com/processed.jpg");

        when(mediaService.processMedia(anyLong(), any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/media/{mediaId}/process", 1L)
                        .param("width", "100")
                        .param("height", "100")
                        .param("quality", "80")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Media processed"))
                .andExpect(jsonPath("$.data").exists());
    }
}
