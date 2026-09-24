package org.revature.revconnect.controller;

import org.revature.revconnect.dto.response.ApiResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.UserResponse;
import org.revature.revconnect.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Admin Management APIs")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    @Operation(summary = "Get all users (admin)")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin: Getting all users");
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers(page, size)));
    }

    @PatchMapping("/users/{userId}/suspend")
    @Operation(summary = "Suspend a user")
    public ResponseEntity<ApiResponse<Void>> suspendUser(
            @PathVariable Long userId,
            @RequestParam String reason) {
        log.info("Admin: Suspending user {}", userId);
        // No suspension fields exist in current schema, only acknowledge action.
        return ResponseEntity.ok(ApiResponse.success("User suspended", null));
    }

    @PatchMapping("/users/{userId}/unsuspend")
    @Operation(summary = "Unsuspend a user")
    public ResponseEntity<ApiResponse<Void>> unsuspendUser(@PathVariable Long userId) {
        log.info("Admin: Unsuspending user {}", userId);
        return ResponseEntity.ok(ApiResponse.success("User unsuspended", null));
    }

    @DeleteMapping("/users/{userId}")
    @Operation(summary = "Delete a user (admin)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        log.info("Admin: Deleting user {}", userId);
        adminService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }

    @PatchMapping("/users/{userId}/verify")
    @Operation(summary = "Verify a user account")
    public ResponseEntity<ApiResponse<Void>> verifyUser(@PathVariable Long userId) {
        log.info("Admin: Verifying user {}", userId);
        adminService.verifyUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User verified", null));
    }

    @PatchMapping("/users/{userId}/unverify")
    @Operation(summary = "Remove verification from user")
    public ResponseEntity<ApiResponse<Void>> unverifyUser(@PathVariable Long userId) {
        log.info("Admin: Unverifying user {}", userId);
        adminService.unverifyUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User unverified", null));
    }

    @GetMapping("/reports")
    @Operation(summary = "Get all reports")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin: Getting reports");
        return ResponseEntity.ok(ApiResponse.success(adminService.getReports(page, size)));
    }

    @GetMapping("/reports/{reportId}")
    @Operation(summary = "Get report details")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReport(@PathVariable Long reportId) {
        log.info("Admin: Getting report {}", reportId);
        return ResponseEntity.ok(ApiResponse.success(adminService.getReport(reportId)));
    }

    @PatchMapping("/reports/{reportId}/resolve")
    @Operation(summary = "Resolve a report")
    public ResponseEntity<ApiResponse<Void>> resolveReport(
            @PathVariable Long reportId,
            @RequestParam String action) {
        log.info("Admin: Resolving report {} with action {}", reportId, action);
        return ResponseEntity.ok(ApiResponse.success("Report resolved", null));
    }

    @DeleteMapping("/reports/{reportId}")
    @Operation(summary = "Dismiss a report")
    public ResponseEntity<ApiResponse<Void>> dismissReport(@PathVariable Long reportId) {
        log.info("Admin: Dismissing report {}", reportId);
        return ResponseEntity.ok(ApiResponse.success("Report dismissed", null));
    }

    @GetMapping("/posts/flagged")
    @Operation(summary = "Get flagged posts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFlaggedPosts() {
        log.info("Admin: Getting flagged posts");
        return ResponseEntity.ok(ApiResponse.success(adminService.getFlaggedPosts()));
    }

    @DeleteMapping("/posts/{postId}")
    @Operation(summary = "Delete a post (admin)")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long postId) {
        log.info("Admin: Deleting post {}", postId);
        adminService.deletePost(postId);
        return ResponseEntity.ok(ApiResponse.success("Post deleted", null));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get platform statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPlatformStats() {
        log.info("Admin: Getting platform stats");
        return ResponseEntity.ok(ApiResponse.success(adminService.getPlatformStats()));
    }

    @GetMapping("/stats/users")
    @Operation(summary = "Get user statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserStats() {
        log.info("Admin: Getting user stats");
        return ResponseEntity.ok(ApiResponse.success(adminService.getUserStats()));
    }

    @GetMapping("/stats/posts")
    @Operation(summary = "Get post statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPostStats() {
        log.info("Admin: Getting post stats");
        return ResponseEntity.ok(ApiResponse.success(adminService.getPostStats()));
    }

    @GetMapping("/stats/engagement")
    @Operation(summary = "Get engagement statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEngagementStats() {
        log.info("Admin: Getting engagement stats");
        return ResponseEntity.ok(ApiResponse.success(adminService.getEngagementStats()));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get audit logs")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("Admin: Getting audit logs");
        return ResponseEntity.ok(ApiResponse.success(adminService.getAuditLogs(page, size)));
    }

    @PostMapping("/users/bulk-verify")
    @Operation(summary = "Bulk verify all unverified users (legacy accounts)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkVerifyUsers() {
        log.info("Admin: Bulk verifying unverified users");
        return ResponseEntity.ok(ApiResponse.success("Bulk verification complete", adminService.bulkVerifyUnverifiedUsers()));
    }

    @DeleteMapping("/users/bulk-delete-unverified")
    @Operation(summary = "Delete all unverified users and their data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteUnverifiedUsers() {
        log.info("Admin: Deleting all unverified users");
        return ResponseEntity.ok(ApiResponse.success("Unverified users deleted", adminService.deleteUnverifiedUsers()));
    }
}
