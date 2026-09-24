package org.revature.revconnect.controller;

import org.revature.revconnect.dto.response.ApiResponse;
import org.revature.revconnect.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "User Analytics APIs")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    @Operation(summary = "Get analytics overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview() {
        log.info("Getting analytics overview");
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getOverview()));
    }

    @GetMapping("/profile-views")
    @Operation(summary = "Get profile view analytics")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getProfileViews(
            @RequestParam(defaultValue = "7") int days) {
        log.info("Getting profile views for {} days", days);
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getProfileViews(days)));
    }

    @GetMapping("/post-performance")
    @Operation(summary = "Get post performance metrics")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPostPerformance(
            @RequestParam(defaultValue = "7") int days) {
        log.info("Getting post performance for {} days", days);
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getPostPerformance(days)));
    }

    @GetMapping("/posts/{postId}/analytics")
    @Operation(summary = "Get specific post analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPostAnalytics(@PathVariable Long postId) {
        log.info("Getting analytics for post: {}", postId);
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getPostAnalytics(postId)));
    }

    @GetMapping("/followers/growth")
    @Operation(summary = "Get follower growth analytics")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFollowerGrowth(
            @RequestParam(defaultValue = "30") int days) {
        log.info("Getting follower growth for {} days", days);
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getFollowerGrowth(days)));
    }

    @GetMapping("/engagement")
    @Operation(summary = "Get engagement analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEngagement(
            @RequestParam(defaultValue = "7") int days) {
        log.info("Getting engagement for {} days", days);
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getEngagement(days)));
    }

    @GetMapping("/audience")
    @Operation(summary = "Get audience demographics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAudienceDemographics() {
        log.info("Getting audience demographics");
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getAudienceDemographics()));
    }

    @GetMapping("/reach")
    @Operation(summary = "Get reach analytics")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getReach(
            @RequestParam(defaultValue = "7") int days) {
        log.info("Getting reach for {} days", days);
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getReach(days)));
    }

    @GetMapping("/impressions")
    @Operation(summary = "Get impression analytics")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getImpressions(
            @RequestParam(defaultValue = "7") int days) {
        log.info("Getting impressions for {} days", days);
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getImpressions(days)));
    }

    @GetMapping("/best-time")
    @Operation(summary = "Get best times to post")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBestTimeToPost() {
        log.info("Getting best time to post");
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getBestTimeToPost()));
    }

    @GetMapping("/top-posts")
    @Operation(summary = "Get top performing posts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopPosts(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Getting top {} posts", limit);
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getTopPosts(limit)));
    }

    @GetMapping("/hashtag-performance")
    @Operation(summary = "Get hashtag performance")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHashtagPerformance() {
        log.info("Getting hashtag performance");
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getHashtagPerformance()));
    }

    @GetMapping("/content-type")
    @Operation(summary = "Get content type performance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getContentTypePerformance() {
        log.info("Getting content type performance");
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getContentTypePerformance()));
    }

    @GetMapping("/export")
    @Operation(summary = "Export analytics data")
    public ResponseEntity<ApiResponse<Map<String, String>>> exportAnalytics(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "csv") String format) {
        log.info("Exporting analytics for {} days in {} format", days, format);
        return ResponseEntity.ok(ApiResponse.success(analyticsService.exportAnalytics(days, format)));
    }
}
