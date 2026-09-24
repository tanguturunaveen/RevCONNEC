package org.revature.revconnect.repository;

import org.junit.jupiter.api.Test;
import org.revature.revconnect.enums.PostType;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.PostAnalytics;
import org.revature.revconnect.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PostAnalyticsRepositoryTest {

    @Autowired
    private PostAnalyticsRepository postAnalyticsRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByPostIdAndDate_shouldReturnAnalyticsForSpecificDate() {
        User u1 = saveUser("u1", "u1@test.com");
        Post p1 = savePost(u1, "Post Content");

        LocalDate today = LocalDate.now();
        saveAnalytics(p1, today, 100, 50);
        saveAnalytics(p1, today.minusDays(1), 50, 25);

        Optional<PostAnalytics> result = postAnalyticsRepository.findByPostIdAndDate(p1.getId(), today);

        assertTrue(result.isPresent());
        assertEquals(100, result.get().getViews());
        assertEquals(50, result.get().getImpressions());
    }

    @Test
    void findByPostIdAndDateBetweenOrderByDateAsc_shouldReturnAnalyticsInRange() {
        User u1 = saveUser("u1", "u1@test.com");
        Post p1 = savePost(u1, "Post Content");

        LocalDate start = LocalDate.now().minusDays(5);
        LocalDate mid = LocalDate.now().minusDays(3);
        LocalDate end = LocalDate.now().minusDays(1);

        saveAnalytics(p1, start, 10, 5);
        saveAnalytics(p1, end, 30, 15);
        saveAnalytics(p1, mid, 20, 10);

        List<PostAnalytics> result = postAnalyticsRepository.findByPostIdAndDateBetweenOrderByDateAsc(
                p1.getId(), start, end);

        assertEquals(3, result.size());
        assertEquals(start, result.get(0).getDate());
        assertEquals(mid, result.get(1).getDate());
        assertEquals(end, result.get(2).getDate());
    }

    @Test
    void getTotalViews_shouldReturnSumOfViewsForPost() {
        User u1 = saveUser("u1", "u1@test.com");
        Post p1 = savePost(u1, "Post Content");

        saveAnalytics(p1, LocalDate.now(), 100, 50);
        saveAnalytics(p1, LocalDate.now().minusDays(1), 50, 25);

        Long totalViews = postAnalyticsRepository.getTotalViews(p1.getId());

        assertEquals(150L, totalViews);
    }

    @Test
    void getTotalViewsByUser_shouldReturnSumOfViewsForAllUserPosts() {
        User u1 = saveUser("u1", "u1@test.com");
        Post p1 = savePost(u1, "Post 1");
        Post p2 = savePost(u1, "Post 2");

        saveAnalytics(p1, LocalDate.now(), 100, 50);
        saveAnalytics(p2, LocalDate.now(), 200, 100);

        Long totalViews = postAnalyticsRepository.getTotalViewsByUser(u1.getId());

        assertEquals(300L, totalViews);
    }

    @Test
    void getTotalImpressionsByUser_shouldReturnSumOfImpressionsForAllUserPosts() {
        User u1 = saveUser("u1", "u1@test.com");
        Post p1 = savePost(u1, "Post 1");
        Post p2 = savePost(u1, "Post 2");

        saveAnalytics(p1, LocalDate.now(), 100, 50); // 50 impressions
        saveAnalytics(p2, LocalDate.now(), 200, 150); // 150 impressions

        Long totalImpressions = postAnalyticsRepository.getTotalImpressionsByUser(u1.getId());

        assertEquals(200L, totalImpressions);
    }

    @Test
    void findByUserIdAndDateRange_shouldReturnAnalyticsForUserInRange() {
        User u1 = saveUser("u1", "u1@test.com");
        Post p1 = savePost(u1, "Post 1");
        Post p2 = savePost(u1, "Post 2");

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        saveAnalytics(p1, today, 100, 50);
        saveAnalytics(p2, yesterday, 200, 100);
        saveAnalytics(p1, today.minusDays(10), 10, 5); // Outside range

        List<PostAnalytics> result = postAnalyticsRepository.findByUserIdAndDateRange(
                u1.getId(), yesterday, today);

        assertEquals(2, result.size());
        assertEquals(yesterday, result.get(0).getDate()); // Ordered ASC
        assertEquals(today, result.get(1).getDate());
    }

    private User saveUser(String username, String email) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password("pwd")
                .name(username)
                .privacy(Privacy.PUBLIC)
                .userType(UserType.PERSONAL)
                .build());
    }

    private Post savePost(User user, String content) {
        return postRepository.save(Post.builder()
                .user(user)
                .content(content)
                .postType(PostType.TEXT)
                .build());
    }

    private PostAnalytics saveAnalytics(Post post, LocalDate date, int views, int impressions) {
        return postAnalyticsRepository.save(PostAnalytics.builder()
                .post(post)
                .date(date)
                .views(views)
                .impressions(impressions)
                .likes(0)
                .comments(0)
                .shares(0)
                .clicks(0)
                .build());
    }
}