
package org.revature.revconnect.controller;

import org.revature.revconnect.dto.request.CommentRequest;
import org.revature.revconnect.dto.request.ShareRequest;
import org.revature.revconnect.dto.response.*;
import org.revature.revconnect.service.InteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Interactions", description = "Like, Comment, Share APIs")
public class InteractionController {

    private final InteractionService interactionService;

    @PostMapping("/posts/{postId}/like")
    @Operation(summary = "Like a post")
    public ResponseEntity<ApiResponse<Void>> likePost(@PathVariable Long postId) {
        log.info("Like post request for post ID: {}", postId);
        interactionService.likePost(postId);
        log.info("Post {} liked successfully", postId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post liked successfully", null));
    }

    @DeleteMapping("/posts/{postId}/like")
    @Operation(summary = "Unlike a post")
    public ResponseEntity<ApiResponse<Void>> unlikePost(@PathVariable Long postId) {
        log.info("Unlike post request for post ID: {}", postId);
        interactionService.unlikePost(postId);
        log.info("Post {} unliked successfully", postId);
        return ResponseEntity.ok(ApiResponse.success("Post unliked successfully", null));
    }

    @GetMapping("/posts/{postId}/likes")
    @Operation(summary = "Get users who liked a post")
    public ResponseEntity<ApiResponse<PagedResponse<LikeResponse>>> getPostLikes(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get likes request for post ID: {}", postId);
        PagedResponse<LikeResponse> likes = interactionService.getPostLikes(postId, page, size);
        return ResponseEntity.ok(ApiResponse.success(likes));
    }

    @GetMapping("/posts/{postId}/liked")
    @Operation(summary = "Check if current user has liked a post")
    public ResponseEntity<ApiResponse<Boolean>> hasUserLikedPost(@PathVariable Long postId) {
        log.info("Check like status for post ID: {}", postId);
        boolean liked = interactionService.hasUserLikedPost(postId);
        return ResponseEntity.ok(ApiResponse.success(liked));
    }

    @PostMapping("/posts/{postId}/comments")
    @Operation(summary = "Add a comment to a post")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CommentRequest request) {
        log.info("[TRACE] Incoming Add Comment request for post ID: {}. Content length: {}", postId,
                request.getContent().length());
        CommentResponse comment = interactionService.addComment(postId, request);
        log.info("[TRACE] Comment successfully added with ID: {}", comment.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added successfully", comment));
    }

    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "Get comments on a post")
    public ResponseEntity<ApiResponse<PagedResponse<CommentResponse>>> getPostComments(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get comments request for post ID: {}", postId);
        PagedResponse<CommentResponse> comments = interactionService.getPostComments(postId, page, size);
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete a comment")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long commentId) {
        log.info("Delete comment request for comment ID: {}", commentId);
        interactionService.deleteComment(commentId);
        log.info("Comment {} deleted successfully", commentId);
        return ResponseEntity.ok(ApiResponse.success("Comment deleted successfully", null));
    }

    @PutMapping("/comments/{commentId}")
    @Operation(summary = "Edit a comment")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request) {
        log.info("Update comment request for comment ID: {}", commentId);
        CommentResponse updatedComment = interactionService.updateComment(commentId, request);
        return ResponseEntity.ok(ApiResponse.success("Comment updated successfully", updatedComment));
    }

    @PostMapping("/comments/{commentId}/like")
    @Operation(summary = "Like a comment")
    public ResponseEntity<ApiResponse<Void>> likeComment(@PathVariable Long commentId) {
        log.info("Like comment request for comment ID: {}", commentId);
        interactionService.likeComment(commentId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment liked successfully", null));
    }

    @DeleteMapping("/comments/{commentId}/like")
    @Operation(summary = "Unlike a comment")
    public ResponseEntity<ApiResponse<Void>> unlikeComment(@PathVariable Long commentId) {
        log.info("Unlike comment request for comment ID: {}", commentId);
        interactionService.unlikeComment(commentId);
        return ResponseEntity.ok(ApiResponse.success("Comment unliked successfully", null));
    }

    @GetMapping("/comments/{commentId}/replies")
    @Operation(summary = "Get replies to a comment")
    public ResponseEntity<ApiResponse<PagedResponse<CommentResponse>>> getCommentReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Get replies request for comment ID: {}", commentId);
        PagedResponse<CommentResponse> replies = interactionService.getCommentReplies(commentId, page, size);
        return ResponseEntity.ok(ApiResponse.success(replies));
    }

    @PostMapping("/posts/{postId}/share")
    @Operation(summary = "Share/Repost a post")
    public ResponseEntity<ApiResponse<ShareResponse>> sharePost(
            @PathVariable Long postId,
            @RequestBody(required = false) ShareRequest request) {
        log.info("Share post request for post ID: {}", postId);
        ShareResponse share = interactionService.sharePost(postId, request);
        log.info("Post {} shared successfully", postId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Post shared successfully", share));
    }

    @PostMapping("/posts/{postId}/share/increment")
    @Operation(summary = "Increment share count for a post without reposting (e.g. DM share)")
    public ResponseEntity<ApiResponse<Void>> incrementShareCount(@PathVariable Long postId) {
        log.info("Increment share count request for post ID: {}", postId);
        interactionService.incrementShareCount(postId);
        return ResponseEntity.ok(ApiResponse.success("Post share count incremented", null));
    }
}
