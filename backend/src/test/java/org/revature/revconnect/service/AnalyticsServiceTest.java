package org.revature.revconnect.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.enums.ConnectionStatus;
import org.revature.revconnect.enums.PostType;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.mapper.PostMapper;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.PostAnalytics;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.ConnectionRepository;
import org.revature.revconnect.repository.PostAnalyticsRepository;
import org.revature.revconnect.repository.PostRepository;
import org.revature.revconnect.repository.UserRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private AuthService authService;
    @Mock private PostRepository postRepository;
    @Mock private PostAnalyticsRepository postAnalyticsRepository;
    @Mock private ConnectionRepository connectionRepository;
    @Mock private UserRepository userRepository;
    @Mock private PostMapper postMapper;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void getOverview_returnsAggregatedValues() {
        User me = user(1L, "me");
        when(authService.getCurrentUser()).thenReturn(me);
        when(postAnalyticsRepository.getTotalViewsByUser(1L)).thenReturn(100L);
        when(postRepository.getTotalLikesByUserId(1L)).thenReturn(25L);
        when(connectionRepository.countByFollowingIdAndStatus(1L, ConnectionStatus.ACCEPTED)).thenReturn(7L);
        when(postRepository.findByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 1)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 9));

        Map<String, Object> result = analyticsService.getOverview();

        assertEquals(100L, result.get("totalViews"));
        assertEquals(25L, result.get("totalLikes"));
        assertEquals(7L, result.get("totalFollowers"));
        assertEquals(9L, result.get("totalPosts"));
    }

    @Test
    void getOverview_whenNulls_returnsZeroForNullableMetrics() {
        User me = user(1L, "me");
        when(authService.getCurrentUser()).thenReturn(me);
        when(postAnalyticsRepository.getTotalViewsByUser(1L)).thenReturn(null);
        when(postRepository.getTotalLikesByUserId(1L)).thenReturn(null);
        when(connectionRepository.countByFollowingIdAndStatus(1L, ConnectionStatus.ACCEPTED)).thenReturn(0L);
        when(postRepository.findByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 1)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));

        Map<String, Object> result = analyticsService.getOverview();

        assertEquals(0L, result.get("totalViews"));
        assertEquals(0L, result.get("totalLikes"));
    }

    @Test
    void getProfileViews_buildsDailySeriesWithReachAndViews() {
        User me = user(2L, "me2");
        LocalDate today = LocalDate.now();
        PostAnalytics row = PostAnalytics.builder()
                .date(today)
                .views(12)
                .impressions(50)
                .build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(postAnalyticsRepository.findByUserIdAndDateRange(eq(2L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(row));

        List<Map<String, Object>> result = analyticsService.getProfileViews(3);

        assertEquals(3, result.size());
        Map<String, Object> last = result.get(2);
        assertEquals(12L, last.get("views"));
        assertEquals(12L, last.get("reach"));
    }

    @Test
    void getPostPerformance_returnsRowsForTopPosts() {
        User me = user(3L, "me3");
        Post p = post(10L, me, "content #x", PostType.TEXT, 5, 2, 1);
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findTopPostsByUserId(3L, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 20), 1));
        when(postAnalyticsRepository.getTotalViews(10L)).thenReturn(88L);

        List<Map<String, Object>> result = analyticsService.getPostPerformance(30);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).get("postId"));
        assertEquals(88L, result.get(0).get("views"));
    }

    @Test
    void getPostAnalytics_whenPostMissing_throwsNotFound() {
        User me = user(4L, "me4");
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findByIdAndUserId(99L, 4L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> analyticsService.getPostAnalytics(99L));
    }

    @Test
    void getPostAnalytics_returnsTotalsAndDaily() {
        User me = user(4L, "me4");
        Post p = post(11L, me, "hello", PostType.TEXT, 7, 3, 2);
        PostResponse postResponse = PostResponse.builder().id(11L).build();
        LocalDate today = LocalDate.now();
        PostAnalytics a1 = PostAnalytics.builder().date(today.minusDays(1)).views(10).impressions(20).likes(2).comments(1).shares(1).build();
        PostAnalytics a2 = PostAnalytics.builder().date(today).views(5).impressions(15).likes(1).comments(1).shares(0).build();

        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findByIdAndUserId(11L, 4L)).thenReturn(Optional.of(p));
        when(postMapper.toResponse(p)).thenReturn(postResponse);
        when(postAnalyticsRepository.findByPostIdAndDateBetweenOrderByDateAsc(eq(11L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(a1, a2));

        Map<String, Object> result = analyticsService.getPostAnalytics(11L);

        assertEquals(15L, result.get("totalViews"));
        assertEquals(35L, result.get("totalImpressions"));
        assertEquals(7, result.get("totalLikes"));
        assertEquals(2, ((List<?>) result.get("daily")).size());
    }

    @Test
    void getFollowerGrowth_returnsFlatSeriesByDays() {
        User me = user(5L, "me5");
        when(authService.getCurrentUser()).thenReturn(me);
        when(connectionRepository.countByFollowingIdAndStatus(5L, ConnectionStatus.ACCEPTED)).thenReturn(11L);

        List<Map<String, Object>> result = analyticsService.getFollowerGrowth(4);

        assertEquals(4, result.size());
        assertEquals(11L, result.get(0).get("followers"));
        assertEquals(11L, result.get(3).get("followers"));
    }

    @Test
    void getEngagement_calculatesRateAndTotals() {
        User me = user(6L, "me6");
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.getTotalLikesByUserId(6L)).thenReturn(20L);
        when(postRepository.getTotalCommentsByUserId(6L)).thenReturn(10L);
        when(postRepository.getTotalSharesByUserId(6L)).thenReturn(5L);
        when(postAnalyticsRepository.getTotalImpressionsByUser(6L)).thenReturn(70L);

        Map<String, Object> result = analyticsService.getEngagement(7);

        assertEquals(35L, result.get("totalInteractions"));
        assertEquals(50.0, result.get("engagementRate"));
        assertEquals(7, result.get("days"));
    }

    @Test
    void getAudienceDemographics_withFollowers_returnsTypeCounts() {
        User me = user(7L, "me7");
        when(authService.getCurrentUser()).thenReturn(me);
        when(connectionRepository.findFollowerUserIds(7L)).thenReturn(List.of(1L, 2L, 3L));
        when(userRepository.countByIdInAndUserType(List.of(1L, 2L, 3L), UserType.PERSONAL)).thenReturn(1L);
        when(userRepository.countByIdInAndUserType(List.of(1L, 2L, 3L), UserType.CREATOR)).thenReturn(1L);
        when(userRepository.countByIdInAndUserType(List.of(1L, 2L, 3L), UserType.BUSINESS)).thenReturn(1L);

        Map<String, Object> result = analyticsService.getAudienceDemographics();

        assertEquals(3, result.get("totalFollowers"));
        assertEquals(1L, result.get("personal"));
        assertEquals(1L, result.get("creator"));
        assertEquals(1L, result.get("business"));
    }

    @Test
    void getAudienceDemographics_noFollowers_returnsZeros() {
        User me = user(8L, "me8");
        when(authService.getCurrentUser()).thenReturn(me);
        when(connectionRepository.findFollowerUserIds(8L)).thenReturn(List.of());

        Map<String, Object> result = analyticsService.getAudienceDemographics();

        assertEquals(0, result.get("totalFollowers"));
        assertEquals(0L, result.get("personal"));
    }

    @Test
    void getReach_and_getImpressions_buildExpectedSeries() {
        User me = user(9L, "me9");
        LocalDate today = LocalDate.now();
        PostAnalytics row = PostAnalytics.builder().date(today).views(9).impressions(19).build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(postAnalyticsRepository.findByUserIdAndDateRange(eq(9L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(row));

        List<Map<String, Object>> reach = analyticsService.getReach(2);
        List<Map<String, Object>> impressions = analyticsService.getImpressions(2);

        assertEquals(2, reach.size());
        assertEquals(9L, reach.get(1).get("reach"));
        assertEquals(19L, impressions.get(1).get("impressions"));
    }

    @Test
    void getBestTimeToPost_returnsTopHoursByCount() {
        User me = user(10L, "me10");
        Post p1 = post(1L, me, "a", PostType.TEXT, 0, 0, 0);
        p1.setCreatedAt(LocalDateTime.of(2026, 2, 1, 10, 0));
        Post p2 = post(2L, me, "b", PostType.TEXT, 0, 0, 0);
        p2.setCreatedAt(LocalDateTime.of(2026, 2, 2, 10, 0));
        Post p3 = post(3L, me, "c", PostType.TEXT, 0, 0, 0);
        p3.setCreatedAt(LocalDateTime.of(2026, 2, 3, 9, 0));
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findByUserId(10L)).thenReturn(List.of(p1, p2, p3));

        List<Map<String, Object>> result = analyticsService.getBestTimeToPost();

        assertEquals(2, result.size());
        assertEquals(10, result.get(0).get("hour"));
        assertEquals(2L, result.get(0).get("postCount"));
    }

    @Test
    void getTopPosts_mapsPostAndEngagementScore() {
        User me = user(11L, "me11");
        Post p = post(100L, me, "hi", PostType.TEXT, 4, 2, 1);
        PostResponse response = PostResponse.builder().id(100L).content("hi").build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findTopPostsByUserId(11L, PageRequest.of(0, 3)))
                .thenReturn(new PageImpl<>(List.of(p), PageRequest.of(0, 3), 1));
        when(postMapper.toResponse(p)).thenReturn(response);

        List<Map<String, Object>> result = analyticsService.getTopPosts(3);

        assertEquals(1, result.size());
        assertEquals(7, result.get(0).get("engagementScore"));
    }

    @Test
    void getHashtagPerformance_countsAndSortsHashtags() {
        User me = user(12L, "me12");
        Post p1 = post(1L, me, "#Java #Spring #java", PostType.TEXT, 0, 0, 0);
        Post p2 = post(2L, me, "NoTag here", PostType.TEXT, 0, 0, 0);
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findByUserId(12L)).thenReturn(List.of(p1, p2));

        List<Map<String, Object>> result = analyticsService.getHashtagPerformance();

        assertEquals("#java", result.get(0).get("hashtag"));
        assertEquals(2L, result.get(0).get("uses"));
    }

    @Test
    void getContentTypePerformance_returnsCountsAndTotal() {
        User me = user(13L, "me13");
        Post p1 = post(1L, me, "a", PostType.TEXT, 0, 0, 0);
        Post p2 = post(2L, me, "b", PostType.IMAGE, 0, 0, 0);
        Post p3 = post(3L, me, "c", PostType.TEXT, 0, 0, 0);
        when(authService.getCurrentUser()).thenReturn(me);
        when(postRepository.findByUserId(13L)).thenReturn(List.of(p1, p2, p3));

        Map<String, Object> result = analyticsService.getContentTypePerformance();

        Map<?, ?> contentTypes = (Map<?, ?>) result.get("contentTypes");
        assertEquals(2L, contentTypes.get("TEXT"));
        assertEquals(1L, contentTypes.get("IMAGE"));
        assertEquals(3, result.get("totalPosts"));
    }

    @Test
    void exportAnalytics_defaultsToCsvWhenFormatBlank() {
        Map<String, String> result = analyticsService.exportAnalytics(30, " ");
        assertEquals("csv", result.get("format"));
        assertEquals("/exports/analytics.csv", result.get("downloadUrl"));
        assertEquals("30", result.get("days"));
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

    private Post post(Long id, User owner, String content, PostType type, int likes, int comments, int shares) {
        return Post.builder()
                .id(id)
                .user(owner)
                .content(content)
                .postType(type)
                .likeCount(likes)
                .commentCount(comments)
                .shareCount(shares)
                .build();
    }
}
