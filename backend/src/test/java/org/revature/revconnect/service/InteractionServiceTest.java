package org.revature.revconnect.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.dto.request.CommentRequest;
import org.revature.revconnect.dto.request.ShareRequest;
import org.revature.revconnect.dto.response.CommentResponse;
import org.revature.revconnect.dto.response.LikeResponse;
import org.revature.revconnect.dto.response.ShareResponse;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.mapper.CommentMapper;
import org.revature.revconnect.mapper.LikeMapper;
import org.revature.revconnect.mapper.ShareMapper;
import org.revature.revconnect.model.Comment;
import org.revature.revconnect.model.Like;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.Share;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.CommentRepository;
import org.revature.revconnect.repository.LikeRepository;
import org.revature.revconnect.repository.PostRepository;
import org.revature.revconnect.repository.ShareRepository;
import org.revature.revconnect.repository.CommentLikeRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
class InteractionServiceTest {

    @Mock
    private LikeRepository likeRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ShareRepository shareRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private AuthService authService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private LikeMapper likeMapper;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private ShareMapper shareMapper;
    @Mock
    private CommentLikeRepository commentLikeRepository;

    @InjectMocks
    private InteractionService interactionService;

    @Test
    void hasUserLikedPost_true() {
        User me = user(1L, "u1", UserType.PERSONAL);
        when(authService.getCurrentUser()).thenReturn(me);
        when(likeRepository.existsByUserIdAndPostId(1L, 3L)).thenReturn(true);
        assertTrue(interactionService.hasUserLikedPost(3L));
    }

    @Test
    void hasUserLikedPost_false() {
        User me = user(1L, "u1", UserType.PERSONAL);
        when(authService.getCurrentUser()).thenReturn(me);
        when(likeRepository.existsByUserIdAndPostId(1L, 4L)).thenReturn(false);
        assertFalse(interactionService.hasUserLikedPost(4L));
    }

    @Test
    void likePost_success_incrementsAndNotifies() {
        User me = user(1L, "u1", UserType.PERSONAL);
        User owner = user(2L, "u2", UserType.PERSONAL);
        Post post = post(11L, owner);
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findById(11L)).thenReturn(Optional.of(post));
        when(likeRepository.existsByUserIdAndPostId(1L, 11L)).thenReturn(false);

        interactionService.likePost(11L);

