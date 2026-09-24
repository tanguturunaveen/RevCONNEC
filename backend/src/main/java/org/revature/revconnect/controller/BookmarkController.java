package org.revature.revconnect.controller;

import org.revature.revconnect.dto.response.ApiResponse;
import org.revature.revconnect.dto.response.BookmarkResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.service.BookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bookmarks", description = "Save/Bookmark Posts APIs")
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping("/posts/{postId}")
    @Operation(summary = "Bookmark a post")
    public ResponseEntity<ApiResponse<Void>> bookmarkPost(@PathVariable Long postId) {
        log.info("Bookmark post request for post ID: {}", postId);
        bookmarkService.bookmarkPost(postId);
        log.info("Post {} bookmarked successfully", postId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post bookmarked successfully", null));
    }

    @DeleteMapping("/posts/{postId}")
    @Operation(summary = "Remove bookmark from a post")
    public ResponseEntity<ApiResponse<Void>> removeBookmark(@PathVariable Long postId) {
        log.info("Remove bookmark request for post ID: {}", postId);
        bookmarkService.removeBookmark(postId);
        log.info("Bookmark removed from post {}", postId);
        return ResponseEntity.ok(ApiResponse.success("Bookmark removed successfully", null));
    }

    @GetMapping
    @Operation(summary = "Get all bookmarked posts")
    public ResponseEntity<ApiResponse<PagedResponse<BookmarkResponse>>> getBookmarks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get bookmarks request - page: {}, size: {}", page, size);
        PagedResponse<BookmarkResponse> bookmarks = bookmarkService.getBookmarks(page, size);
        return ResponseEntity.ok(ApiResponse.success(bookmarks));
    }

    @GetMapping("/posts/{postId}/status")
    @Operation(summary = "Check if a post is bookmarked")
    public ResponseEntity<ApiResponse<Boolean>> isBookmarked(@PathVariable Long postId) {
        log.info("Check bookmark status for post ID: {}", postId);
        boolean bookmarked = bookmarkService.isBookmarked(postId);
        return ResponseEntity.ok(ApiResponse.success(bookmarked));
    }
}
