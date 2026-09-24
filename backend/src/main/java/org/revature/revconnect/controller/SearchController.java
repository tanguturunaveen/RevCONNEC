package org.revature.revconnect.controller;

import org.revature.revconnect.dto.response.ApiResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.dto.response.UserResponse;
import org.revature.revconnect.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Search", description = "Advanced Search APIs")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/all")
    @Operation(summary = "Search across all content types")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchAll(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        log.info("Global search for: {}", query);
        Map<String, Object> results = searchService.searchAll(query, limit);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/users")
    @Operation(summary = "Search users")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Searching users: {}", query);
        PagedResponse<UserResponse> users = searchService.searchUsers(query, page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/posts")
    @Operation(summary = "Search posts")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> searchPosts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Searching posts: {}", query);
        PagedResponse<PostResponse> posts = searchService.searchPosts(query, page, size);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @GetMapping("/posts/advanced")
    @Operation(summary = "Advanced post search with filters")
    public ResponseEntity<ApiResponse<PagedResponse<PostResponse>>> advancedPostSearch(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String postType,
            @RequestParam(required = false) Integer minLikes,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Advanced post search");
        PagedResponse<PostResponse> posts = searchService.advancedPostSearch(
                query, author, dateFrom, dateTo, postType, minLikes, page, size);
        return ResponseEntity.ok(ApiResponse.success(posts));
    }

    @GetMapping("/users/advanced")
    @Operation(summary = "Advanced user search with filters")
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> advancedUserSearch(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Advanced user search");
        PagedResponse<UserResponse> users = searchService.advancedUserSearch(
                query, location, userType, verified, page, size);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent searches")
    public ResponseEntity<ApiResponse<List<String>>> getRecentSearches() {
        log.info("Getting recent searches");
        return ResponseEntity.ok(ApiResponse.success(searchService.getRecentSearches()));
    }

    @DeleteMapping("/recent")
    @Operation(summary = "Clear recent searches")
    public ResponseEntity<ApiResponse<Void>> clearRecentSearches() {
        log.info("Clearing recent searches");
        searchService.clearRecentSearches();
        return ResponseEntity.ok(ApiResponse.success("Recent searches cleared", null));
    }

    @DeleteMapping("/recent/{query}")
    @Operation(summary = "Remove a specific recent search")
    public ResponseEntity<ApiResponse<Void>> removeRecentSearch(@PathVariable String query) {
        log.info("Removing recent search: {}", query);
        searchService.removeRecentSearch(query);
        return ResponseEntity.ok(ApiResponse.success("Search removed", null));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get search suggestions")
    public ResponseEntity<ApiResponse<List<String>>> getSearchSuggestions(
            @RequestParam String query) {
        log.info("Getting suggestions for: {}", query);
        return ResponseEntity.ok(ApiResponse.success(searchService.getSearchSuggestions(query)));
    }

    @GetMapping("/trending")
    @Operation(summary = "Get trending searches")
    public ResponseEntity<ApiResponse<List<String>>> getTrendingSearches() {
        log.info("Getting trending searches");
        return ResponseEntity.ok(ApiResponse.success(searchService.getTrendingSearches()));
    }
}
