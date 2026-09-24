package org.revature.revconnect.service;

import org.revature.revconnect.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.dto.request.PostRequest;
import org.revature.revconnect.dto.request.SchedulePostRequest;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.enums.PostType;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.mapper.PostMapper;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.ConnectionRepository;
import org.revature.revconnect.repository.PostRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private AuthService authService;
    @Mock
    private PostMapper postMapper;
    @Mock
    private HashtagService hashtagService;
    @Mock
    private ConnectionRepository connectionRepository;
    @Mock
    private LikeRepository likeRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private BookmarkRepository bookmarkRepository;
    @Mock
    private PostAnalyticsRepository postAnalyticsRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;

    @InjectMocks
    private PostService postService;

    @Test
    void createPost_defaultsToTextType() {
        User me = user(1L, "u1");
        PostRequest req = PostRequest.builder().content("Hello #tag").build();
        Post saved = Post.builder().id(1L).user(me).content("Hello #tag").postType(PostType.TEXT)
                .mediaUrls(List.of()).likeCount(0).commentCount(0).shareCount(0).build();
        PostResponse response = PostResponse.builder().id(1L).postType(PostType.TEXT).content("Hello #tag").build();

        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.save(any(Post.class))).thenReturn(saved);
        when(postMapper.toResponseWithMetadata(any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(response);

        PostResponse out = postService.createPost(req);

        assertEquals(PostType.TEXT, out.getPostType());
        verify(hashtagService).processHashtagsFromContent("Hello #tag");
    }

    @Test
    void createPost_withExplicitTypeAndMedia() {
        User me = user(1L, "u1");
        PostRequest req = PostRequest.builder().content("pic").postType(PostType.IMAGE)
                .mediaUrls(List.of("/a.jpg")).build();
        Post saved = Post.builder().id(2L).user(me).content("pic").postType(PostType.IMAGE)
                .mediaUrls(List.of("/a.jpg")).likeCount(0).commentCount(0).shareCount(0).build();
        PostResponse response = PostResponse.builder().id(2L).postType(PostType.IMAGE).build();

        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.save(any(Post.class))).thenReturn(saved);
        when(postMapper.toResponseWithMetadata(any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(response);

        PostResponse out = postService.createPost(req);
        assertEquals(PostType.IMAGE, out.getPostType());
    }

    @Test
    void updatePost_foreignPost_throwsUnauthorized() {
        User me = user(1L, "u1");
        User other = user(2L, "u2");
        Post post = Post.builder().id(5L).user(other).content("x").build();

        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findById(5L)).thenReturn(Optional.of(post));

        assertThrows(UnauthorizedException.class,
                () -> postService.updatePost(5L, PostRequest.builder().content("new").build()));
    }

    @Test
    void updatePost_ownPost_updatesAndProcessesHashtag() {
        User me = user(1L, "u1");
        Post post = Post.builder().id(6L).user(me).content("old").postType(PostType.TEXT).mediaUrls(List.of()).build();
        PostResponse response = PostResponse.builder().id(6L).content("new #x").build();

        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findById(6L)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);
        when(postMapper.toResponseWithMetadata(any(), any(), any(), any(), any(), anyBoolean(), any()))
                .thenReturn(response);

        PostResponse out = postService.updatePost(6L,
                PostRequest.builder().content("new #x").postType(PostType.TEXT).mediaUrls(List.of("/m.jpg")).build());

        assertEquals("new #x", post.getContent());
        assertEquals(List.of("/m.jpg"), post.getMediaUrls());
        verify(hashtagService).processHashtagsFromContent("new #x");
        assertEquals("new #x", out.getContent());
    }

    @Test
    void deletePost_foreignPost_throwsUnauthorized() {
        User me = user(1L, "u1");
        User other = user(2L, "u2");
        Post post = Post.builder().id(7L).user(other).build();

        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findById(7L)).thenReturn(Optional.of(post));

        assertThrows(UnauthorizedException.class, () -> postService.deletePost(7L));
        verify(postRepository, never()).delete(any(Post.class));
    }

    @Test
    void deletePost_ownPost_deletes() {
        User me = user(1L, "u1");
        Post post = Post.builder().id(8L).user(me).build();

        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findById(8L)).thenReturn(Optional.of(post));

        postService.deletePost(8L);
        verify(postRepository).delete(post);
    }

    @Test
    void getPostMetadata_parsesCtaAndTags() {
        Post post = Post.builder().id(9L)
                .content("Hello\n[[CTA|Shop Now|https://shop.com]]\n[[TAGS|a,b]]")
                .build();
        when(postRepository.findById(9L)).thenReturn(Optional.of(post));

        Map<String, Object> meta = postService.getPostMetadata(9L);
        assertEquals("Shop Now", meta.get("ctaLabel"));
        assertEquals("https://shop.com", meta.get("ctaUrl"));
        assertEquals(List.of("a", "b"), meta.get("tags"));
    }

    @Test
    void getPostMetadata_withoutMarkers_returnsNullCtaAndEmptyTags() {
        Post post = Post.builder().id(10L).content("Plain content").build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));

        Map<String, Object> meta = postService.getPostMetadata(10L);
        assertNull(meta.get("ctaLabel"));
        assertNull(meta.get("ctaUrl"));
        assertEquals(List.of(), meta.get("tags"));
    }

    @Test
    void clearPostCta_removesCtaToken() {
        User me = user(1L, "u1");
        Post post = Post.builder().id(11L).user(me).content("Hi\n[[CTA|Learn|https://x.com]]").build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findById(11L)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);

        Map<String, Object> res = postService.clearPostCta(11L);
        assertEquals(true, res.get("ctaCleared"));
        assertTrue(!post.getContent().contains("[[CTA|"));
    }

    @Test
    void setPostCta_foreignPost_throwsUnauthorized() {
        User me = user(1L, "u1");
        User other = user(2L, "u2");
        Post post = Post.builder().id(12L).user(other).content("Hi").build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findById(12L)).thenReturn(Optional.of(post));

        assertThrows(UnauthorizedException.class,
                () -> postService.setPostCta(12L, "Shop", "https://x.com"));
    }

    @Test
    void setProductTags_sanitizesDuplicatesAndBlanks() {
        User me = user(1L, "u1");
        Post post = Post.builder().id(13L).user(me).content("hello").build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findById(13L)).thenReturn(Optional.of(post));
        when(postRepository.save(post)).thenReturn(post);

        Map<String, Object> res = postService.setProductTags(13L, List.of(" a ", "", "a", "b"));
        assertEquals(List.of("a", "b"), res.get("tags"));
    }

    @Test
    void schedulePost_createsScheduledItem() {
        User me = user(1L, "u1");
        when(authService.getCurrentUser()).thenReturn(me);

        SchedulePostRequest request = SchedulePostRequest.builder()
                .content("scheduled post")
                .postType(PostType.TEXT)
                .mediaUrls(List.of())
                .publishAt(LocalDateTime.now().plusMinutes(5))
                .build();

        Map<String, Object> info = postService.schedulePost(request);
        assertEquals("SCHEDULED", info.get("status"));
        assertEquals(1L, info.get("userId"));
    }

    @Test
    void getMyScheduledPosts_filtersByCurrentUser() {
        User me = user(1L, "u1");
        when(authService.getCurrentUser()).thenReturn(me);

        postService.schedulePost(SchedulePostRequest.builder()
                .content("mine")
                .postType(PostType.TEXT)
                .mediaUrls(List.of())
                .publishAt(LocalDateTime.now().plusMinutes(5))
                .build());

        List<Map<String, Object>> list = postService.getMyScheduledPosts();
        assertTrue(list.size() >= 1);
    }

    private User user(Long id, String username) {
        return User.builder().id(id).username(username).name(username)
                .email(username + "@test.com").password("x").build();
    }
}
