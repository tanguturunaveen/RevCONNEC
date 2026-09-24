package org.revature.revconnect.service;

import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.dto.response.UserResponse;
import org.revature.revconnect.enums.PostType;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.mapper.PostMapper;
import org.revature.revconnect.mapper.UserMapper;
import org.revature.revconnect.model.Hashtag;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final HashtagService hashtagService;
    private final AuthService authService;

    private static final int MAX_RECENT = 15;
    private static final Map<Long, Deque<String>> RECENT_SEARCHES = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public Map<String, Object> searchAll(String query, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        log.info("Global search for: {}", query);
        saveRecentQuery(query);

        Page<User> usersPage = userRepository.searchPublicUsers(query, PageRequest.of(0, safeLimit));
        Page<Post> postsPage = postRepository.searchPosts(
                normalize(query), null, null, null, null, null, PageRequest.of(0, safeLimit));
        List<Hashtag> hashtags = hashtagService.search(query);

        List<UserResponse> users = usersPage.getContent().stream()
                .map(userMapper::toPublicResponse)
                .toList();
        List<PostResponse> posts = postsPage.getContent().stream()
                .map(postMapper::toResponse)
                .toList();
        List<String> tags = hashtags.stream()
                .map(Hashtag::getName)
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("users", users);
        result.put("posts", posts);
        result.put("hashtags", tags);
        return result;
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> searchUsers(String query, int page, int size) {
        saveRecentQuery(query);
        Page<User> users = userRepository.searchPublicUsers(query, PageRequest.of(page, size));
        return PagedResponse.fromEntityPage(users, userMapper::toPublicResponse);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> searchPosts(String query, int page, int size) {
        saveRecentQuery(query);
        Page<Post> posts = postRepository.searchPosts(
                normalize(query), null, null, null, null, null, PageRequest.of(page, size));
        return PagedResponse.fromEntityPage(posts, postMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PagedResponse<PostResponse> advancedPostSearch(String query, String author, String dateFrom, String dateTo,
                                                          String postType, Integer minLikes, int page, int size) {
        saveRecentQuery(query);
        PostType parsedPostType = parsePostType(postType);
        LocalDateTime from = parseDateStart(dateFrom);
        LocalDateTime to = parseDateEnd(dateTo);

        Page<Post> posts = postRepository.searchPosts(
                normalize(query),
                normalize(author),
                parsedPostType,
                minLikes,
                from,
                to,
                PageRequest.of(page, size));

        return PagedResponse.fromEntityPage(posts, postMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> advancedUserSearch(String query, String location, String userType,
                                                          Boolean verified, int page, int size) {
        saveRecentQuery(query);
        UserType parsedUserType = parseUserType(userType);
        Page<User> users = userRepository.advancedSearchPublicUsers(
                normalize(query),
                normalize(location),
                parsedUserType,
                verified,
                PageRequest.of(page, size));
        return PagedResponse.fromEntityPage(users, userMapper::toPublicResponse);
    }

    public List<String> getSearchSuggestions(String query) {
        saveRecentQuery(query);
        return hashtagService.search(query).stream()
                .map(Hashtag::getName)
                .limit(10)
                .toList();
    }

    public List<String> getTrendingSearches() {
        return hashtagService.getTrending(10).stream()
                .map(Hashtag::getName)
                .toList();
    }

    public List<String> getRecentSearches() {
        Long userId = authService.getCurrentUser().getId();
        Deque<String> deque = RECENT_SEARCHES.getOrDefault(userId, new LinkedList<>());
        return new ArrayList<>(deque);
    }

    public void clearRecentSearches() {
        Long userId = authService.getCurrentUser().getId();
        RECENT_SEARCHES.remove(userId);
    }

    public void removeRecentSearch(String query) {
        if (query == null || query.isBlank()) {
            return;
        }
        Long userId = authService.getCurrentUser().getId();
        Deque<String> deque = RECENT_SEARCHES.get(userId);
        if (deque == null) {
            return;
        }
        String normalized = query.trim();
        LinkedHashSet<String> unique = new LinkedHashSet<>(deque);
        unique.remove(normalized);
        RECENT_SEARCHES.put(userId, new LinkedList<>(unique));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PostType parsePostType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PostType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid postType: " + value);
        }
    }

    private UserType parseUserType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UserType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid userType: " + value);
        }
    }

    private LocalDateTime parseDateStart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim()).atStartOfDay();
        } catch (Exception ex) {
            throw new BadRequestException("Invalid dateFrom: " + value);
        }
    }

    private LocalDateTime parseDateEnd(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim()).plusDays(1).atStartOfDay().minusNanos(1);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid dateTo: " + value);
        }
    }

    private void saveRecentQuery(String query) {
        if (query == null || query.isBlank()) {
            return;
        }
        Long userId = authService.getCurrentUser().getId();
        String normalized = query.trim();
        Deque<String> deque = RECENT_SEARCHES.computeIfAbsent(userId, ignored -> new LinkedList<>());
        deque.remove(normalized);
        deque.addFirst(normalized);
        while (deque.size() > MAX_RECENT) {
            deque.removeLast();
        }
    }
}
