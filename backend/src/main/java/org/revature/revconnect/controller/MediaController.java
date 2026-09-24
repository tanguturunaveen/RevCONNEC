package org.revature.revconnect.controller;

import org.revature.revconnect.dto.response.ApiResponse;
import org.revature.revconnect.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Media", description = "Media Upload and Management APIs")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a single file")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadFile(
            @RequestParam("file") MultipartFile file) {
        log.info("Uploading file: {}", file.getOriginalFilename());
        Map<String, String> uploaded = mediaService.uploadFile(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded", uploaded));
    }

    @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload multiple files")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files) {
        log.info("Uploading {} files", files.size());
        List<Map<String, String>> uploaded = mediaService.uploadMultipleFiles(files);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Files uploaded", uploaded));
    }

    @PostMapping(value = "/upload/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload profile picture")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProfilePicture(
            @RequestParam("file") MultipartFile file) {
        log.info("Uploading profile picture");
        Map<String, String> uploaded = mediaService.uploadProfilePicture(file);
        return ResponseEntity.ok(ApiResponse.success("Profile picture updated", uploaded));
    }

    @PostMapping(value = "/upload/cover-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload cover photo")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadCoverPhoto(
            @RequestParam("file") MultipartFile file) {
        log.info("Uploading cover photo");
        Map<String, String> uploaded = mediaService.uploadCoverPhoto(file);
        return ResponseEntity.ok(ApiResponse.success("Cover photo updated", uploaded));
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete a media file")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(@PathVariable Long mediaId) {
        log.info("Deleting media: {}", mediaId);
        mediaService.deleteMedia(mediaId);
        return ResponseEntity.ok(ApiResponse.success("Media deleted", null));
    }

    @GetMapping("/{mediaId}")
    @Operation(summary = "Get media details")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMedia(@PathVariable Long mediaId) {
        log.info("Getting media: {}", mediaId);
        return ResponseEntity.ok(ApiResponse.success(mediaService.getMedia(mediaId)));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my media files")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyMedia(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Getting my media");
        return ResponseEntity.ok(ApiResponse.success(mediaService.getMyMedia(page, size)));
    }

    @PostMapping(value = "/upload/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload video")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadVideo(
            @RequestParam("file") MultipartFile file) {
        log.info("Uploading video");
        Map<String, String> uploaded = mediaService.uploadVideo(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Video uploaded", uploaded));
    }

    @GetMapping("/{mediaId}/thumbnail")
    @Operation(summary = "Get media thumbnail")
    public ResponseEntity<ApiResponse<Map<String, String>>> getThumbnail(@PathVariable Long mediaId) {
        log.info("Getting thumbnail for media: {}", mediaId);
        return ResponseEntity.ok(ApiResponse.success(mediaService.getThumbnail(mediaId)));
    }

    @PostMapping("/{mediaId}/process")
    @Operation(summary = "Process media (resize, compress)")
    public ResponseEntity<ApiResponse<Map<String, String>>> processMedia(
            @PathVariable Long mediaId,
            @RequestParam(required = false) Integer width,
            @RequestParam(required = false) Integer height,
            @RequestParam(required = false) Integer quality) {
        log.info("Processing media: {}", mediaId);
        Map<String, String> processed = mediaService.processMedia(mediaId, width, height, quality);
        return ResponseEntity.ok(ApiResponse.success("Media processed", processed));
    }
}
