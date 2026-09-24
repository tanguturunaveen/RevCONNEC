package org.revature.revconnect.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.dto.response.NotificationResponse;
import org.revature.revconnect.enums.NotificationType;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.mapper.NotificationMapper;
import org.revature.revconnect.model.Notification;
import org.revature.revconnect.model.User;
import org.revature.revconnect.model.UserSettings;
import org.revature.revconnect.repository.NotificationRepository;
import org.revature.revconnect.repository.UserSettingsRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private AuthService authService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private NotificationMapper notificationMapper;
    @Mock private UserSettingsRepository userSettingsRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void getUnreadCount_returnsRepositoryValue() {
        User me = user(1L, "u1");
        when(authService.getCurrentUser()).thenReturn(me);
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(3L);
        assertEquals(3L, notificationService.getUnreadCount());
    }

    @Test
    void createNotification_selfNotification_isSkipped() {
        User me = user(1L, "u1");
        notificationService.createNotification(me, me, NotificationType.LIKE, "self", 10L);
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createNotification_savesAndPushes_whenEnabled() {
        User recipient = user(1L, "r");
        User actor = user(2L, "a");
        Notification n = Notification.builder().id(1L).user(recipient).actor(actor)
                .type(NotificationType.LIKE).message("msg").referenceId(10L).build();

        when(userSettingsRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenReturn(n);
        when(notificationMapper.toResponse(n)).thenReturn(NotificationResponse.builder().id(1L).build());

        notificationService.createNotification(recipient, actor, NotificationType.LIKE, "msg", 10L);

        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/notifications/1"), any(NotificationResponse.class));
    }

    @Test
    void createNotification_skipsWhenDisabledInSettings() {
        User recipient = user(1L, "r");
        User actor = user(2L, "a");
        UserSettings settings = UserSettings.builder().notifyLike(false).build();
        when(userSettingsRepository.findByUserId(1L)).thenReturn(Optional.of(settings));

        notificationService.createNotification(recipient, actor, NotificationType.LIKE, "msg", 10L);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void getNotifications_returnsPagedData() {
        User me = user(1L, "u1");
        Notification n = Notification.builder().id(5L).user(me).build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(java.util.List.of(n), PageRequest.of(0, 10), 1));
        when(notificationMapper.toResponse(n)).thenReturn(NotificationResponse.builder().id(5L).build());

        var page = notificationService.getNotifications(0, 10);
        assertEquals(1, page.getContent().size());
        assertEquals(1L, page.getTotalElements());
    }

    @Test
    void markAsRead_ownNotification_success() {
        User me = user(1L, "u1");
        Notification n = Notification.builder().id(7L).user(me).isRead(false).build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(notificationRepository.findById(7L)).thenReturn(Optional.of(n));

        notificationService.markAsRead(7L);

        assertEquals(true, n.getIsRead());
        verify(notificationRepository).save(n);
    }

    @Test
    void markAsRead_foreignNotification_throws() {
        User me = user(1L, "u1");
        User other = user(2L, "u2");
        Notification n = Notification.builder().id(8L).user(other).isRead(false).build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(notificationRepository.findById(8L)).thenReturn(Optional.of(n));

        assertThrows(UnauthorizedException.class, () -> notificationService.markAsRead(8L));
    }

    @Test
    void deleteNotification_own_success() {
        User me = user(1L, "u1");
        Notification n = Notification.builder().id(9L).user(me).build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(notificationRepository.findById(9L)).thenReturn(Optional.of(n));

        notificationService.deleteNotification(9L);

        verify(notificationRepository).delete(n);
    }

    @Test
    void deleteNotification_foreign_throws() {
        User me = user(1L, "u1");
        User other = user(2L, "u2");
        Notification n = Notification.builder().id(10L).user(other).build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(n));

        assertThrows(UnauthorizedException.class, () -> notificationService.deleteNotification(10L));
    }

    @Test
    void markAllAsRead_returnsCount() {
        User me = user(1L, "u1");
        when(authService.getCurrentUser()).thenReturn(me);
        when(notificationRepository.markAllAsRead(1L)).thenReturn(4);

        assertEquals(4, notificationService.markAllAsRead());
    }

    private User user(Long id, String username) {
        return User.builder().id(id).username(username).name(username)
                .email(username + "@test.com").password("x").build();
    }
}
