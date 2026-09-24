package org.revature.revconnect.controller;

import org.revature.revconnect.dto.response.*;
import org.revature.revconnect.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Notification Management APIs")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications for current user")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Get notifications request - page: {}, size: {}", page, size);
        PagedResponse<NotificationResponse> notifications = notificationService.getNotifications(page, size);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread")
    @Operation(summary = "Get unread notifications for current user")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> getUnreadNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Get unread notifications request");
        PagedResponse<NotificationResponse> notifications = notificationService.getUnreadNotifications(page, size);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        log.info("Get unread count request");
        long count = notificationService.getUnreadCount();
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long notificationId) {
        log.info("Mark as read request for notification: {}", notificationId);
        notificationService.markAsRead(notificationId);
        log.info("Notification {} marked as read", notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead() {
        log.info("Mark all as read request");
        int count = notificationService.markAllAsRead();
        log.info("Marked {} notifications as read", count);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", Map.of("markedCount", count)));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long notificationId) {
        log.info("Delete notification request for ID: {}", notificationId);
        notificationService.deleteNotification(notificationId);
        log.info("Notification {} deleted successfully", notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully", null));
    }
}
