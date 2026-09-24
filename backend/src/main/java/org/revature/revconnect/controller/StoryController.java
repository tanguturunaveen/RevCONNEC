package org.revature.revconnect.controller;

import org.revature.revconnect.dto.response.ApiResponse;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.model.Story;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.UserRepository;
import org.revature.revconnect.service.AuthService;
import org.revature.revconnect.service.StoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stories", description = "Story Management APIs")
public class StoryController {

    private final StoryService storyService;
    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Create a new story")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createStory(
            @RequestParam String mediaUrl,
            @RequestParam(required = false) String caption) {
        log.info("Creating new story");
        Story story = storyService.createStory(mediaUrl, caption);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Story created", Map.of("storyId", story.getId())));
    }

    @GetMapping
    @Operation(summary = "Get current user's stories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyStories() {
        log.info("Getting my stories");
        User currentUser = authService.getCurrentUser();
        List<Map<String, Object>> stories = storyService.getActiveStories(currentUser).stream()
                .map(this::toStoryMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(stories));
    }

    @GetMapping("/feed")
    @Operation(summary = "Get stories feed from followed users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStoriesFeed() {
        log.info("Getting stories feed");
        List<Map<String, Object>> stories = storyService.getStoriesFeedForCurrentUser().stream()
                .map(this::toStoryMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(stories));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get stories of a specific user")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserStories(@PathVariable Long userId) {
        log.info("Getting stories for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        List<Map<String, Object>> stories = storyService.getActiveStories(user).stream()
                .map(this::toStoryMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(stories));
    }

    @GetMapping("/{storyId}")
    @Operation(summary = "Get a specific story")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStory(@PathVariable Long storyId) {
        log.info("Getting story: {}", storyId);
        Story story = storyService.getStory(storyId);
        return ResponseEntity.ok(ApiResponse.success(toStoryMap(story)));
    }

    @DeleteMapping("/{storyId}")
    @Operation(summary = "Delete a story")
    public ResponseEntity<ApiResponse<Void>> deleteStory(@PathVariable Long storyId) {
        log.info("Deleting story: {}", storyId);
        storyService.deleteStory(storyId);
        return ResponseEntity.ok(ApiResponse.success("Story deleted", null));
    }

    @PostMapping("/{storyId}/view")
    @Operation(summary = "Mark story as viewed")
    public ResponseEntity<ApiResponse<Void>> viewStory(@PathVariable Long storyId) {
        log.info("Viewing story: {}", storyId);
        storyService.incrementViewCount(storyId);
        return ResponseEntity.ok(ApiResponse.success("Story viewed", null));
    }

    @GetMapping("/{storyId}/viewers")
    @Operation(summary = "Get story viewers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getStoryViewers(@PathVariable Long storyId) {
        log.info("Getting viewers for story: {}", storyId);
        return ResponseEntity.ok(ApiResponse.success(storyService.getStoryViewers(storyId)));
    }

    @PostMapping("/{storyId}/react")
    @Operation(summary = "React to a story")
    public ResponseEntity<ApiResponse<Void>> reactToStory(
            @PathVariable Long storyId,
            @RequestParam String reaction) {
        log.info("Reacting to story {} with {}", storyId, reaction);
        storyService.reactToStory(storyId, reaction);
        return ResponseEntity.ok(ApiResponse.success("Reaction added", null));
    }

    @PostMapping("/{storyId}/reply")
    @Operation(summary = "Reply to a story")
    public ResponseEntity<ApiResponse<Void>> replyToStory(
            @PathVariable Long storyId,
            @RequestParam String message) {
        log.info("Replying to story {}", storyId);
        storyService.replyToStory(storyId, message);
        return ResponseEntity.ok(ApiResponse.success("Reply sent", null));
    }

    @GetMapping("/highlights")
    @Operation(summary = "Get story highlights")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHighlights() {
        log.info("Getting highlights");
        User currentUser = authService.getCurrentUser();
        List<Map<String, Object>> stories = storyService.getHighlights(currentUser).stream()
                .map(this::toStoryMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(stories));
    }

    @PostMapping("/highlights")
    @Operation(summary = "Create a highlight from stories")
    public ResponseEntity<ApiResponse<Map<String, Long>>> createHighlight(
            @RequestParam String name,
            @RequestParam List<Long> storyIds) {
        log.info("Creating highlight: {}", name);
        if (storyIds == null || storyIds.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("No stories provided", Map.of("highlightId", 0L)));
        }
        storyIds.forEach(storyService::markAsHighlight);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Highlight created", Map.of("highlightId", storyIds.get(0))));
    }

    @DeleteMapping("/highlights/{highlightId}")
    @Operation(summary = "Delete a highlight")
    public ResponseEntity<ApiResponse<Void>> deleteHighlight(@PathVariable Long highlightId) {
        log.info("Deleting highlight: {}", highlightId);
        storyService.unmarkHighlight(highlightId);
        return ResponseEntity.ok(ApiResponse.success("Highlight deleted", null));
    }

    @GetMapping("/archive")
    @Operation(summary = "Get archived stories")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getArchivedStories() {
        log.info("Getting archived stories");
        User currentUser = authService.getCurrentUser();
        List<Map<String, Object>> stories = storyService.getArchivedStories(currentUser).stream()
                .map(this::toStoryMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(stories));
    }

    private Map<String, Object> toStoryMap(Story story) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", story.getId());
        map.put("userId", story.getUser().getId());
        map.put("mediaUrl", story.getMediaUrl());
        map.put("caption", story.getCaption());
        map.put("createdAt", story.getCreatedAt());
        map.put("expiresAt", story.getExpiresAt());
        map.put("isHighlight", story.isHighlight());
        map.put("viewCount", story.getViewCount());

        Map<String, Object> userMap = new java.util.HashMap<>();
        userMap.put("id", story.getUser().getId());
        userMap.put("username", story.getUser().getUsername());
        userMap.put("name", story.getUser().getName());
        userMap.put("profilePicture", story.getUser().getProfilePicture());
        map.put("user", userMap);

        return map;
    }
}
