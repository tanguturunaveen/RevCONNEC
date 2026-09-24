package org.revature.revconnect.service;

import org.revature.revconnect.dto.request.CommentRequest;
import org.revature.revconnect.dto.request.ShareRequest;
import org.revature.revconnect.dto.response.CommentResponse;
import org.revature.revconnect.dto.response.LikeResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.ShareResponse;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.mapper.CommentMapper;
import org.revature.revconnect.mapper.LikeMapper;
import org.revature.revconnect.mapper.ShareMapper;
import org.revature.revconnect.exception.UnauthorizedException;
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
import org.revature.revconnect.model.CommentLike;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.enums.PostType;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InteractionService {

    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final ShareRepository shareRepository;
    private final PostRepository postRepository;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final LikeMapper likeMapper;
    private final CommentMapper commentMapper;
    private final ShareMapper shareMapper;
    private final CommentLikeRepository commentLikeRepository;

    @Transactional
    public void likePost(long postId) {
        User currentUser = authService.getCurrentUser();
        log.info("User {} attempting to like post {}", currentUser.getUsername(), postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (likeRepository.existsByUserIdAndPostId(currentUser.getId(), postId)) {
            log.warn("User {} already liked post {}", currentUser.getUsername(), postId);
            throw new BadRequestException("You have already liked this post");
        }

        Like like = Like.builder()
                .user(currentUser)
                .post(post)
                .build();

        likeRepository.save(like);
        post.setLikeCount(post.getLikeCount() + 1);
        postRepository.save(post);

        // Send notification to post owner
        notificationService.notifyLike(post.getUser(), currentUser, postId);

        log.info("User {} successfully liked post {}", currentUser.getUsername(), postId);
    }

    @Transactional
    public void unlikePost(Long postId) {
        User currentUser = authService.getCurrentUser();
        log.info("User {} attempting to unlike post {}", currentUser.getUsername(), postId);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        Like like = likeRepository.findByUserIdAndPostId(currentUser.getId(), postId)
                .orElseThrow(() -> new BadRequestException("You have not liked this post"));

        likeRepository.delete(like);
        post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
        postRepository.save(post);

        log.info("User {} successfully unliked post {}", currentUser.getUsername(), postId);
    }

    public PagedResponse<LikeResponse> getPostLikes(Long postId, int page, int size) {
        log.info("Fetching likes for post {}, page: {}, size: {}", postId, page, size);

        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }

        Page<Like> likes = likeRepository.findByPostIdOrderByCreatedAtDesc(postId, PageRequest.of(page, size));
        log.info("Found {} likes for post {}", likes.getTotalElements(), postId);
        return PagedResponse.fromEntityPage(likes, likeMapper::toResponse);
    }

    public boolean hasUserLikedPost(Long postId) {
        User currentUser = authService.getCurrentUser();
        return likeRepository.existsByUserIdAndPostId(currentUser.getId(), postId);
    }

    @Transactional
    public CommentResponse addComment(Long postId, CommentRequest request) {
        try {
            User currentUser = authService.getCurrentUser();
            log.info("[TRACE] User {} adding comment to post {}. Content: {}",
                    currentUser.getUsername(), postId, request.getContent());

            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
            log.info("[TRACE] Found post {}. Current comment count: {}", postId, post.getCommentCount());

            Comment parent = null;
            if (request.getParentId() != null) {
                log.info("[TRACE] Comment is a reply to {}", request.getParentId());
                parent = commentRepository.findById(request.getParentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.getParentId()));
            }

            Comment comment = Comment.builder()
                    .content(request.getContent())
                    .user(currentUser)
                    .post(post)
                    .parent(parent)
                    .likeCount(0)
                    .replyCount(0)
                    .build();

            log.info("[TRACE] Calling commentRepository.saveAndFlush()...");
            Comment savedComment = commentRepository.saveAndFlush(comment);
            log.info("[TRACE] Comment saved with ID: {}. CreatedAt: {}", savedComment.getId(),
                    savedComment.getCreatedAt());

            if (parent != null) {
                int currentPR = parent.getReplyCount() == null ? 0 : parent.getReplyCount();
                parent.setReplyCount(currentPR + 1);
                commentRepository.save(parent);
                log.info("[TRACE] Parent comment {} replyCount updated to {}", parent.getId(), currentPR + 1);
            }

            Integer currentCount = post.getCommentCount();
            int newCount = (currentCount == null ? 0 : currentCount) + 1;
            post.setCommentCount(newCount);
            postRepository.save(post);
            log.info("[TRACE] Post {} comment count updated to {}", postId, newCount);

            try {
                notificationService.notifyComment(post.getUser(), currentUser, postId);
                log.info("[TRACE] Notification sent to recipient {}", post.getUser().getUsername());
            } catch (Exception e) {
                log.error("[TRACE] Notification failed but proceeding: {}", e.getMessage());
            }

            log.info("[TRACE] Enriching response for comment {}", savedComment.getId());
            CommentResponse response = enrichCommentResponse(savedComment, currentUser);
            log.info("[TRACE] addComment FINISHED successfully for comment {}", savedComment.getId());
            return response;
        } catch (Exception e) {
            log.error("[TRACE] FATAL ERROR in addComment for post {}: {}", postId, e.getMessage(), e);
            throw e;
        }
    }

    public PagedResponse<CommentResponse> getPostComments(Long postId, int page, int size) {
        log.info("Fetching top-level comments for post {}, page: {}, size: {}", postId, page, size);
        User currentUser = authService.getCurrentUser();

        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }

        Page<Comment> comments = commentRepository.findByPostIdAndParentIsNullOrderByCreatedAtDesc(postId,
                PageRequest.of(page, size));
        log.info("Found {} top-level comments for post {}", comments.getTotalElements(), postId);

        return PagedResponse.fromEntityPage(comments, comment -> enrichCommentResponse(comment, currentUser));
    }

    private CommentResponse enrichCommentResponse(Comment comment, User currentUser) {
        CommentResponse resp = commentMapper.toResponse(comment);
        resp.setIsLikedByCurrentUser(
                commentLikeRepository.existsByUserIdAndCommentId(currentUser.getId(), comment.getId()));
        return resp;
    }

    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = authService.getCurrentUser();
        log.info("User {} attempting to delete comment {}", currentUser.getUsername(), commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        Post post = comment.getPost();

        if (!comment.getUser().getId().equals(currentUser.getId()) &&
                !post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only delete your own comments or comments on your own post");
        }

        List<Comment> replies = commentRepository.findByParentId(commentId);
        for (Comment reply : replies) {
            deleteComment(reply.getId());
        }

        commentLikeRepository.deleteByCommentId(commentId);

        commentRepository.delete(comment);

        Integer currentCount = post.getCommentCount();
        post.setCommentCount(currentCount == null || currentCount <= 0 ? 0 : currentCount - 1);
        postRepository.save(post);

        if (comment.getParent() != null) {
            Comment parent = comment.getParent();
            int pr = parent.getReplyCount() == null ? 0 : parent.getReplyCount();
            parent.setReplyCount(Math.max(0, pr - 1));
            commentRepository.save(parent);
        }

        log.info("Comment {} and its replies deleted successfully", commentId);
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, CommentRequest request) {
        User currentUser = authService.getCurrentUser();
        log.info("User {} attempting to update comment {}", currentUser.getUsername(), commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only edit your own comments");
        }

        comment.setContent(request.getContent());
        Comment saved = commentRepository.save(comment);

        return enrichCommentResponse(saved, currentUser);
    }

    @Transactional
    public void likeComment(Long commentId) {
        User currentUser = authService.getCurrentUser();
        log.info("User {} attempting to like comment {}", currentUser.getUsername(), commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        if (commentLikeRepository.existsByUserIdAndCommentId(currentUser.getId(), commentId)) {
            throw new BadRequestException("You have already liked this comment");
        }

        CommentLike like = CommentLike.builder()
                .user(currentUser)
                .comment(comment)
                .build();

        commentLikeRepository.save(like);

        Integer currentCount = comment.getLikeCount();
        comment.setLikeCount(currentCount == null ? 1 : currentCount + 1);
        commentRepository.save(comment);

        log.info("User {} successfully liked comment {}", currentUser.getUsername(), commentId);
    }

    @Transactional
    public void unlikeComment(Long commentId) {
        User currentUser = authService.getCurrentUser();
        log.info("User {} attempting to unlike comment {}", currentUser.getUsername(), commentId);

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        CommentLike like = commentLikeRepository.findByUserAndComment(currentUser, comment)
                .orElseThrow(() -> new BadRequestException("You have not liked this comment"));

        commentLikeRepository.delete(like);

        Integer currentCount = comment.getLikeCount();
        comment.setLikeCount(currentCount == null || currentCount <= 0 ? 0 : currentCount - 1);
        commentRepository.save(comment);

        log.info("User {} successfully unliked comment {}", currentUser.getUsername(), commentId);
    }

    public PagedResponse<CommentResponse> getCommentReplies(Long commentId, int page, int size) {
        log.info("Fetching replies for comment {}, page: {}, size: {}", commentId, page, size);
        User currentUser = authService.getCurrentUser();

        if (!commentRepository.existsById(commentId)) {
            throw new ResourceNotFoundException("Comment", "id", commentId);
        }

        Page<Comment> replies = commentRepository.findByParentIdOrderByCreatedAtAsc(commentId,
                PageRequest.of(page, size));

        return PagedResponse.fromEntityPage(replies, reply -> enrichCommentResponse(reply, currentUser));
    }

    @Transactional
    public CommentResponse replyToComment(Long postId, Long commentId, String message) {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getUserType() == UserType.PERSONAL) {
            throw new UnauthorizedException("Only creator/business accounts can use this reply endpoint");
        }
        Comment parent = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        if (!parent.getPost().getId().equals(post.getId())) {
            throw new BadRequestException("Comment does not belong to this post");
        }
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only reply to comments on your own posts");
        }

        String prefixed = "Reply to #" + parent.getId() + ": " + message;
        Comment reply = Comment.builder()
                .content(prefixed)
                .user(currentUser)
                .post(post)
                .build();

        Comment saved = commentRepository.save(reply);
        Integer count = post.getCommentCount();
        post.setCommentCount(count == null ? 1 : count + 1);
        postRepository.save(post);
        return commentMapper.toResponse(saved);
    }

    @Transactional
    public ShareResponse sharePost(Long postId, ShareRequest request) {
        User currentUser = authService.getCurrentUser();
        log.info("User {} attempting to share post {}", currentUser.getUsername(), postId);

        Post originalPost = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (shareRepository.existsByUserIdAndPostId(currentUser.getId(), postId)) {
            throw new BadRequestException("You have already shared this post");
        }

        originalPost.setShareCount(originalPost.getShareCount() + 1);
        postRepository.save(originalPost);

        Post repost = Post.builder()
                .content(request != null && request.getComment() != null ? request.getComment() : "Reposted this post")
                .user(currentUser)
                .postType(PostType.valueOf("REPOST")) // We'll add this enum value next
                .originalPost(originalPost)
                .build();

        Post savedRepost = postRepository.save(repost);

        Share share = Share.builder()
                .user(currentUser)
                .post(originalPost)
                .comment(request != null ? request.getComment() : null)
                .build();
        Share savedShare = shareRepository.save(share);

        notificationService.notifyShare(originalPost.getUser(), currentUser, postId);

        log.info("User {} successfully reposted post {}", currentUser.getUsername(), postId);
        return shareMapper.toResponse(savedShare);
    }

    @Transactional
    public void incrementShareCount(Long postId) {
        Post originalPost = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        // Just increment the counter when shared via Messages/DM
        originalPost.setShareCount((originalPost.getShareCount() == null ? 0 : originalPost.getShareCount()) + 1);
        postRepository.save(originalPost);
        log.info("Post {} share count incremented", postId);
    }
}
