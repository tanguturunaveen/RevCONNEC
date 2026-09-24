package org.revature.revconnect.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.model.Story;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.ConnectionRepository;
import org.revature.revconnect.repository.StoryRepository;
import org.revature.revconnect.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryServiceTest {

    @Mock private StoryRepository storyRepository;
    @Mock private AuthService authService;
    @Mock private ConnectionRepository connectionRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private StoryService storyService;

    @Test
    void createStory_success_savesStory() {
        User me = user(1001L, "me1001");
        Story saved = story(1L, me, false, 0);
        saved.setCaption("hello");
        saved.setMediaUrl("/img.jpg");
        when(authService.getCurrentUser()).thenReturn(me);
        when(storyRepository.save(any(Story.class))).thenReturn(saved);

        Story result = storyService.createStory("/img.jpg", "hello");

        assertEquals(1L, result.getId());
        assertEquals("hello", result.getCaption());
        assertEquals("/img.jpg", result.getMediaUrl());
    }

    @Test
    void getActiveStories_returnsRepositoryResult() {
        User u = user(1002L, "u1002");
        when(storyRepository.findByUserAndExpiresAtAfterOrderByCreatedAtDesc(any(User.class), any(LocalDateTime.class)))
                .thenReturn(List.of(story(2L, u, false, 0)));

        List<Story> result = storyService.getActiveStories(u);
        assertEquals(1, result.size());
    }

    @Test
    void getStoriesFeed_returnsRepositoryResult() {
        User u = user(1003L, "u1003");
        when(storyRepository.findActiveStoriesByUsers(any(List.class), any(LocalDateTime.class)))
                .thenReturn(List.of(story(3L, u, false, 0)));

        List<Story> result = storyService.getStoriesFeed(List.of(u));
        assertEquals(1, result.size());
    }

    @Test
    void getStoriesFeedForCurrentUser_usesFollowingIds() {
        User me = user(1004L, "me1004");
        User followed = user(1005L, "followed1005");
        Story s = story(4L, followed, false, 0);
        when(authService.getCurrentUser()).thenReturn(me);
        when(connectionRepository.findFollowingUserIds(1004L)).thenReturn(List.of(1005L));
        when(userRepository.findAllById(List.of(1005L))).thenReturn(List.of(followed));
        when(storyRepository.findActiveStoriesByUsers(any(List.class), any(LocalDateTime.class))).thenReturn(List.of(s));

        List<Story> result = storyService.getStoriesFeedForCurrentUser();

        assertEquals(1, result.size());
        assertEquals(4L, result.get(0).getId());
    }

    @Test
    void getStory_whenMissing_throwsNotFound() {
        when(storyRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> storyService.getStory(99L));
    }

    @Test
    void markAsHighlight_andUnmarkHighlight_toggleFlag() {
        User me = user(1006L, "u1006");
        Story s = story(6L, me, false, 0);
        when(storyRepository.findById(6L)).thenReturn(Optional.of(s));
        when(storyRepository.save(s)).thenReturn(s);

        Story highlighted = storyService.markAsHighlight(6L);
        assertTrue(highlighted.isHighlight());

        Story unhighlighted = storyService.unmarkHighlight(6L);
        assertFalse(unhighlighted.isHighlight());
    }

    @Test
    void getHighlights_returnsRepositoryResult() {
        User u = user(1007L, "u1007");
        when(storyRepository.findByUserAndIsHighlightTrueOrderByCreatedAtDesc(u))
                .thenReturn(List.of(story(7L, u, true, 1)));

        List<Story> result = storyService.getHighlights(u);
        assertEquals(1, result.size());
    }

    @Test
    void incrementViewCount_firstTimeIncrementsAndSaves() {
        User owner = user(1008L, "owner1008");
        User viewer = user(2008L, "viewer2008");
        Story s = story(8L, owner, false, 0);
        when(storyRepository.findById(8L)).thenReturn(Optional.of(s));
        when(authService.getCurrentUser()).thenReturn(viewer);

        storyService.incrementViewCount(8L);

        assertEquals(1, s.getViewCount());
        verify(storyRepository).save(s);
    }

    @Test
    void incrementViewCount_sameViewerTwice_countsOnce() {
        User owner = user(1009L, "owner1009");
        User viewer = user(2009L, "viewer2009");
        Story s = story(9L, owner, false, 0);
        when(storyRepository.findById(9L)).thenReturn(Optional.of(s));
        when(authService.getCurrentUser()).thenReturn(viewer);

        storyService.incrementViewCount(9L);
        storyService.incrementViewCount(9L);

        assertEquals(1, s.getViewCount());
    }

    @Test
    void getArchivedStories_returnsRepositoryResult() {
        User u = user(1010L, "u1010");
        when(storyRepository.findByUserAndExpiresAtBeforeOrderByCreatedAtDesc(any(User.class), any(LocalDateTime.class)))
                .thenReturn(List.of(story(10L, u, false, 0)));

        List<Story> result = storyService.getArchivedStories(u);
        assertEquals(1, result.size());
    }

    @Test
    void getStoryViewers_whenNoViewers_returnsSummaryShape() {
        User owner = user(1011L, "owner1011");
        Story s = story(11L, owner, false, 3);
        when(storyRepository.findById(11L)).thenReturn(Optional.of(s));

        List<Map<String, Object>> result = storyService.getStoryViewers(11L);

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).get("storyId"));
        assertEquals(1011L, result.get(0).get("ownerId"));
        assertEquals(3, result.get(0).get("viewerCount"));
    }

    @Test
    void reactToStory_andReplyToStory_executeWithoutError() {
        User owner = user(1012L, "owner1012");
        User me = user(2012L, "me2012");
        Story s = story(12L, owner, false, 0);
        when(storyRepository.findById(12L)).thenReturn(Optional.of(s));
        when(authService.getCurrentUser()).thenReturn(me);

        storyService.reactToStory(12L, "LIKE");
        storyService.replyToStory(12L, "Nice story");

        verify(storyRepository, never()).save(any(Story.class));
    }

    @Test
    void deleteStory_notOwner_throwsRuntimeException() {
        User owner = user(1013L, "owner1013");
        User other = user(2013L, "other2013");
        Story s = story(13L, owner, false, 0);
        when(authService.getCurrentUser()).thenReturn(other);
        when(storyRepository.findById(13L)).thenReturn(Optional.of(s));

        assertThrows(RuntimeException.class, () -> storyService.deleteStory(13L));
    }

    @Test
    void deleteStory_owner_deletesStory() {
        User owner = user(1014L, "owner1014");
        Story s = story(14L, owner, false, 0);
        when(authService.getCurrentUser()).thenReturn(owner);
        when(storyRepository.findById(14L)).thenReturn(Optional.of(s));

        storyService.deleteStory(14L);

        verify(storyRepository).delete(s);
    }

    @Test
    void cleanupExpiredStories_deletesAllExpired() {
        User owner = user(1015L, "owner1015");
        List<Story> expired = List.of(story(15L, owner, false, 0), story(16L, owner, false, 0));
        when(storyRepository.findExpiredStories(any(LocalDateTime.class))).thenReturn(expired);

        storyService.cleanupExpiredStories();

        verify(storyRepository).deleteAll(expired);
    }

    private User user(Long id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .email(username + "@test.com")
                .name(username)
                .password("pwd")
                .privacy(Privacy.PUBLIC)
                .userType(UserType.PERSONAL)
                .build();
    }

    private Story story(Long id, User user, boolean highlight, int viewCount) {
        return Story.builder()
                .id(id)
                .user(user)
                .mediaUrl("/m.jpg")
                .caption("caption")
                .createdAt(LocalDateTime.now().minusHours(1))
                .expiresAt(LocalDateTime.now().plusHours(23))
                .isHighlight(highlight)
                .viewCount(viewCount)
                .build();
    }
}
