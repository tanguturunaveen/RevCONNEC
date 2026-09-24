package org.revature.revconnect.service;

import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private static final AtomicLong MEDIA_ID_SEQUENCE = new AtomicLong(1);
    private static final Map<Long, MediaRecord> MEDIA_STORE = new ConcurrentHashMap<>();

    private final AuthService authService;
    private final UserRepository userRepository;

    @Value("${app.media.upload-dir:uploads}")
    private String uploadRoot;

    public Map<String, String> uploadFile(MultipartFile file) {
        User currentUser = authService.getCurrentUser();
        return saveFile(currentUser, file, "generic");
    }

    public List<Map<String, String>> uploadMultipleFiles(List<MultipartFile> files) {
        User currentUser = authService.getCurrentUser();
        List<Map<String, String>> response = new ArrayList<>();
        for (MultipartFile file : files) {
            response.add(saveFile(currentUser, file, "generic"));
        }
        return response;
    }

    @Transactional
    public Map<String, String> uploadProfilePicture(MultipartFile file) {
        User currentUser = authService.getCurrentUser();
        Map<String, String> saved = saveFile(currentUser, file, "profile");
        currentUser.setProfilePicture(saved.get("url"));
        userRepository.save(currentUser);
        return saved;
    }

    @Transactional
    public Map<String, String> uploadCoverPhoto(MultipartFile file) {
        User currentUser = authService.getCurrentUser();
        Map<String, String> saved = saveFile(currentUser, file, "cover");
        currentUser.setCoverPhoto(saved.get("url"));
        userRepository.save(currentUser);
        return saved;
    }

    public Map<String, String> uploadVideo(MultipartFile file) {
        User currentUser = authService.getCurrentUser();
        return saveFile(currentUser, file, "video");
    }

    public void deleteMedia(Long mediaId) {
        User currentUser = authService.getCurrentUser();
        MediaRecord record = MEDIA_STORE.get(mediaId);
        if (record == null) {
            throw new ResourceNotFoundException("Media", "id", mediaId);
        }
        if (!record.ownerId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only delete your own media");
        }
        try {
            Files.deleteIfExists(Paths.get(record.absolutePath()));
        } catch (IOException ex) {
            log.warn("Failed to delete physical file for media {}: {}", mediaId, ex.getMessage());
        }
        MEDIA_STORE.remove(mediaId);
    }

    public Map<String, Object> getMedia(Long mediaId) {
        User currentUser = authService.getCurrentUser();
        MediaRecord record = MEDIA_STORE.get(mediaId);
        if (record == null) {
            throw new ResourceNotFoundException("Media", "id", mediaId);
        }
        if (!record.ownerId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only access your own media");
        }
        return toMap(record);
    }

    public List<Map<String, Object>> getMyMedia(int page, int size) {
        User currentUser = authService.getCurrentUser();
        List<MediaRecord> records = MEDIA_STORE.values().stream()
                .filter(record -> record.ownerId().equals(currentUser.getId()))
                .sorted(Comparator.comparing(MediaRecord::createdAt).reversed())
                .toList();

        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, size);
        int from = safePage * safeSize;
        if (from >= records.size()) {
            return List.of();
        }
        int to = Math.min(records.size(), from + safeSize);
        List<MediaRecord> sub = records.subList(from, to);
        return sub.stream().map(this::toMap).toList();
    }

    public Map<String, String> getThumbnail(Long mediaId) {
        MediaRecord record = MEDIA_STORE.get(mediaId);
        if (record == null) {
            throw new ResourceNotFoundException("Media", "id", mediaId);
        }
        Map<String, String> map = new HashMap<>();
        map.put("url", record.url());
        return map;
    }

    public Map<String, String> processMedia(Long mediaId, Integer width, Integer height, Integer quality) {
        MediaRecord record = MEDIA_STORE.get(mediaId);
        if (record == null) {
            throw new ResourceNotFoundException("Media", "id", mediaId);
        }
        Map<String, String> map = new HashMap<>();
        map.put("mediaId", String.valueOf(mediaId));
        map.put("url", record.url());
        map.put("status", "processed");
        return map;
    }

    private Map<String, String> saveFile(User user, MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new org.revature.revconnect.exception.BadRequestException("File is empty");
        }

        try {
            String originalName = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String extension = getExtension(originalName);
            String storedName = UUID.randomUUID() + extension;
            Path uploadPath = Paths.get(uploadRoot, "media", String.valueOf(user.getId()), category);
            Files.createDirectories(uploadPath);
            Path target = uploadPath.resolve(storedName);

            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            Long mediaId = MEDIA_ID_SEQUENCE.getAndIncrement();
            String url = "/" + uploadPath.resolve(storedName).toString().replace("\\", "/");

            MediaRecord record = new MediaRecord(
                    mediaId,
                    user.getId(),
                    originalName,
                    file.getContentType(),
                    file.getSize(),
                    category,
                    target.toAbsolutePath().toString(),
                    url,
                    LocalDateTime.now());
            MEDIA_STORE.put(mediaId, record);

            Map<String, String> response = new HashMap<>();
            response.put("mediaId", String.valueOf(mediaId));
            response.put("url", url);
            response.put("fileName", originalName);
            return response;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> toMap(MediaRecord record) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", record.id());
        map.put("ownerId", record.ownerId());
        map.put("fileName", record.fileName());
        map.put("contentType", record.contentType());
        map.put("size", record.size());
        map.put("category", record.category());
        map.put("url", record.url());
        map.put("createdAt", record.createdAt());
        return map;
    }

    private String getExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot);
    }

    private record MediaRecord(
            Long id,
            Long ownerId,
            String fileName,
            String contentType,
            long size,
            String category,
            String absolutePath,
            String url,
            LocalDateTime createdAt) {
    }
}