        assertEquals(1, post.getLikeCount());
        verify(likeRepository).save(any(Like.class));
        verify(notificationService).notifyLike(owner, me, 11L);
    }

    @Test
    void likePost_duplicate_throws() {
        User me = user(1L, "u1", UserType.PERSONAL);
        Post post = post(12L, me);
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findById(12L)).thenReturn(Optional.of(post));
        when(likeRepository.existsByUserIdAndPostId(1L, 12L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> interactionService.likePost(12L));
    }

    @Test
    void unlikePost_withoutLike_throws() {
        User me = user(1L, "u1", UserType.PERSONAL);
        Post post = post(13L, me);
        post.setLikeCount(1);
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findById(13L)).thenReturn(Optional.of(post));
        when(likeRepository.findByUserIdAndPostId(1L, 13L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> interactionService.unlikePost(13L));
    }

    @Test
    void addComment_success_incrementsAndNotifies() {
        User me = user(1L, "u1", UserType.PERSONAL);
        User owner = user(2L, "u2", UserType.PERSONAL);
        Post post = post(14L, owner);
        Comment saved = Comment.builder().id(1L).user(me).post(post).content("nice").build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findById(14L)).thenReturn(Optional.of(post));
        when(commentRepository.saveAndFlush(any(Comment.class))).thenReturn(saved);
        when(commentMapper.toResponse(saved)).thenReturn(CommentResponse.builder().id(1L).content("nice").build());

        CommentResponse res = interactionService.addComment(14L, CommentRequest.builder().content("nice").build());

        assertEquals(1L, res.getId());
        assertEquals(1, post.getCommentCount());
        verify(notificationService).notifyComment(owner, me, 14L);
    }

    @Test
    void deleteComment_foreignOwner_throws() {
        User me = user(1L, "u1", UserType.PERSONAL);
        User other = user(2L, "u2", UserType.PERSONAL);
        Post post = post(15L, other);
        post.setCommentCount(1);
        Comment comment = Comment.builder().id(1L).user(other).post(post).build();

        when(authService.getCurrentUser()).thenReturn(me);
        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        assertThrows(UnauthorizedException.class, () -> interactionService.deleteComment(1L));
    }

    @Test
    void replyToComment_personalUser_throws() {
        User me = user(1L, "u1", UserType.PERSONAL);
        when(authService.getCurrentUser()).thenReturn(me);

        assertThrows(UnauthorizedException.class,
                () -> interactionService.replyToComment(1L, 1L, "reply"));
    }

    @Test
    void replyToComment_creatorOnOwnPost_success() {
        User creator = user(1L, "creator", UserType.CREATOR);
        Post post = post(20L, creator);
        Comment parent = Comment.builder().id(2L).post(post).user(user(3L, "x", UserType.PERSONAL)).content("q")
                .build();
        Comment saved = Comment.builder().id(3L).post(post).user(creator).content("Reply to #2: thanks").build();

        when(authService.getCurrentUser()).thenReturn(creator);
        when(commentRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(postRepository.findById(20L)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenReturn(saved);
        when(commentMapper.toResponse(saved))
                .thenReturn(CommentResponse.builder().id(3L).content(saved.getContent()).build());

        CommentResponse res = interactionService.replyToComment(20L, 2L, "thanks");
        assertEquals(3L, res.getId());
        assertEquals(1, post.getCommentCount());
    }

    @Test
    void getPostLikes_postMissing_throws() {
        when(postRepository.existsById(99L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> interactionService.getPostLikes(99L, 0, 10));
    }

    @Test
    void getPostLikes_success_returnsPage() {
        Like like = Like.builder().id(1L).build();
        when(postRepository.existsById(30L)).thenReturn(true);
        when(likeRepository.findByPostIdOrderByCreatedAtDesc(30L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(like), PageRequest.of(0, 10), 1));
        when(likeMapper.toResponse(like)).thenReturn(LikeResponse.builder().id(1L).build());

        var page = interactionService.getPostLikes(30L, 0, 10);
        assertEquals(1, page.getContent().size());
    }

    @Test
    void sharePost_success_incrementsAndNotifies() {
        User me = user(1L, "u1", UserType.PERSONAL);
        User owner = user(2L, "u2", UserType.PERSONAL);
        Post post = post(40L, owner);
        Share saved = Share.builder().id(1L).post(post).user(me).comment("great").build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findById(40L)).thenReturn(Optional.of(post));
        when(shareRepository.existsByUserIdAndPostId(1L, 40L)).thenReturn(false);
        when(shareRepository.save(any(Share.class))).thenReturn(saved);
        when(shareMapper.toResponse(saved)).thenReturn(ShareResponse.builder().id(1L).comment("great").build());

        ShareResponse res = interactionService.sharePost(40L, ShareRequest.builder().comment("great").build());

        assertEquals(1L, res.getId());
        assertEquals(1, post.getShareCount());
        verify(notificationService).notifyShare(owner, me, 40L);
    }

    @Test
    void sharePost_duplicate_throws() {
        User me = user(1L, "u1", UserType.PERSONAL);
        Post post = post(41L, me);
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findById(41L)).thenReturn(Optional.of(post));
        when(shareRepository.existsByUserIdAndPostId(1L, 41L)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> interactionService.sharePost(41L, ShareRequest.builder().build()));
    }

    private User user(Long id, String username, UserType type) {
        return User.builder().id(id).username(username).name(username)
                .email(username + "@test.com").password("x").userType(type).build();
    }

    private Post post(Long id, User owner) {
        return Post.builder().id(id).user(owner).content("post")
                .likeCount(0).commentCount(0).shareCount(0).build();
    }
}
