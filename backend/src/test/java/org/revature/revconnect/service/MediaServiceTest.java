package org.revature.revconnect.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.UserRepository;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

    @Mock private AuthService authService;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private MediaService mediaService;

    @BeforeEach
    void configureUploadRoot() {
        String root = Path.of("target", "test-uploads", String.valueOf(System.nanoTime())).toString();
        ReflectionTestUtils.setField(mediaService, "uploadRoot", root);
    }

    @Test
    void uploadFile_empty_throwsBadRequest() {
        User me = user(2001L, "me2001");
        when(authService.getCurrentUser()).thenReturn(me);
        MultipartFile empty = new MockMultipartFile("file", new byte[0]);

        assertThrows(BadRequestException.class, () -> mediaService.uploadFile(empty));
    }

    @Test
    void uploadFile_success_returnsMetadataAndStoresFile() {
        User me = user(2002L, "me2002");
        when(authService.getCurrentUser()).thenReturn(me);
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", "img".getBytes());

        Map<String, String> result = mediaService.uploadFile(file);

        assertNotNull(result.get("mediaId"));
        assertTrue(result.get("url").contains("/media/2002/generic/"));
        assertEquals("a.jpg", result.get("fileName"));
    }

    @Test
    void uploadMultipleFiles_returnsEachUploadResult() {
        User me = user(2003L, "me2003");
        when(authService.getCurrentUser()).thenReturn(me);
        MockMultipartFile f1 = new MockMultipartFile("files", "a.jpg", "image/jpeg", "a".getBytes());
        MockMultipartFile f2 = new MockMultipartFile("files", "b.jpg", "image/jpeg", "b".getBytes());

        List<Map<String, String>> result = mediaService.uploadMultipleFiles(List.of(f1, f2));

        assertEquals(2, result.size());
    }

    @Test
    void uploadProfilePicture_updatesCurrentUserProfileAndSaves() {
        User me = user(2004L, "me2004");
        when(authService.getCurrentUser()).thenReturn(me);
        MockMultipartFile file = new MockMultipartFile("file", "p.jpg", "image/jpeg", "img".getBytes());
        when(userRepository.save(me)).thenReturn(me);

        Map<String, String> result = mediaService.uploadProfilePicture(file);

        assertEquals(result.get("url"), me.getProfilePicture());
        verify(userRepository).save(me);
    }

    @Test
    void uploadCoverPhoto_and_uploadVideo_returnUrls() {
        User me = user(2005L, "me2005");
        when(authService.getCurrentUser()).thenReturn(me);
        MockMultipartFile cover = new MockMultipartFile("file", "c.jpg", "image/jpeg", "c".getBytes());
        MockMultipartFile video = new MockMultipartFile("file", "v.mp4", "video/mp4", "v".getBytes());

        Map<String, String> coverResult = mediaService.uploadCoverPhoto(cover);
        Map<String, String> videoResult = mediaService.uploadVideo(video);

        assertTrue(coverResult.get("url").contains("/cover/"));
        assertTrue(videoResult.get("url").contains("/video/"));
    }

    @Test
    void deleteMedia_notFound_throwsNotFound() {
        User me = user(2006L, "me2006");
        when(authService.getCurrentUser()).thenReturn(me);

        assertThrows(ResourceNotFoundException.class, () -> mediaService.deleteMedia(99999L));
    }

    @Test
    void deleteMedia_foreignOwner_throwsUnauthorized() {
        User owner = user(2007L, "owner2007");
        User other = user(3007L, "other3007");
        when(authService.getCurrentUser()).thenReturn(owner);
        Map<String, String> uploaded = mediaService.uploadFile(new MockMultipartFile("file", "x.jpg", "image/jpeg", "x".getBytes()));
        Long mediaId = Long.valueOf(uploaded.get("mediaId"));

        when(authService.getCurrentUser()).thenReturn(other);
        assertThrows(UnauthorizedException.class, () -> mediaService.deleteMedia(mediaId));
    }

    @Test
    void deleteMedia_owner_deletesRecordAndPhysicalFile() throws Exception {
        User me = user(2008L, "me2008");
        when(authService.getCurrentUser()).thenReturn(me);
        Map<String, String> uploaded = mediaService.uploadFile(new MockMultipartFile("file", "x.jpg", "image/jpeg", "x".getBytes()));
        Long mediaId = Long.valueOf(uploaded.get("mediaId"));

        Map<String, Object> media = mediaService.getMedia(mediaId);
        Path filePath = resolveStoredPath(media.get("url").toString());
        assertTrue(Files.exists(filePath));

        mediaService.deleteMedia(mediaId);

        assertFalse(Files.exists(filePath));
        assertThrows(ResourceNotFoundException.class, () -> mediaService.getMedia(mediaId));
    }

    @Test
    void getMedia_foreignOwner_throwsUnauthorized() {
        User owner = user(2009L, "owner2009");
        User other = user(3009L, "other3009");
        when(authService.getCurrentUser()).thenReturn(owner);
        Map<String, String> uploaded = mediaService.uploadFile(new MockMultipartFile("file", "x.jpg", "image/jpeg", "x".getBytes()));
        Long mediaId = Long.valueOf(uploaded.get("mediaId"));

        when(authService.getCurrentUser()).thenReturn(other);
        assertThrows(UnauthorizedException.class, () -> mediaService.getMedia(mediaId));
    }

    @Test
    void getMyMedia_returnsOnlyCurrentUserMediaWithPaging() {
        User u1 = user(2010L, "u1");
        User u2 = user(3010L, "u2");

        when(authService.getCurrentUser()).thenReturn(u1);
        mediaService.uploadFile(new MockMultipartFile("file", "a.jpg", "image/jpeg", "a".getBytes()));
        mediaService.uploadFile(new MockMultipartFile("file", "b.jpg", "image/jpeg", "b".getBytes()));

        when(authService.getCurrentUser()).thenReturn(u2);
        mediaService.uploadFile(new MockMultipartFile("file", "c.jpg", "image/jpeg", "c".getBytes()));

        when(authService.getCurrentUser()).thenReturn(u1);
        List<Map<String, Object>> page0 = mediaService.getMyMedia(0, 1);
        List<Map<String, Object>> page1 = mediaService.getMyMedia(1, 1);

        assertEquals(1, page0.size());
        assertEquals(1, page1.size());
        assertEquals(2010L, page0.get(0).get("ownerId"));
    }

    @Test
    void getThumbnail_notFound_throwsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> mediaService.getThumbnail(77777L));
    }

    @Test
    void getThumbnail_success_returnsUrl() {
        User me = user(2011L, "me2011");
        when(authService.getCurrentUser()).thenReturn(me);
        Map<String, String> uploaded = mediaService.uploadFile(new MockMultipartFile("file", "t.jpg", "image/jpeg", "t".getBytes()));
        Long mediaId = Long.valueOf(uploaded.get("mediaId"));

        Map<String, String> thumb = mediaService.getThumbnail(mediaId);
        assertEquals(uploaded.get("url"), thumb.get("url"));
    }

    @Test
    void processMedia_notFound_throwsNotFound() {
        assertThrows(ResourceNotFoundException.class, () -> mediaService.processMedia(88888L, 100, 100, 80));
    }

    @Test
    void processMedia_success_returnsProcessedStatus() {
        User me = user(2012L, "me2012");
        when(authService.getCurrentUser()).thenReturn(me);
        Map<String, String> uploaded = mediaService.uploadFile(new MockMultipartFile("file", "p.jpg", "image/jpeg", "p".getBytes()));
        Long mediaId = Long.valueOf(uploaded.get("mediaId"));

        Map<String, String> result = mediaService.processMedia(mediaId, 100, 100, 90);

        assertEquals(String.valueOf(mediaId), result.get("mediaId"));
        assertEquals("processed", result.get("status"));
    }

    @Test
    void getMedia_notFound_throwsNotFound() {
        User me = user(2013L, "me2013");
        when(authService.getCurrentUser()).thenReturn(me);
        assertThrows(ResourceNotFoundException.class, () -> mediaService.getMedia(123456L));
    }

    private User user(Long id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .name(username)
                .email(username + "@test.com")
                .password("pwd")
                .privacy(Privacy.PUBLIC)
                .userType(UserType.PERSONAL)
                .build();
    }

    private Path resolveStoredPath(String url) {
        String normalized = url.startsWith("/") ? url.substring(1) : url;
        return Path.of(normalized);
    }
}
