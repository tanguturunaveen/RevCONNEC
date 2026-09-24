package org.revature.revconnect.service;

import org.revature.revconnect.dto.request.PostRequest;
import org.revature.revconnect.dto.request.SchedulePostRequest;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.enums.PostType;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.mapper.PostMapper;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.ConnectionRepository;
import org.revature.revconnect.repository.PostRepository;
import org.revature.revconnect.repository.CommentRepository;
import org.revature.revconnect.repository.LikeRepository;
import org.revature.revconnect.repository.BookmarkRepository;
import org.revature.revconnect.repository.PostAnalyticsRepository;
import org.revature.revconnect.repository.CommentLikeRepository;
import org.revature.revconnect.model.Comment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final AuthService authService;
    private final PostMapper postMapper;
    private final HashtagService hashtagService;
    private final ConnectionRepository connectionRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PostAnalyticsRepository postAnalyticsRepository;
    private final CommentLikeRepository commentLikeRepository;
    private static final ScheduledExecutorService POST_SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static final AtomicLong SCHEDULE_ID = new AtomicLong(1);
    private static final Map<Long, Map<String, Object>> SCHEDULED_POSTS = new ConcurrentHashMap<>();

    @Transactional
    public PostResponse createPost(PostRequest request) {
        User currentUser = authService.getCurrentUser();
        log.info("Creating post for user: {}", currentUser.getUsername());

        Post post = Post.builder()
                .content(request.getContent())
                .user(currentUser)
                .postType(request.getPostType() != null ? request.getPostType() : PostType.TEXT)
                .mediaUrls(request.getMediaUrls() != null ? request.getMediaUrls() : new ArrayList<>())
                .build();

        Post savedPost = postRepository.save(post);
        hashtagService.processHashtagsFromContent(savedPost.getContent());
        log.info("Post created with ID: {}", savedPost.getId());
        return toResponseWithFullMetadata(savedPost);
    }

    public PostResponse getPostById(Long postId) {
        log.info("Fetching post with ID: {}", postId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        return toResponseWithFullMetadata(post);
    }

    public PagedResponse<PostResponse> getMyPosts(int page, int size) {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching posts for user: {}", currentUser.getUsername());
        Page<Post> posts = postRepository.findByUserIdWithPinnedFirst(currentUser.getId(), PageRequest.of(page, size));
        return PagedResponse.fromEntityPage(posts, this::toResponseWithFullMetadata);
    }

    public PagedResponse<PostResponse> getUserPosts(Long userId, int page, int size) {
        log.info("Fetching posts for user ID: {}", userId);
        Page<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return PagedResponse.fromEntityPage(posts, this::toResponseWithFullMetadata);
    }

    public PagedResponse<PostResponse> getUserLikedPosts(Long userId, int page, int size) {
        log.info("Fetching liked posts for user ID: {}", userId);
        Page<Post> posts = postRepository.findLikedPostsByUserId(userId, PageRequest.of(page, size));
        return PagedResponse.fromEntityPage(posts, this::toResponseWithFullMetadata);
    }

    public PagedResponse<PostResponse> getUserMediaPosts(Long userId, int page, int size) {
        log.info("Fetching media posts for user ID: {}", userId);
        Page<Post> posts = postRepository.findMediaPostsByUserId(userId, PageRequest.of(page, size));
        return PagedResponse.fromEntityPage(posts, this::toResponseWithFullMetadata);
    }

    public PagedResponse<PostResponse> getPublicFeed(int page, int size) {
        log.info("Fetching public feed, page: {}, size: {}", page, size);
        Page<Post> posts = postRepository.findPublicPosts(PageRequest.of(page, size));
        return PagedResponse.fromEntityPage(posts, this::toResponseWithFullMetadata);
    }

    public PagedResponse<PostResponse> getTrendingPosts(int page, int size) {
        log.info("Fetching trending posts, page: {}, size: {}", page, size);
        Page<Post> posts = postRepository.findTrendingPublicPosts(PageRequest.of(page, size));
        return PagedResponse.fromEntityPage(posts, this::toResponseWithFullMetadata);
    }

    public PagedResponse<PostResponse> getPersonalizedFeed(int page, int size, PostType postType, UserType userType) {
        User currentUser = authService.getCurrentUser();
        List<Long> userIds = new ArrayList<>(connectionRepository.findFollowingUserIds(currentUser.getId()));
        if (!userIds.contains(currentUser.getId())) {
            userIds.add(currentUser.getId());
        }
        Page<Post> posts = postRepository.findPersonalizedFeed(
                userIds, postType, userType, PageRequest.of(page, size));
        return PagedResponse.fromEntityPage(posts, this::toResponseWithFullMetadata);
    }

    @Transactional
    public PostResponse updatePost(Long postId, PostRequest request) {
        User currentUser = authService.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            log.warn("User {} attempted to edit post {} owned by user {}",
                    currentUser.getId(), postId, post.getUser().getId());
            throw new UnauthorizedException("You can only edit your own posts");
        }

        log.info("Updating post ID: {}", postId);
        post.setContent(request.getContent());
        if (request.getPostType() != null) {
            post.setPostType(request.getPostType());
        }
        if (request.getMediaUrls() != null) {
            post.setMediaUrls(request.getMediaUrls());
        }

        Post updatedPost = postRepository.save(post);
        hashtagService.processHashtagsFromContent(updatedPost.getContent());
        log.info("Post updated successfully: {}", postId);
        return toResponseWithFullMetadata(updatedPost);
    }

    @Transactional
    public void deletePost(Long postId) {
        User currentUser = authService.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            log.warn("User {} attempted to delete post {} owned by user {}",
                    currentUser.getId(), postId, post.getUser().getId());
            throw new UnauthorizedException("You can only delete your own posts");
        }

        log.info("Deleting post ID: {}", postId);

        // 1. Clean up Comment Likes
        commentRepository.findByPostId(postId).forEach(comment -> {
            commentLikeRepository.deleteByCommentId(comment.getId());
        });

        // 2. Clean up Comments
        commentRepository.deleteByPostId(postId);

        // 3. Clean up Likes
        likeRepository.deleteByPostId(postId);

        // 4. Clean up Bookmarks
        bookmarkRepository.deleteByPostId(postId);

        // 5. Clean up Analytics
        postAnalyticsRepository.deleteByPostId(postId);

        // 6. Finally delete the post
        postRepository.delete(post);
        log.info("Post deleted successfully: {}", postId);
    }

    @Transactional
    public PostResponse togglePinPost(Long postId) {
        User currentUser = authService.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only pin your own posts");
        }

        post.setPinned(!Boolean.TRUE.equals(post.getPinned()));
        Post updatedPost = postRepository.save(post);
        log.info("Post {} pinned status changed to: {}", postId, updatedPost.getPinned());
        return toResponseWithFullMetadata(updatedPost);
    }

    @Transactional
    public Map<String, Object> setPostCta(Long postId, String label, String url) {
        User currentUser = authService.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only edit your own posts");
        }
        PostMetadata metadata = parseMetadata(post.getContent());
        String updated = buildContent(metadata.baseContent(), label, url, metadata.tags(), metadata.isPromotional(),
                metadata.partnerName());
        post.setContent(updated);
        postRepository.save(post);

        Map<String, Object> response = new HashMap<>();
        response.put("postId", postId);
        response.put("ctaLabel", label);
        response.put("ctaUrl", url);
        return response;
    }

    @Transactional
    public Map<String, Object> clearPostCta(Long postId) {
        User currentUser = authService.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only edit your own posts");
        }
        PostMetadata metadata = parseMetadata(post.getContent());
        post.setContent(buildContent(metadata.baseContent(), null, null, metadata.tags(), metadata.isPromotional(),
                metadata.partnerName()));
        postRepository.save(post);
        return Map.of("postId", postId, "ctaCleared", true);
    }

    @Transactional
    public Map<String, Object> setProductTags(Long postId, List<String> tags) {
        User currentUser = authService.getCurrentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        if (!post.getUser().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only edit your own posts");
        }
        List<String> sanitized = tags == null ? List.of()
                : tags.stream()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
        PostMetadata metadata = parseMetadata(post.getContent());
        post.setContent(buildContent(metadata.baseContent(), metadata.ctaLabel(), metadata.ctaUrl(), sanitized,
                metadata.isPromotional(), metadata.partnerName()));
        postRepository.save(post);
        return Map.of("postId", postId, "tags", sanitized);
    }

    public Map<String, Object> getPostMetadata(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        PostMetadata metadata = parseMetadata(post.getContent());
        Map<String, Object> map = new HashMap<>();
        map.put("postId", postId);
        map.put("ctaLabel", metadata.ctaLabel());
        map.put("ctaUrl", metadata.ctaUrl());
        map.put("tags", metadata.tags());
        map.put("isPromotional", metadata.isPromotional());
        map.put("partnerName", metadata.partnerName());
        return map;
    }

    @Transactional
    public Map<String, Object> schedulePost(SchedulePostRequest request) {
        User currentUser = authService.getCurrentUser();
        long scheduleId = SCHEDULE_ID.getAndIncrement();
        long delayMs = Math.max(0,
                java.time.Duration.between(java.time.LocalDateTime.now(), request.getPublishAt()).toMillis());

        Map<String, Object> info = new HashMap<>();
        info.put("scheduleId", scheduleId);
        info.put("status", "SCHEDULED");
        info.put("publishAt", request.getPublishAt());
        info.put("userId", currentUser.getId());
        SCHEDULED_POSTS.put(scheduleId, info);

        POST_SCHEDULER.schedule(() -> {
            try {
                Post post = Post.builder()
                        .content(request.getContent())
                        .user(currentUser)
                        .postType(request.getPostType() != null ? request.getPostType() : PostType.TEXT)
                        .mediaUrls(request.getMediaUrls() != null ? request.getMediaUrls() : new ArrayList<>())
                        .build();
                Post savedPost = postRepository.save(post);
                hashtagService.processHashtagsFromContent(savedPost.getContent());
                info.put("status", "PUBLISHED");
                info.put("postId", savedPost.getId());
            } catch (Exception ex) {
                info.put("status", "FAILED");
                info.put("error", ex.getMessage());
            }
        }, delayMs, TimeUnit.MILLISECONDS);

        return info;
    }

    public List<Map<String, Object>> getMyScheduledPosts() {
        User currentUser = authService.getCurrentUser();
        return SCHEDULED_POSTS.values().stream()
                .filter(item -> currentUser.getId().equals(item.get("userId")))
                .toList();
    }

    private PostMetadata parseMetadata(String content) {
        if (content == null) {
            return new PostMetadata("", null, null, List.of(), false, null);
        }
        String base = content;
        String ctaLabel = null;
        String ctaUrl = null;
        List<String> tags = new ArrayList<>();
        boolean isPromotional = false;
        String partnerName = null;

        // Extract CTA
        int ctaStart = base.indexOf("\n[[CTA|");
        if (ctaStart >= 0) {
            int ctaEnd = base.indexOf("]]", ctaStart);
            if (ctaEnd > ctaStart) {
                String token = base.substring(ctaStart + 7, ctaEnd);
                String[] parts = token.split("\\|", 2);
                if (parts.length == 2) {
                    ctaLabel = parts[0].trim();
                    ctaUrl = parts[1].trim();
                }
                base = base.substring(0, ctaStart) + base.substring(ctaEnd + 2);
            }
        }

        // Extract Promo
        int promoStart = base.indexOf("\n[[PROMO|");
        if (promoStart >= 0) {
            int promoEnd = base.indexOf("]]", promoStart);
            if (promoEnd > promoStart) {
                isPromotional = true;
                partnerName = base.substring(promoStart + 9, promoEnd).trim();
                base = base.substring(0, promoStart) + base.substring(promoEnd + 2);
            }
        }

        // Extract Tags
        int tagsStart = base.indexOf("\n[[TAGS|");
        if (tagsStart >= 0) {
            int tagsEnd = base.indexOf("]]", tagsStart);
            if (tagsEnd > tagsStart) {
                String token = base.substring(tagsStart + 8, tagsEnd);
                tags = Arrays.stream(token.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .distinct()
                        .collect(Collectors.toList());
                base = base.substring(0, tagsStart) + base.substring(tagsEnd + 2);
            }
        }

        return new PostMetadata(base.trim(), ctaLabel, ctaUrl, tags, isPromotional, partnerName);
    }

    private String buildContent(String baseContent, String ctaLabel, String ctaUrl, List<String> tags,
                                boolean isPromotional, String partnerName) {
        StringBuilder sb = new StringBuilder(baseContent == null ? "" : baseContent.trim());
        if (ctaLabel != null && !ctaLabel.isBlank() && ctaUrl != null && !ctaUrl.isBlank()) {
            sb.append("\n[[CTA|").append(ctaLabel.trim()).append("|").append(ctaUrl.trim()).append("]]");
        }
        if (isPromotional && partnerName != null && !partnerName.isBlank()) {
            sb.append("\n[[PROMO|").append(partnerName.trim()).append("]]");
        }
        List<String> safeTags = tags == null ? Collections.emptyList() : tags;
        if (!safeTags.isEmpty()) {
            sb.append("\n[[TAGS|").append(String.join(",", safeTags)).append("]]");
        }
        return sb.toString().trim();
    }

    public PostResponse toResponseWithFullMetadata(Post post) {
        User currentUser = authService.getCurrentUser();
        PostMetadata meta = parseMetadata(post.getContent());
        PostResponse resp = postMapper.toResponseWithMetadata(post, meta.baseContent(), meta.ctaLabel(), meta.ctaUrl(),
                meta.tags(),
                meta.isPromotional(), meta.partnerName());

        if (currentUser != null) {
            resp.setIsLikedByCurrentUser(likeRepository.existsByUserIdAndPostId(currentUser.getId(), post.getId()));
        }

        return resp;
    }

    private record PostMetadata(String baseContent, String ctaLabel, String ctaUrl, List<String> tags,
                                boolean isPromotional, String partnerName) {
    }
}
