package org.revature.revconnect.controller;

import org.revature.revconnect.dto.response.ApiResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.model.Hashtag;
import org.revature.revconnect.service.HashtagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hashtags")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Hashtags", description = "Hashtag Management APIs")
public class HashtagController {

    private final HashtagService hashtagService;

    @GetMapping("/trending")
    @Operation(summary = "Get trending hashtags")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTrendingHashtags(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Getting top {} trending hashtags", limit);
        List<Map<String, Object>> tags = hashtagService.getTrending(limit).stream()
                .map(this::toHashtagMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    @GetMapping("/{hashtag}")
    @Operation(summary = "Get hashtag details")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHashtag(@PathVariable String hashtag) {
        log.info("Getting hashtag details: {}", hashtag);
        Hashtag tag = hashtagService.getHashtag(hashtag);
        return ResponseEntity.ok(ApiResponse.success(toHashtagMap(tag)));
    }

    @GetMapping("/{hashtag}/posts")
    @Operation(summary = "Get posts by hashtag")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> getPostsByHashtag(
            @PathVariable String hashtag,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Getting posts for hashtag: {}", hashtag);
        PagedResponse<PostResponse> posts = hashtagService.getPostsByHashtag(hashtag, page, size);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @PostMapping("/{hashtag}/follow")
    @Operation(summary = "Follow a hashtag")
    public ResponseEntity<ApiResponse<Void>> followHashtag(@PathVariable String hashtag) {
        log.info("Following hashtag: {}", hashtag);
        hashtagService.followHashtag(hashtag);
        return ResponseEntity.ok(ApiResponse.success("Hashtag followed", null));
    }

    @DeleteMapping("/{hashtag}/follow")
    @Operation(summary = "Unfollow a hashtag")
    public ResponseEntity<ApiResponse<Void>> unfollowHashtag(@PathVariable String hashtag) {
        log.info("Unfollowing hashtag: {}", hashtag);
        hashtagService.unfollowHashtag(hashtag);
        return ResponseEntity.ok(ApiResponse.success("Hashtag unfollowed", null));
    }

    @GetMapping("/following")
    @Operation(summary = "Get followed hashtags")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFollowedHashtags() {
        log.info("Getting followed hashtags");
        return ResponseEntity.ok(ApiResponse.success(hashtagService.getFollowedHashtagsView()));
    }

    @GetMapping("/suggested")
    @Operation(summary = "Get suggested hashtags")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSuggestedHashtags() {
        log.info("Getting suggested hashtags");
        List<Map<String, Object>> tags = hashtagService.getSuggestedHashtags(10).stream()
                .map(this::toHashtagMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    @GetMapping("/search")
    @Operation(summary = "Search hashtags")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchHashtags(
            @RequestParam String query) {
        log.info("Searching hashtags: {}", query);
        List<Map<String, Object>> tags = hashtagService.search(query).stream()
                .map(this::toHashtagMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    @GetMapping("/autocomplete")
    @Operation(summary = "Autocomplete hashtags")
    public ResponseEntity<ApiResponse<List<String>>> autocompleteHashtags(
            @RequestParam String prefix) {
        log.info("Autocomplete hashtags: {}", prefix);
        List<String> tags = hashtagService.search(prefix).stream()
                .map(Hashtag::getName)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    @GetMapping("/{hashtag}/related")
    @Operation(summary = "Get related hashtags")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRelatedHashtags(@PathVariable String hashtag) {
        log.info("Getting related hashtags for: {}", hashtag);
        List<Map<String, Object>> tags = hashtagService.search(hashtag).stream()
                .map(this::toHashtagMap)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(tags));
    }

    private Map<String, Object> toHashtagMap(Hashtag hashtag) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("id", hashtag.getId());
        map.put("tag", hashtag.getName());
        map.put("usageCount", hashtag.getUsageCount());
        map.put("lastUsed", hashtag.getLastUsed());
        return map;
    }
}
