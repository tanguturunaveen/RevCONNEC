package org.revature.revconnect.service;

import org.revature.revconnect.dto.request.BusinessProfileRequest;
import org.revature.revconnect.dto.response.AnalyticsResponse;
import org.revature.revconnect.dto.response.BusinessProfileResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.enums.BusinessCategory;
import org.revature.revconnect.enums.ConnectionStatus;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.mapper.BusinessProfileMapper;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.model.BusinessProfile;
import org.revature.revconnect.model.PostAnalytics;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.BusinessProfileRepository;
import org.revature.revconnect.repository.ConnectionRepository;
import org.revature.revconnect.repository.PostAnalyticsRepository;
import org.revature.revconnect.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessService {

    private final BusinessProfileRepository businessProfileRepository;
    private final PostAnalyticsRepository postAnalyticsRepository;
    private final PostRepository postRepository;
    private final ConnectionRepository connectionRepository;
    private final AuthService authService;
    private final BusinessProfileMapper businessProfileMapper;
    private static final String SHOWCASE_MARKER = "\n[[SHOWCASE]]\n";

    @Transactional
    public BusinessProfileResponse createBusinessProfile(BusinessProfileRequest request) {
        User currentUser = authService.getCurrentUser();
        log.info("Creating business profile for user: {}", currentUser.getUsername());

        if (businessProfileRepository.existsByUserId(currentUser.getId())) {
            log.warn("User {} already has a business profile", currentUser.getUsername());
            throw new BadRequestException("You already have a business profile");
        }

        BusinessProfile profile = BusinessProfile.builder()
                .user(currentUser)
                .businessName(request.getBusinessName())
                .category(request.getCategory())
                .description(request.getDescription())
                .websiteUrl(request.getWebsiteUrl())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .address(request.getAddress())
                .logoUrl(request.getLogoUrl())
                .coverImageUrl(request.getCoverImageUrl())
                .build();

        BusinessProfile saved = businessProfileRepository.save(profile);
        log.info("Business profile created with ID: {}", saved.getId());
        return businessProfileMapper.toResponse(saved);
    }

    public BusinessProfileResponse getBusinessProfile(Long userId) {
        log.info("Fetching business profile for user ID: {}", userId);

        BusinessProfile profile = businessProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("BusinessProfile", "userId", userId));

        log.info("Found business profile: {}", profile.getBusinessName());
        return businessProfileMapper.toResponse(profile);
    }

    public BusinessProfileResponse getMyBusinessProfile() {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching business profile for current user: {}", currentUser.getUsername());
        return getBusinessProfile(currentUser.getId());
    }

    @Transactional
    public BusinessProfileResponse updateBusinessProfile(BusinessProfileRequest request) {
        User currentUser = authService.getCurrentUser();
        log.info("Updating business profile for user: {}", currentUser.getUsername());

        BusinessProfile profile = businessProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("BusinessProfile", "userId", currentUser.getId()));

        profile.setBusinessName(request.getBusinessName());
        profile.setCategory(request.getCategory());
        // Preserve showcase data when updating description
        String newDescription = request.getDescription();
        String existingDescription = profile.getDescription();
        if (existingDescription != null && existingDescription.contains(SHOWCASE_MARKER)) {
            String showcasePart = existingDescription.substring(existingDescription.indexOf(SHOWCASE_MARKER));
            newDescription = (newDescription == null ? "" : newDescription.trim()) + showcasePart;
        }
        profile.setDescription(newDescription);
        profile.setWebsiteUrl(request.getWebsiteUrl());
        profile.setContactEmail(request.getContactEmail());
        profile.setContactPhone(request.getContactPhone());
        profile.setAddress(request.getAddress());
        profile.setLogoUrl(request.getLogoUrl());
        profile.setCoverImageUrl(request.getCoverImageUrl());

        BusinessProfile saved = businessProfileRepository.save(profile);
        log.info("Business profile updated: {}", saved.getBusinessName());
        return businessProfileMapper.toResponse(saved);
    }

    @Transactional
    public void deleteBusinessProfile() {
        User currentUser = authService.getCurrentUser();
        log.info("Deleting business profile for user: {}", currentUser.getUsername());

        BusinessProfile profile = businessProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("BusinessProfile", "userId", currentUser.getId()));

        businessProfileRepository.delete(profile);
        log.info("Business profile deleted for user: {}", currentUser.getUsername());
    }

    public PagedResponse<BusinessProfileResponse> getBusinessesByCategory(BusinessCategory category, int page,
                                                                          int size) {
        log.info("Fetching businesses in category: {}", category);

        Page<BusinessProfile> profiles = businessProfileRepository.findByCategory(category, PageRequest.of(page, size));

        log.info("Found {} businesses in category {}", profiles.getTotalElements(), category);
        return PagedResponse.fromEntityPage(profiles, businessProfileMapper::toResponse);
    }

    public PagedResponse<BusinessProfileResponse> searchBusinesses(String query, int page, int size) {
        log.info("Searching businesses with query: {}", query);

        Page<BusinessProfile> profiles = businessProfileRepository.findByBusinessNameContainingIgnoreCase(
                query, PageRequest.of(page, size));

        log.info("Found {} businesses matching '{}'", profiles.getTotalElements(), query);
        return PagedResponse.fromEntityPage(profiles, businessProfileMapper::toResponse);
    }

    public AnalyticsResponse getAnalytics(int days) {
        User currentUser = authService.getCurrentUser();
        log.info("Fetching analytics for user: {} for last {} days", currentUser.getUsername(), days);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);


        List<PostAnalytics> dailyData = postAnalyticsRepository.findByUserIdAndDateRange(
                currentUser.getId(), startDate, endDate);


        Long totalViews = postAnalyticsRepository.getTotalViewsByUser(currentUser.getId());
        Long totalImpressions = postAnalyticsRepository.getTotalImpressionsByUser(currentUser.getId());
        long totalFollowers = connectionRepository.countByFollowingIdAndStatus(currentUser.getId(),
                ConnectionStatus.ACCEPTED);
        long totalPosts = postRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), PageRequest.of(0, 1))
                .getTotalElements();


        int totalLikes = dailyData.stream().mapToInt(PostAnalytics::getLikes).sum();
        int totalComments = dailyData.stream().mapToInt(PostAnalytics::getComments).sum();
        int totalShares = dailyData.stream().mapToInt(PostAnalytics::getShares).sum();


        double engagementRate = 0.0;
        if (totalImpressions != null && totalImpressions > 0) {
            engagementRate = ((double) (totalLikes + totalComments + totalShares) / totalImpressions) * 100;
        }

        List<AnalyticsResponse.DailyAnalytics> daily = dailyData.stream()
                .map(pa -> AnalyticsResponse.DailyAnalytics.builder()
                        .date(pa.getDate())
                        .views(pa.getViews())
                        .likes(pa.getLikes())
                        .comments(pa.getComments())
                        .shares(pa.getShares())
                        .impressions(pa.getImpressions())
                        .build())
                .collect(Collectors.toList());

        log.info("Analytics retrieved for user {}: {} views, {} followers",
                currentUser.getUsername(), totalViews, totalFollowers);

        return AnalyticsResponse.builder()
                .totalViews(totalViews != null ? totalViews : 0L)
                .totalLikes((long) totalLikes)
                .totalComments((long) totalComments)
                .totalShares((long) totalShares)
                .totalImpressions(totalImpressions != null ? totalImpressions : 0L)
                .totalFollowers(totalFollowers)
                .totalPosts(totalPosts)
                .engagementRate(Math.round(engagementRate * 100.0) / 100.0)
                .dailyData(daily)
                .build();
    }

    @Transactional
    public void recordPostView(Long postId) {
        log.debug("Recording view for post: {}", postId);
        updateAnalytics(postId, pa -> pa.setViews(pa.getViews() + 1));
    }

    @Transactional
    public void recordPostImpression(Long postId) {
        log.debug("Recording impression for post: {}", postId);
        updateAnalytics(postId, pa -> pa.setImpressions(pa.getImpressions() + 1));
    }

    private void updateAnalytics(Long postId, java.util.function.Consumer<PostAnalytics> updater) {
        LocalDate today = LocalDate.now();
        PostAnalytics analytics = postAnalyticsRepository.findByPostIdAndDate(postId, today)
                .orElseGet(() -> {
                    var post = postRepository.findById(postId).orElse(null);
                    if (post == null)
                        return null;
                    return PostAnalytics.builder()
                            .post(post)
                            .date(today)
                            .build();
                });

        if (analytics != null) {
            updater.accept(analytics);
            postAnalyticsRepository.save(analytics);
        }
    }

    public List<Map<String, Object>> getShowcase() {
        User currentUser = authService.getCurrentUser();
        return businessProfileRepository.findByUserId(currentUser.getId())
                .map(profile -> parseShowcase(profile.getDescription()))
                .orElse(new ArrayList<>());
    }

    @Transactional
    public List<Map<String, Object>> addShowcaseItem(Map<String, String> item) {
        User currentUser = authService.getCurrentUser();
        BusinessProfile profile = businessProfileRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> {
                    BusinessProfile newProfile = BusinessProfile.builder()
                            .user(currentUser)
                            .businessName(currentUser.getName())
                            .category(org.revature.revconnect.enums.BusinessCategory.OTHER)
                            .build();
                    return businessProfileRepository.save(newProfile);
                });
        List<Map<String, Object>> items = parseShowcase(profile.getDescription());
        Map<String, Object> normalized = normalizeShowcaseItem(item);
        items.add(normalized);
        profile.setDescription(mergeDescriptionWithShowcase(profile.getDescription(), items));
        businessProfileRepository.save(profile);
        return items;
    }

    @Transactional
    public List<Map<String, Object>> updateShowcaseItem(int index, Map<String, String> item) {
        User currentUser = authService.getCurrentUser();
        BusinessProfile profile = businessProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("BusinessProfile", "userId", currentUser.getId()));
        List<Map<String, Object>> items = parseShowcase(profile.getDescription());
        if (index < 0 || index >= items.size()) {
            throw new BadRequestException("Invalid showcase index");
        }
        items.set(index, normalizeShowcaseItem(item));
        profile.setDescription(mergeDescriptionWithShowcase(profile.getDescription(), items));
        businessProfileRepository.save(profile);
        return items;
    }

    @Transactional
    public List<Map<String, Object>> removeShowcaseItem(int index) {
        User currentUser = authService.getCurrentUser();
        BusinessProfile profile = businessProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("BusinessProfile", "userId", currentUser.getId()));
        List<Map<String, Object>> items = parseShowcase(profile.getDescription());
        if (index < 0 || index >= items.size()) {
            throw new BadRequestException("Invalid showcase index");
        }
        items.remove(index);
        profile.setDescription(mergeDescriptionWithShowcase(profile.getDescription(), items));
        businessProfileRepository.save(profile);
        return items;
    }

    private List<Map<String, Object>> parseShowcase(String description) {
        if (description == null || !description.contains(SHOWCASE_MARKER)) {
            return new ArrayList<>();
        }
        String showcaseRaw = description.substring(description.indexOf(SHOWCASE_MARKER) + SHOWCASE_MARKER.length());
        if (showcaseRaw.isBlank()) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        String[] lines = showcaseRaw.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) {
                continue;
            }
            String[] parts = trimmed.split("\\|", -1);
            if (parts.length >= 4) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", parts[0]);
                item.put("type", parts[1]);
                item.put("price", parts[2]);
                item.put("link", parts[3]);
                items.add(item);
            }
        }
        return items;
    }

    private Map<String, Object> normalizeShowcaseItem(Map<String, String> item) {
        Map<String, Object> normalized = new HashMap<>();
        normalized.put("name", item.getOrDefault("name", "").trim());
        normalized.put("type", item.getOrDefault("type", "").trim());
        normalized.put("price", item.getOrDefault("price", "").trim());
        normalized.put("link", item.getOrDefault("link", "").trim());
        return normalized;
    }

    private String mergeDescriptionWithShowcase(String originalDescription, List<Map<String, Object>> items) {
        String bio = originalDescription == null ? "" : originalDescription;
        if (bio.contains(SHOWCASE_MARKER)) {
            bio = bio.substring(0, bio.indexOf(SHOWCASE_MARKER));
        }
        StringBuilder sb = new StringBuilder(bio.trim());
        sb.append(SHOWCASE_MARKER);
        for (Map<String, Object> item : items) {
            sb.append(item.getOrDefault("name", "")).append("|")
                    .append(item.getOrDefault("type", "")).append("|")
                    .append(item.getOrDefault("price", "")).append("|")
                    .append(item.getOrDefault("link", "")).append("\n");
        }
        return sb.toString().trim();
    }
}
