package org.revature.revconnect.controller;

import org.revature.revconnect.dto.response.*;
import org.revature.revconnect.service.ConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Connections", description = "Follow/Unfollow and Connection Management APIs")
public class ConnectionController {

    private final ConnectionService connectionService;

    @PostMapping("/users/{userId}/follow")
    @Operation(summary = "Follow a user")
    public ResponseEntity<ApiResponse<Void>> followUser(@PathVariable Long userId) {
        log.info("Follow user request for user ID: {}", userId);
        connectionService.followUser(userId);
        log.info("Successfully followed user ID: {}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Successfully followed user", null));
    }

    @DeleteMapping("/users/{userId}/follow")
    @Operation(summary = "Unfollow a user")
    public ResponseEntity<ApiResponse<Void>> unfollowUser(@PathVariable Long userId) {
        log.info("Unfollow user request for user ID: {}", userId);
        connectionService.unfollowUser(userId);
        log.info("Successfully unfollowed user ID: {}", userId);
        return ResponseEntity.ok(ApiResponse.success("Successfully unfollowed user", null));
    }

    @GetMapping("/users/{userId}/followers")
    @Operation(summary = "Get followers of a user")
    public ResponseEntity<ApiResponse<PagedResponse<ConnectionResponse>>> getFollowers(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get followers request for user ID: {}", userId);
        PagedResponse<ConnectionResponse> followers = connectionService.getFollowers(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(followers));
    }

    @GetMapping("/users/{userId}/following")
    @Operation(summary = "Get users that a user is following")
    public ResponseEntity<ApiResponse<PagedResponse<ConnectionResponse>>> getFollowing(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get following request for user ID: {}", userId);
        PagedResponse<ConnectionResponse> following = connectionService.getFollowing(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(following));
    }

    @GetMapping("/users/{userId}/connection-stats")
    @Operation(summary = "Get connection statistics for a user")
    public ResponseEntity<ApiResponse<ConnectionStatsResponse>> getConnectionStats(@PathVariable Long userId) {
        log.info("Get connection stats request for user ID: {}", userId);
        ConnectionStatsResponse stats = connectionService.getConnectionStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/connections/pending")
    @Operation(summary = "Get pending connection requests")
    public ResponseEntity<ApiResponse<PagedResponse<ConnectionResponse>>> getPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get pending connection requests");
        PagedResponse<ConnectionResponse> pending = connectionService.getPendingRequests(page, size);
        return ResponseEntity.ok(ApiResponse.success(pending));
    }

    @GetMapping("/connections/pending/sent")
    @Operation(summary = "Get sent pending connection requests")
    public ResponseEntity<ApiResponse<PagedResponse<ConnectionResponse>>> getSentPendingRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<ConnectionResponse> pending = connectionService.getSentPendingRequests(page, size);
        return ResponseEntity.ok(ApiResponse.success(pending));
    }

    @GetMapping("/connections/past")
    @Operation(summary = "Get past (accepted/rejected) connection requests")
    public ResponseEntity<ApiResponse<PagedResponse<ConnectionResponse>>> getPastRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get past connection requests");
        PagedResponse<ConnectionResponse> past = connectionService.getPastRequests(page, size);
        return ResponseEntity.ok(ApiResponse.success(past));
    }

    @PostMapping("/connections/{connectionId}/accept")
    @Operation(summary = "Accept a connection request")
    public ResponseEntity<ApiResponse<Void>> acceptRequest(@PathVariable Long connectionId) {
        log.info("Accept connection request: {}", connectionId);
        connectionService.acceptRequest(connectionId);
        log.info("Connection request {} accepted", connectionId);
        return ResponseEntity.ok(ApiResponse.success("Connection request accepted", null));
    }

    @DeleteMapping("/connections/{connectionId}/reject")
    @Operation(summary = "Reject a connection request")
    public ResponseEntity<ApiResponse<Void>> rejectRequest(@PathVariable Long connectionId) {
        log.info("Reject connection request: {}", connectionId);
        connectionService.rejectRequest(connectionId);
        log.info("Connection request {} rejected", connectionId);
        return ResponseEntity.ok(ApiResponse.success("Connection request rejected", null));
    }

    @GetMapping("/users/{userId}/is-following")
    @Operation(summary = "Check if current user is following a user")
    public ResponseEntity<ApiResponse<Boolean>> isFollowing(@PathVariable Long userId) {
        log.info("Check if following user ID: {}", userId);
        boolean following = connectionService.isFollowing(userId);
        return ResponseEntity.ok(ApiResponse.success(following));
    }

    @DeleteMapping("/users/{userId}/connection")
    @Operation(summary = "Remove a connection with a user")
    public ResponseEntity<ApiResponse<Void>> removeConnection(@PathVariable Long userId) {
        connectionService.removeConnection(userId);
        return ResponseEntity.ok(ApiResponse.success("Connection removed", null));
    }
}
