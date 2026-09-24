package org.revature.revconnect.service;

import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.UserResponse;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.mapper.UserMapper;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.PostRepository;
import org.revature.revconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(int page, int size) {
        Page<User> users = userRepository.findAll(PageRequest.of(page, size));
        return PagedResponse.fromEntityPage(users, userMapper::toResponse);
    }

    @Transactional
    public void verifyUser(Long userId) {
        User user = findUser(userId);
        user.setIsVerified(true);
        userRepository.save(user);
    }

    @Transactional
    public void unverifyUser(Long userId) {
        User user = findUser(userId);
        user.setIsVerified(false);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = findUser(userId);
        userRepository.delete(user);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getReports(int page, int size) {
        return List.of();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getReport(Long reportId) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", reportId);
        map.put("status", "NOT_AVAILABLE");
        map.put("message", "Report module is not implemented yet");
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getFlaggedPosts() {
        return List.of();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPlatformStats() {
        Map<String, Object> map = new HashMap<>();
        map.put("totalUsers", userRepository.count());
        map.put("totalPosts", postRepository.count());
        map.put("verifiedUsers", userRepository.findAll().stream().filter(u -> Boolean.TRUE.equals(u.getIsVerified())).count());
        map.put("activeUsers", userRepository.count());
        return map;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getUserStats() {
        Map<String, Object> map = new HashMap<>();
        map.put("total", userRepository.count());
        map.put("personal", userRepository.countByUserType(UserType.PERSONAL));
        map.put("creator", userRepository.countByUserType(UserType.CREATOR));
        map.put("business", userRepository.countByUserType(UserType.BUSINESS));
        map.put("verified", userRepository.findAll().stream().filter(u -> Boolean.TRUE.equals(u.getIsVerified())).count());
        return map;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPostStats() {
        List<Post> posts = postRepository.findAll();
        long totalLikes = posts.stream().mapToLong(post -> post.getLikeCount() == null ? 0 : post.getLikeCount()).sum();
        long totalComments = posts.stream().mapToLong(post -> post.getCommentCount() == null ? 0 : post.getCommentCount()).sum();
        long totalShares = posts.stream().mapToLong(post -> post.getShareCount() == null ? 0 : post.getShareCount()).sum();

        Map<String, Object> map = new HashMap<>();
        map.put("totalPosts", posts.size());
        map.put("totalLikes", totalLikes);
        map.put("totalComments", totalComments);
        map.put("totalShares", totalShares);
        return map;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getEngagementStats() {
        Map<String, Object> postStats = getPostStats();
        long likes = ((Number) postStats.get("totalLikes")).longValue();
        long comments = ((Number) postStats.get("totalComments")).longValue();
        long shares = ((Number) postStats.get("totalShares")).longValue();
        long posts = ((Number) postStats.get("totalPosts")).longValue();
        double avg = posts > 0 ? (likes + comments + shares) / (double) posts : 0.0;

        Map<String, Object> map = new HashMap<>();
        map.put("totalEngagement", likes + comments + shares);
        map.put("avgEngagementPerPost", Math.round(avg * 100.0) / 100.0);
        map.put("posts", posts);
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAuditLogs(int page, int size) {
        return new ArrayList<>();
    }

    @Transactional
    public Map<String, Object> bulkVerifyUnverifiedUsers() {
        List<User> unverified = userRepository.findAllUnverifiedUsers();
        for (User u : unverified) {
            u.setIsVerified(true);
            userRepository.save(u);
        }
        log.info("Bulk verified {} unverified users", unverified.size());
        Map<String, Object> result = new HashMap<>();
        result.put("verifiedCount", unverified.size());
        return result;
    }

    @Transactional
    public Map<String, Object> deleteUnverifiedUsers() {
        List<User> unverified = userRepository.findAllUnverifiedUsers();
        int count = unverified.size();
        for (User u : unverified) {
            userRepository.delete(u);
        }
        log.info("Deleted {} unverified users", count);
        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", count);
        return result;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
