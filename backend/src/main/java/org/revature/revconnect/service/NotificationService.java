package org.revature.revconnect.service;

import org.revature.revconnect.dto.response.NotificationResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.enums.NotificationType;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.mapper.NotificationMapper;
import org.revature.revconnect.model.Notification;
import org.revature.revconnect.model.User;
import org.revature.revconnect.model.UserSettings;
import org.revature.revconnect.repository.NotificationRepository;
import org.revature.revconnect.repository.UserSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AuthService authService;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationMapper notificationMapper;
    private final UserSettingsRepository userSettingsRepository;

    @Transactional
    public void createNotification(User recipient, User actor, NotificationType type, String message,
                                   Long referenceId) {
        log.info("Creating notification for user {} from actor {}: {}", recipient.getUsername(), actor.getUsername(),
                type);

        if (recipient.getId().equals(actor.getId())) {
            log.debug("Skipping self-notification");
            return;
        }

        if (!isNotificationEnabled(recipient.getId(), type)) {
            log.debug("Notification type {} disabled by user {}", type, recipient.getId());
            return;
        }

        Notification notification = Notification.builder()
                .user(recipient)
                .actor(actor)
                .type(type)
                .message(message)
                .referenceId(referenceId)
                .build();

        Notification saved = notificationRepository.save(notification);
        log.info("Notification created for user {}", recipient.getUsername());

        try {
            messagingTemplate.convertAndSend(
                    "/topic/notifications/" + recipient.getId(),
                    notificationMapper.toResponse(saved));
            log.info("Real-time notification pushed to user {}", recipient.getUsername());
        } catch (Exception e) {
            log.warn("Failed to push real-time notification (user may not be connected): {}", e.getMessage());
        }
    }

    public PagedResponse<NotificationResponse> getNotifications(int page, int size) {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching notifications for user: {}", currentUser.getUsername());

        Page<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                currentUser.getId(), PageRequest.of(page, size));

        log.info("Found {} notifications for user {}", notifications.getTotalElements(), currentUser.getUsername());
        return PagedResponse.fromEntityPage(notifications, notificationMapper::toResponse);
    }

    public PagedResponse<NotificationResponse> getUnreadNotifications(int page, int size) {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching unread notifications for user: {}", currentUser.getUsername());

        Page<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(
                currentUser.getId(), PageRequest.of(page, size));

        log.info("Found {} unread notifications for user {}", notifications.getTotalElements(),
                currentUser.getUsername());
        return PagedResponse.fromEntityPage(notifications, notificationMapper::toResponse);
    }

    public long getUnreadCount() {
        User currentUser = authService.getCurrentUser();
        long count = notificationRepository.countByUserIdAndIsReadFalse(currentUser.getId());
        log.info("User {} has {} unread notifications", currentUser.getUsername(), count);
        return count;
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        User currentUser = authService.getCurrentUser();
        log.info("Marking notification {} as read for user {}", notificationId, currentUser.getUsername());

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            log.warn("User {} attempted to mark notification {} owned by another user", currentUser.getUsername(),
                    notificationId);
            throw new UnauthorizedException("You can only mark your own notifications as read");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
        log.info("Notification {} marked as read", notificationId);
    }

    @Transactional
    public int markAllAsRead() {
        User currentUser = authService.getCurrentUser();
        log.info("Marking all notifications as read for user: {}", currentUser.getUsername());

        int count = notificationRepository.markAllAsRead(currentUser.getId());
        log.info("Marked {} notifications as read for user {}", count, currentUser.getUsername());
        return count;
    }

    @Transactional
    public void deleteNotification(Long notificationId) {
        User currentUser = authService.getCurrentUser();
        log.info("Deleting notification {} for user {}", notificationId, currentUser.getUsername());

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            log.warn("User {} attempted to delete notification {} owned by another user", currentUser.getUsername(),
                    notificationId);
            throw new UnauthorizedException("You can only delete your own notifications");
        }

        notificationRepository.delete(notification);
        log.info("Notification {} deleted successfully", notificationId);
    }

    public void notifyLike(User postOwner, User liker, Long postId) {
        createNotification(postOwner, liker, NotificationType.LIKE,
                liker.getName() + " liked your post", postId);
    }

    public void notifyComment(User postOwner, User commenter, Long postId) {
        createNotification(postOwner, commenter, NotificationType.COMMENT,
                commenter.getName() + " commented on your post", postId);
    }

    public void notifyShare(User postOwner, User sharer, Long postId) {
        createNotification(postOwner, sharer, NotificationType.SHARE,
                sharer.getName() + " shared your post", postId);
    }

    public void notifyFollow(User followed, User follower) {
        createNotification(followed, follower, NotificationType.NEW_FOLLOWER,
                follower.getName() + " started following you", null);
    }

    public void notifyConnectionRequest(User recipient, User sender) {
        createNotification(recipient, sender, NotificationType.CONNECTION_REQUEST,
                sender.getName() + " sent you a connection request", null);
    }

    public void notifyConnectionAccepted(User requester, User acceptor) {
        createNotification(requester, acceptor, NotificationType.CONNECTION_ACCEPTED,
                acceptor.getName() + " accepted your connection request", null);
    }

    private boolean isNotificationEnabled(Long userId, NotificationType type) {
        UserSettings settings = userSettingsRepository.findByUserId(userId).orElse(null);
        if (settings == null) {
            return true;
        }
        return switch (type) {
            case LIKE -> Boolean.TRUE.equals(settings.getNotifyLike());
            case COMMENT -> Boolean.TRUE.equals(settings.getNotifyComment());
            case SHARE -> Boolean.TRUE.equals(settings.getNotifyShare());
            case NEW_FOLLOWER -> Boolean.TRUE.equals(settings.getNotifyNewFollower());
            case CONNECTION_REQUEST -> Boolean.TRUE.equals(settings.getNotifyConnectionRequest());
            case CONNECTION_ACCEPTED -> Boolean.TRUE.equals(settings.getNotifyConnectionAccepted());
        };
    }
}
