package org.revature.revconnect.service;

import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.mapper.PostMapper;
import org.revature.revconnect.model.Hashtag;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.repository.HashtagRepository;
import org.revature.revconnect.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class HashtagService {

    private final HashtagRepository hashtagRepository;
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final AuthService authService;
    private static final Map<Long, Set<String>> FOLLOWED_HASHTAGS = new ConcurrentHashMap<>();

    @Transactional
    public Hashtag createOrIncrement(String name) {
        String normalizedName = normalizeHashtag(name);
        log.info("Processing hashtag: {}", normalizedName);

        Optional<Hashtag> existingHashtag = hashtagRepository.findByName(normalizedName);

        if (existingHashtag.isPresent()) {
            Hashtag hashtag = existingHashtag.get();
            hashtag.incrementUsage();
            hashtagRepository.save(hashtag);
        } else {
            Hashtag newHashtag = Hashtag.builder()
                    .name(normalizedName)
                    .build();
            hashtagRepository.save(newHashtag);
        }
        return null;
    }

    public List<Hashtag> getTrending(int limit) {
        log.info("Fetching top {} trending hashtags", limit);
        return hashtagRepository.findTrending(PageRequest.of(0, limit));
    }

    public Hashtag getHashtag(String name) {
        String normalizedName = normalizeHashtag(name);
        return hashtagRepository.findByName(normalizedName)
                .orElseThrow(() -> new ResourceNotFoundException("Hashtag", "name", normalizedName));
    }

    public PagedResponse<PostResponse> getPostsByHashtag(String name, int page, int size) {
        String normalizedName = normalizeHashtag(name);
        String tag = "#" + normalizedName;
        Page<Post> posts = postRepository.findByContentContainingTag(tag, PageRequest.of(page, size));
        return PagedResponse.fromEntityPage(posts, postMapper::toResponse);
    }

    public List<Hashtag> search(String query) {
        log.info("Searching hashtags with query: {}", query);
        return hashtagRepository.findByNameContainingIgnoreCase(query);
    }

    public List<Hashtag> getSuggestedHashtags(int limit) {
        Long userId = authService.getCurrentUser().getId();
        Set<String> followed = FOLLOWED_HASHTAGS.getOrDefault(userId, Set.of());
        return getTrending(limit * 2).stream()
                .filter(tag -> !followed.contains(tag.getName()))
                .limit(limit)
                .toList();
    }

    public List<Map<String, Object>> getFollowedHashtagsView() {
        Long userId = authService.getCurrentUser().getId();
        Set<String> followed = FOLLOWED_HASHTAGS.getOrDefault(userId, Set.of());
        return followed.stream()
                .map(this::toHashtagSummary)
                .toList();
    }

    @Transactional
    public void followHashtag(String hashtag) {
        String normalized = normalizeHashtag(hashtag);
        createOrIncrement(normalized);
        Long userId = authService.getCurrentUser().getId();
        FOLLOWED_HASHTAGS.computeIfAbsent(userId, ignored -> new LinkedHashSet<>()).add(normalized);
    }

    @Transactional
    public void unfollowHashtag(String hashtag) {
        String normalized = normalizeHashtag(hashtag);
        Long userId = authService.getCurrentUser().getId();
        Set<String> followed = FOLLOWED_HASHTAGS.get(userId);
        if (followed != null) {
            followed.remove(normalized);
        }
    }

    private String normalizeHashtag(String name) {
        if (name == null)
            return "";
        String normalized = name.toLowerCase().trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    @Transactional
    public void processHashtagsFromContent(String content) {
        if (content == null)
            return;

        String[] words = content.split("\\s+");
        for (String word : words) {
            if (word.startsWith("#") && word.length() > 1) {
                createOrIncrement(word);
            }
        }
    }

    private Map<String, Object> toHashtagSummary(String hashtagName) {
        Hashtag hashtag = hashtagRepository.findByName(hashtagName).orElse(null);
        Map<String, Object> map = new HashMap<>();
        map.put("tag", hashtagName);
        map.put("usageCount", hashtag != null ? hashtag.getUsageCount() : 0L);
        map.put("lastUsed", hashtag != null ? hashtag.getLastUsed() : null);
        return map;
    }
}
