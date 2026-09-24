package org.revature.revconnect.repository;

import org.junit.jupiter.api.Test;
import org.revature.revconnect.enums.NotificationType;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.model.Notification;
import org.revature.revconnect.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUserIdOrderByCreatedAtDesc_shouldReturnNotifications() {
        User u1 = saveUser("u1", "u1@test.com");
        User actor = saveUser("actor", "actor@test.com");

        saveNotification(u1, actor, "Msg 1", true);
        saveNotification(u1, actor, "Msg 2", false);

        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(u1.getId(),
                PageRequest.of(0, 5));

        assertEquals(2, notifications.getTotalElements());
    }

    @Test
    void findByUserIdAndIsReadFalseOrderByCreatedAtDesc_shouldReturnUnread() {
        User u1 = saveUser("u1", "u1@test.com");
        User actor = saveUser("actor", "actor@test.com");

        saveNotification(u1, actor, "Read Msg", true);
        saveNotification(u1, actor, "Unread Msg", false);

        Page<Notification> unread = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(u1.getId(),
                PageRequest.of(0, 5));

        assertEquals(1, unread.getTotalElements());
        assertEquals("Unread Msg", unread.getContent().get(0).getMessage());
    }

    @Test
    void countByUserIdAndIsReadFalse_shouldReturnCount() {
        User u1 = saveUser("u1", "u1@test.com");
        User actor = saveUser("actor", "actor@test.com");

        saveNotification(u1, actor, "N1", false);
        saveNotification(u1, actor, "N2", false);
        saveNotification(u1, actor, "N3", true);

        long count = notificationRepository.countByUserIdAndIsReadFalse(u1.getId());

        assertEquals(2, count);
    }

    @Test
    void markAllAsRead_shouldUpdateUnreadStatus() {
        User u1 = saveUser("u1", "u1@test.com");
        User actor = saveUser("actor", "actor@test.com");

        saveNotification(u1, actor, "N1", false);
        saveNotification(u1, actor, "N2", false);

        int updatedCount = notificationRepository.markAllAsRead(u1.getId());

        assertEquals(2, updatedCount);
        assertEquals(0, notificationRepository.countByUserIdAndIsReadFalse(u1.getId()));
    }

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Test
    void deleteOldNotifications_shouldRemoveNotificationsOlderThanCutoff() {
        User u1 = saveUser("u1", "u1@test.com");
        User actor = saveUser("actor", "actor@test.com");

        // Use jdbc template to bypass @CreationTimestamp for old record
        jdbcTemplate.update("INSERT INTO notifications (user_id, actor_id, type, message, is_read, created_at) " +
                        "VALUES (?, ?, 'LIKE', 'Old', true, ?)", u1.getId(), actor.getId(),
                java.sql.Timestamp.valueOf(LocalDateTime.now().minusDays(10)));

        Notification newNotif = Notification.builder()
                .user(u1)
                .actor(actor)
                .message("New")
                .type(NotificationType.LIKE)
                .isRead(false)
                .build();
        notificationRepository.save(newNotif);

        int deletedCount = notificationRepository.deleteOldNotifications(LocalDateTime.now().minusDays(5));

        assertEquals(1, deletedCount);
        assertEquals(1, notificationRepository.count());
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

    private Notification saveNotification(User user, User actor, String message, boolean isRead) {
        Notification notification = Notification.builder()
                .user(user)
                .actor(actor)
                .message(message)
                .type(NotificationType.LIKE)
                .isRead(isRead)
                .build();
        return notificationRepository.save(notification);
    }
}