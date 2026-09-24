package org.revature.revconnect.repository;

import org.junit.jupiter.api.Test;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.model.Story;
import org.revature.revconnect.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class StoryRepositoryTest {

    @Autowired
    private StoryRepository storyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findByUserAndExpiresAtAfterOrderByCreatedAtDesc_shouldReturnActiveStories() {
        User u1 = saveUser("u1", "u1@test.com");

        saveStory(u1, "Active Story", false);

        List<Story> active = storyRepository.findByUserAndExpiresAtAfterOrderByCreatedAtDesc(u1, LocalDateTime.now());

        assertEquals(1, active.size());
        assertEquals("Active Story", active.get(0).getCaption());
    }

    @Test
    void findActiveStoriesByUsers_shouldReturnActiveStoriesForMultipleUsers() {
        User u1 = saveUser("u1", "u1@test.com");
        User u2 = saveUser("u2", "u2@test.com");

        saveStory(u1, "U1 Active", false);
        saveStory(u2, "U2 Active", false);

        List<Story> active = storyRepository.findActiveStoriesByUsers(List.of(u1, u2), LocalDateTime.now());

        assertEquals(2, active.size());
    }

    @Test
    void findByUserAndIsHighlightTrueOrderByCreatedAtDesc_shouldReturnHighlights() {
        User u1 = saveUser("u1", "u1@test.com");

        saveStory(u1, "Normal", false);
        saveStory(u1, "Highlight", true);

        List<Story> highlights = storyRepository.findByUserAndIsHighlightTrueOrderByCreatedAtDesc(u1);

        assertEquals(1, highlights.size());
        assertEquals("Highlight", highlights.get(0).getCaption());
    }

    @Test
    void findByUserAndExpiresAtBeforeOrderByCreatedAtDesc_shouldReturnExpiredForUser() {
        User u1 = saveUser("u1", "u1@test.com");

        // Insert expired story natively to bypass @PrePersist overrides
        jdbcTemplate.update(
                "INSERT INTO stories (user_id, caption, is_highlight, view_count, created_at, expires_at) " +
                        "VALUES (?, 'Expired', false, 0, ?, ?)",
                u1.getId(),
                java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(2)),
                java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(1)));

        List<Story> expired = storyRepository.findByUserAndExpiresAtBeforeOrderByCreatedAtDesc(u1, LocalDateTime.now());

        assertEquals(1, expired.size());
        assertEquals("Expired", expired.get(0).getCaption());
    }

    @Test
    void findExpiredStories_shouldReturnAllExpired() {
        User u1 = saveUser("u1", "u1@test.com");
        User u2 = saveUser("u2", "u2@test.com");

        // Insert expired stories natively to bypass @PrePersist overrides
        jdbcTemplate.update(
                "INSERT INTO stories (user_id, caption, is_highlight, view_count, created_at, expires_at) " +
                        "VALUES (?, 'Expired U1', false, 0, ?, ?)",
                u1.getId(),
                java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(2)),
                java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(1)));

        jdbcTemplate.update(
                "INSERT INTO stories (user_id, caption, is_highlight, view_count, created_at, expires_at) " +
                        "VALUES (?, 'Expired U2', false, 0, ?, ?)",
                u2.getId(),
                java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(2)),
                java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(1)));

        List<Story> expired = storyRepository.findExpiredStories(LocalDateTime.now());

        assertEquals(2, expired.size());
        assertTrue(expired.stream().anyMatch(s -> s.getCaption().equals("Expired U1")));
        assertTrue(expired.stream().anyMatch(s -> s.getCaption().equals("Expired U2")));
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

    private Story saveStory(User user, String caption, boolean isHighlight) {
        return storyRepository.save(Story.builder()
                .user(user)
                .caption(caption)
                .isHighlight(isHighlight)
                .viewCount(0)
                .build());
    }
}