package org.revature.revconnect.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.dto.response.UserResponse;
import org.revature.revconnect.enums.PostType;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.mapper.PostMapper;
import org.revature.revconnect.mapper.UserMapper;
import org.revature.revconnect.model.Hashtag;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.PostRepository;
import org.revature.revconnect.repository.UserRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private PostRepository postRepository;
    @Mock private PostMapper postMapper;
    @Mock private HashtagService hashtagService;
    @Mock private AuthService authService;

    @InjectMocks
    private SearchService searchService;

    @Test
    void searchAll_returnsUsersPostsHashtags() {
        User me = user(1L, "me");
        User user = user(2L, "alice");
        Post post = Post.builder().id(1L).content("hello").build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.searchPublicUsers(eq("hello"), eq(PageRequest.of(0, 5))))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 5), 1));
        when(postRepository.searchPosts(eq("hello"), eq(null), eq(null), eq(null), eq(null), eq(null), eq(PageRequest.of(0, 5))))
                .thenReturn(new PageImpl<>(List.of(post), PageRequest.of(0, 5), 1));
        when(userMapper.toPublicResponse(user)).thenReturn(UserResponse.builder().id(2L).username("alice").build());
        when(postMapper.toResponse(post)).thenReturn(PostResponse.builder().id(1L).content("hello").build());
        when(hashtagService.search("hello")).thenReturn(List.of(Hashtag.builder().name("hello").build()));

        Map<String, Object> result = searchService.searchAll("hello", 5);

        assertEquals(1, ((List<?>) result.get("users")).size());
        assertEquals(1, ((List<?>) result.get("posts")).size());
        assertEquals(List.of("hello"), result.get("hashtags"));
    }

    @Test
    void searchUsers_addsToRecentSearches() {
        User me = user(1L, "u1");
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.searchPublicUsers(eq("java"), any())).thenReturn(new PageImpl<>(List.of()));

        searchService.searchUsers("java", 0, 10);
        assertEquals("java", searchService.getRecentSearches().get(0));
    }

    @Test
    void clearRecentSearches_emptiesList() {
        User me = user(2L, "u2");
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.searchPublicUsers(eq("spring"), any())).thenReturn(new PageImpl<>(List.of()));

        searchService.searchUsers("spring", 0, 10);
        searchService.clearRecentSearches();

        assertEquals(0, searchService.getRecentSearches().size());
    }

    @Test
    void removeRecentSearch_removesOneValue() {
        User me = user(3L, "u3");
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.searchPublicUsers(eq("alpha"), any())).thenReturn(new PageImpl<>(List.of()));
        when(userRepository.searchPublicUsers(eq("beta"), any())).thenReturn(new PageImpl<>(List.of()));

        searchService.searchUsers("alpha", 0, 10);
        searchService.searchUsers("beta", 0, 10);
        searchService.removeRecentSearch("alpha");

        assertEquals(List.of("beta"), searchService.getRecentSearches());
    }

    @Test
    void advancedPostSearch_invalidPostType_throws() {
        User me = user(4L, "u4");
        when(authService.getCurrentUser()).thenReturn(me);

        assertThrows(BadRequestException.class,
                () -> searchService.advancedPostSearch("q", null, null, null, "bad", null, 0, 10));
    }

    @Test
    void advancedUserSearch_invalidUserType_throws() {
        User me = user(5L, "u5");
        when(authService.getCurrentUser()).thenReturn(me);

        assertThrows(BadRequestException.class,
                () -> searchService.advancedUserSearch("q", null, "bad", null, 0, 10));
    }

    @Test
    void advancedPostSearch_invalidDateFrom_throws() {
        User me = user(6L, "u6");
        when(authService.getCurrentUser()).thenReturn(me);

        assertThrows(BadRequestException.class,
                () -> searchService.advancedPostSearch("q", null, "2026-13-99", null, "TEXT", null, 0, 10));
    }

    @Test
    void advancedPostSearch_validFilters_callsRepository() {
        User me = user(7L, "u7");
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.searchPosts(any(), any(), eq(PostType.TEXT), eq(5), any(LocalDateTime.class), any(LocalDateTime.class), any()))
                .thenReturn(new PageImpl<>(List.of()));

        searchService.advancedPostSearch("q", "alice", "2026-01-01", "2026-01-02", "TEXT", 5, 0, 10);
    }

    @Test
    void advancedUserSearch_validFilters_callsRepository() {
        User me = user(8L, "u8");
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.advancedSearchPublicUsers(any(), any(), eq(UserType.PERSONAL), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of()));

        searchService.advancedUserSearch("q", "hyd", "PERSONAL", true, 0, 10);
    }

    @Test
    void getSearchSuggestions_andTrendingSearches() {
        User me = user(9L, "u9");
        when(authService.getCurrentUser()).thenReturn(me);
        when(hashtagService.search("ja")).thenReturn(List.of(
                Hashtag.builder().name("java").build(),
                Hashtag.builder().name("jakarta").build()));
        when(hashtagService.getTrending(10)).thenReturn(List.of(
                Hashtag.builder().name("spring").build(),
                Hashtag.builder().name("boot").build()));

        assertEquals(List.of("java", "jakarta"), searchService.getSearchSuggestions("ja"));
        assertEquals(List.of("spring", "boot"), searchService.getTrendingSearches());
    }

    private User user(Long id, String username) {
        return User.builder().id(id).username(username).name(username)
                .email(username + "@test.com").password("x").build();
    }
}
