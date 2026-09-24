package org.revature.revconnect.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.model.Bookmark;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.BookmarkRepository;
import org.revature.revconnect.repository.PostRepository;
import org.revature.revconnect.repository.UserRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PostService postService;

    @InjectMocks
    private BookmarkService bookmarkService;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bookmarkPost_success() {
        User user = user(1L, "alice");
        Post post = post(10L, user);
        setAuth("alice");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(bookmarkRepository.existsByUserAndPost(user, post)).thenReturn(false);

        bookmarkService.bookmarkPost(10L);

        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    void bookmarkPost_duplicate_throws() {
        User user = user(1L, "alice");
        Post post = post(10L, user);
        setAuth("alice");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(bookmarkRepository.existsByUserAndPost(user, post)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> bookmarkService.bookmarkPost(10L));
    }

    @Test
    void removeBookmark_success() {
        User user = user(1L, "alice");
        Post post = post(11L, user);
        setAuth("alice");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(postRepository.findById(11L)).thenReturn(Optional.of(post));

        bookmarkService.removeBookmark(11L);

        verify(bookmarkRepository).deleteByUserAndPost(user, post);
    }

    @Test
    void removeBookmark_postMissing_throws() {
        User user = user(1L, "alice");
        setAuth("alice");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(postRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> bookmarkService.removeBookmark(404L));
    }

    @Test
    void getBookmarks_returnsPagedResult() {
        User user = user(1L, "alice");
        Post post = post(21L, user);
        Bookmark bookmark = Bookmark.builder().id(1L).user(user).post(post).build();
        bookmark.setCreatedAt(LocalDateTime.now());
        setAuth("alice");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(bookmarkRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(bookmark), PageRequest.of(0, 10), 1));
        when(postService.toResponseWithFullMetadata(post)).thenReturn(new PostResponse());

        var page = bookmarkService.getBookmarks(0, 10);
        assertEquals(1, page.getContent().size());
        assertEquals(1L, page.getTotalElements());
    }

    @Test
    void isBookmarked_true() {
        User user = user(1L, "alice");
        Post post = post(9L, user);
        setAuth("alice");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(postRepository.findById(9L)).thenReturn(Optional.of(post));
        when(bookmarkRepository.existsByUserAndPost(user, post)).thenReturn(true);

        assertTrue(bookmarkService.isBookmarked(9L));
    }

    @Test
    void isBookmarked_false() {
        User user = user(1L, "alice");
        Post post = post(12L, user);
        setAuth("alice");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(postRepository.findById(12L)).thenReturn(Optional.of(post));
        when(bookmarkRepository.existsByUserAndPost(user, post)).thenReturn(false);

        assertFalse(bookmarkService.isBookmarked(12L));
    }

    private void setAuth(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(username, "pass"));
    }

    private User user(Long id, String username) {
        return User.builder().id(id).username(username).name(username)
                .email(username + "@test.com").password("x").build();
    }

    private Post post(Long id, User user) {
        return Post.builder().id(id).user(user).content("post")
                .likeCount(0).commentCount(0).shareCount(0).build();
    }
}
