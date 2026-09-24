package org.revature.revconnect.controller;

import org.revature.revconnect.dto.request.ProfileUpdateRequest;
import org.revature.revconnect.dto.response.ApiResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.UserResponse;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Users", description = "User profile management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get my profile", description = "Get the current authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        log.info("Request to get current user profile");
        UserResponse response = userService.getMyProfile();
        log.debug("Profile retrieved for user: {}", response.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "View another user's profile by their ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long userId) {
        log.info("Request to get user profile by ID: {}", userId);
        UserResponse response = userService.getUserById(userId);
        log.debug("Profile retrieved for user ID: {}", userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get user by username", description = "View another user's profile by their username")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByUsername(@PathVariable String username) {
        log.info("Request to get user profile by username: {}", username);
        UserResponse response = userService.getUserByUsername(username);
        log.debug("Profile retrieved for username: {}", username);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    @Operation(summary = "Update my profile", description = "Update the current user's profile information")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request) {
        log.info("Request to update user profile");
        UserResponse response = userService.updateProfile(request);
        log.info("Profile updated successfully for user: {}", response.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Search for users by name or username")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("User search request - query: {}, page: {}, size: {}", query, page, size);
        PagedResponse<UserResponse> response = userService.searchUsers(query, page, size);
        log.debug("Search returned {} results", response.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/me/privacy")
    @Operation(summary = "Update privacy setting", description = "Set profile privacy to PUBLIC or PRIVATE")
    public ResponseEntity<ApiResponse<UserResponse>> updatePrivacy(@RequestParam Privacy privacy) {
        log.info("Request to update privacy to: {}", privacy);
        UserResponse response = userService.updatePrivacy(privacy);
        log.info("Privacy updated to {} for user: {}", privacy, response.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Privacy updated to " + privacy, response));
    }

    @GetMapping("/suggested")
    @Operation(summary = "Get suggested users", description = "Get list of suggested users to follow")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getSuggestedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Request to get suggested users - page: {}, size: {}", page, size);
        PagedResponse<UserResponse> response = userService.getSuggestedUsers(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{userId}/block")
    @Operation(summary = "Block a user", description = "Block a user from viewing your profile")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable Long userId) {
        log.info("Request to block user ID: {}", userId);
        userService.blockUser(userId);
        log.info("User {} blocked successfully", userId);
        return ResponseEntity.ok(ApiResponse.success("User blocked successfully", null));
    }

    @DeleteMapping("/{userId}/block")
    @Operation(summary = "Unblock a user", description = "Unblock a previously blocked user")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable Long userId) {
        log.info("Request to unblock user ID: {}", userId);
        userService.unblockUser(userId);
        log.info("User {} unblocked successfully", userId);
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully", null));
    }

    @GetMapping("/blocked")
    @Operation(summary = "Get blocked users", description = "Get list of users you have blocked")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getBlockedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Request to get blocked users - page: {}, size: {}", page, size);
        PagedResponse<UserResponse> response = userService.getBlockedUsers(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{userId}/report")
    @Operation(summary = "Report a user", description = "Report a user for inappropriate behavior")
    public ResponseEntity<ApiResponse<Void>> reportUser(
            @PathVariable Long userId,
            @RequestParam String reason) {
        log.info("Request to report user ID: {} for reason: {}", userId, reason);
        userService.reportUser(userId, reason);
        log.info("User {} reported successfully", userId);
        return ResponseEntity.ok(ApiResponse.success("User reported successfully", null));
    }

    @GetMapping("/mutual/{userId}")
    @Operation(summary = "Get mutual connections", description = "Get mutual connections with a user")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getMutualConnections(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Request to get mutual connections with user ID: {}", userId);
        PagedResponse<UserResponse> response = userService.getMutualConnections(userId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}