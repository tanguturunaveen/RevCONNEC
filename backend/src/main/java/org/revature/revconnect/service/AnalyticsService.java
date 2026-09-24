package org.revature.revconnect.service;

import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.mapper.PostMapper;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.PostAnalytics;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.ConnectionRepository;
import org.revature.revconnect.repository.PostAnalyticsRepository;
import org.revature.revconnect.repository.PostRepository;
import org.revature.revconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final AuthService authService;
    private final PostRepository postRepository;
    private final PostAnalyticsRepository postAnalyticsRepository;
    private final ConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;

    @Transactional(readOnly = true)
    public Map<String, Object> getOverview() {
        User currentUser = authService.getCurrentUser();
        Long userId = currentUser.getId();

        long totalViews = valueOrZero(postAnalyticsRepository.getTotalViewsByUser(userId));
        long totalLikes = valueOrZero(postRepository.getTotalLikesByUserId(userId));
        long totalFollowers = connectionRepository.countByFollowingIdAndStatus(
                userId, org.revature.revconnect.enums.ConnectionStatus.ACCEPTED);
        long totalShares = valueOrZero(postRepository.getTotalSharesByUserId(userId));
        long totalComments = valueOrZero(postRepository.getTotalCommentsByUserId(userId));

        Map<String, Object> overview = new HashMap<>();
        overview.put("totalViews", totalViews);
        overview.put("totalLikes", totalLikes);
        overview.put("totalFollowers", totalFollowers);
        overview.put("totalShares", totalShares);
        overview.put("totalComments", totalComments);
        overview.put("totalPosts", postRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 1))
                .getTotalElements());
        return overview;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProfileViews(int days) {
        return buildDailySeries(days, true, false);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPostPerformance(int days) {
        User currentUser = authService.getCurrentUser();
        List<Post> posts = postRepository.findTopPostsByUserId(currentUser.getId(), PageRequest.of(0, 20)).getContent();
        List<Map<String, Object>> response = new ArrayList<>();
        for (Post post : posts) {
            Map<String, Object> row = new HashMap<>();
            row.put("postId", post.getId());
            row.put("content", post.getContent());
            row.put("likes", post.getLikeCount());
            row.put("comments", post.getCommentCount());
            row.put("shares", post.getShareCount());
            row.put("views", valueOrZero(postAnalyticsRepository.getTotalViews(post.getId())));
            response.add(row);
        }
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPostAnalytics(Long postId) {
        User currentUser = authService.getCurrentUser();
        Post post = postRepository.findByIdAndUserId(postId, currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(30);
        List<PostAnalytics> analytics = postAnalyticsRepository.findByPostIdAndDateBetweenOrderByDateAsc(
                postId, start, end);

        long totalViews = analytics.stream().mapToLong(a -> a.getViews() == null ? 0 : a.getViews()).sum();
        long totalImpressions = analytics.stream().mapToLong(a -> a.getImpressions() == null ? 0 : a.getImpressions())
                .sum();

        Map<String, Object> map = new HashMap<>();
        map.put("post", postMapper.toResponse(post));
        map.put("totalViews", totalViews);
        map.put("totalImpressions", totalImpressions);
        map.put("totalLikes", post.getLikeCount());
        map.put("totalComments", post.getCommentCount());
        map.put("totalShares", post.getShareCount());
        map.put("daily", analytics.stream().map(a -> {
            Map<String, Object> row = new HashMap<>();
            row.put("date", a.getDate());
            row.put("views", a.getViews());
            row.put("impressions", a.getImpressions());
            row.put("likes", a.getLikes());
            row.put("comments", a.getComments());
            row.put("shares", a.getShares());
            return row;
        }).toList());
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFollowerGrowth(int days) {
        long currentFollowers = connectionRepository.countByFollowingIdAndStatus(
                authService.getCurrentUser().getId(), org.revature.revconnect.enums.ConnectionStatus.ACCEPTED);
        List<Map<String, Object>> growth = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            Map<String, Object> row = new HashMap<>();
            row.put("date", date);
            row.put("followers", currentFollowers);
            growth.add(row);
        }
        return growth;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEngagement(int days) {
        User currentUser = authService.getCurrentUser();
        long likes = valueOrZero(postRepository.getTotalLikesByUserId(currentUser.getId()));
        long comments = valueOrZero(postRepository.getTotalCommentsByUserId(currentUser.getId()));
        long shares = valueOrZero(postRepository.getTotalSharesByUserId(currentUser.getId()));
        long impressions = valueOrZero(postAnalyticsRepository.getTotalImpressionsByUser(currentUser.getId()));
        long interactions = likes + comments + shares;
        double rate = impressions > 0 ? (interactions * 100.0) / impressions : 0.0;

        Map<String, Object> map = new HashMap<>();
        map.put("totalLikes", likes);
        map.put("totalComments", comments);
        map.put("totalShares", shares);
        map.put("totalInteractions", interactions);
        map.put("engagementRate", Math.round(rate * 100.0) / 100.0);
        map.put("days", days);
        return map;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAudienceDemographics() {
        User currentUser = authService.getCurrentUser();
        List<Long> followerIds = connectionRepository.findFollowerUserIds(currentUser.getId());

        long personal = followerIds.isEmpty() ? 0
                : userRepository.countByIdInAndUserType(followerIds, org.revature.revconnect.enums.UserType.PERSONAL);
        long creator = followerIds.isEmpty() ? 0
                : userRepository.countByIdInAndUserType(followerIds, org.revature.revconnect.enums.UserType.CREATOR);
        long business = followerIds.isEmpty() ? 0
                : userRepository.countByIdInAndUserType(followerIds, org.revature.revconnect.enums.UserType.BUSINESS);

        Map<String, Object> map = new HashMap<>();
        map.put("totalFollowers", followerIds.size());
        map.put("personal", personal);
        map.put("creator", creator);
        map.put("business", business);
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getReach(int days) {
        return buildDailySeries(days, true, false);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getImpressions(int days) {
        return buildDailySeries(days, false, true);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBestTimeToPost() {
        User currentUser = authService.getCurrentUser();
        List<Post> posts = postRepository.findByUserId(currentUser.getId());
        Map<Integer, Long> byHour = new HashMap<>();
        for (Post post : posts) {
            LocalDateTime createdAt = post.getCreatedAt();
            if (createdAt != null) {
                int hour = createdAt.getHour();
                byHour.put(hour, byHour.getOrDefault(hour, 0L) + 1);
            }
        }

        return byHour.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(entry -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("hour", entry.getKey());
                    row.put("postCount", entry.getValue());
                    return row;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopPosts(int limit) {
        User currentUser = authService.getCurrentUser();
        List<Post> posts = postRepository.findTopPostsByUserId(
                currentUser.getId(), PageRequest.of(0, Math.max(1, Math.min(limit, 100)))).getContent();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Post post : posts) {
            Map<String, Object> row = new HashMap<>();
            PostResponse response = postMapper.toResponse(post);
            row.put("post", response);
            row.put("engagementScore", post.getLikeCount() + post.getCommentCount() + post.getShareCount());
            result.add(row);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHashtagPerformance() {
        User currentUser = authService.getCurrentUser();
        List<Post> posts = postRepository.findByUserId(currentUser.getId());
        Map<String, Long> hashtagCount = new HashMap<>();

        for (Post post : posts) {
            if (post.getContent() == null) {
                continue;
            }
            String[] words = post.getContent().split("\\s+");
            for (String word : words) {
                if (word.startsWith("#") && word.length() > 1) {
                    String normalized = word.toLowerCase();
                    hashtagCount.put(normalized, hashtagCount.getOrDefault(normalized, 0L) + 1);
                }
            }
        }

        return hashtagCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(20)
                .map(entry -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("hashtag", entry.getKey());
                    row.put("uses", entry.getValue());
                    return row;
                }).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getContentTypePerformance() {
        User currentUser = authService.getCurrentUser();
        List<Post> posts = postRepository.findByUserId(currentUser.getId());
        Map<String, Long> counts = new HashMap<>();
        for (Post post : posts) {
            String key = post.getPostType().name();
            counts.put(key, counts.getOrDefault(key, 0L) + 1);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("contentTypes", counts);
        result.put("totalPosts", posts.size());
        return result;
    }

    public Map<String, String> exportAnalytics(int days, String format) {
        String normalized = (format == null || format.isBlank()) ? "csv" : format.toLowerCase();
        Map<String, String> map = new HashMap<>();
        map.put("downloadUrl", "/exports/analytics." + normalized);
        map.put("format", normalized);
        map.put("days", String.valueOf(days));
        return map;
    }

    private List<Map<String, Object>> buildDailySeries(int days, boolean includeViews, boolean includeImpressions) {
        User currentUser = authService.getCurrentUser();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(Math.max(1, days) - 1L);
        List<PostAnalytics> rows = postAnalyticsRepository.findByUserIdAndDateRange(currentUser.getId(), start, end);

        Map<LocalDate, long[]> aggregate = new HashMap<>();
        for (PostAnalytics row : rows) {
            long[] values = aggregate.computeIfAbsent(row.getDate(), ignored -> new long[] { 0L, 0L });
            values[0] += row.getViews() == null ? 0 : row.getViews();
            values[1] += row.getImpressions() == null ? 0 : row.getImpressions();
        }

        List<Map<String, Object>> response = new ArrayList<>();
        for (int i = 0; i < Math.max(1, days); i++) {
            LocalDate date = start.plusDays(i);
            long[] values = aggregate.getOrDefault(date, new long[] { 0L, 0L });
            Map<String, Object> map = new HashMap<>();
            map.put("date", date);
            if (includeViews) {
                map.put("views", values[0]);
            }
            if (includeImpressions) {
                map.put("impressions", values[1]);
            }
            if (includeViews && !includeImpressions) {
                map.put("reach", values[0]);
            }
            response.add(map);
        }
        return response;
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
