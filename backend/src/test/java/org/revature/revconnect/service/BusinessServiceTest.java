package org.revature.revconnect.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.dto.request.BusinessProfileRequest;
import org.revature.revconnect.dto.response.AnalyticsResponse;
import org.revature.revconnect.dto.response.BusinessProfileResponse;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.enums.BusinessCategory;
import org.revature.revconnect.enums.ConnectionStatus;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.mapper.BusinessProfileMapper;
import org.revature.revconnect.model.BusinessProfile;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.PostAnalytics;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.BusinessProfileRepository;
import org.revature.revconnect.repository.ConnectionRepository;
import org.revature.revconnect.repository.PostAnalyticsRepository;
import org.revature.revconnect.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BusinessServiceTest {

    @Mock private BusinessProfileRepository businessProfileRepository;
    @Mock private PostAnalyticsRepository postAnalyticsRepository;
    @Mock private PostRepository postRepository;
    @Mock private ConnectionRepository connectionRepository;
    @Mock private AuthService authService;
    @Mock private BusinessProfileMapper businessProfileMapper;

    @InjectMocks
    private BusinessService businessService;

    @Test
    void createBusinessProfile_whenAlreadyExists_throwsBadRequest() {
        User me = user(1L, "owner");
        when(authService.getCurrentUser()).thenReturn(me);
        when(businessProfileRepository.existsByUserId(1L)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> businessService.createBusinessProfile(request()));
    }

    @Test
    void createBusinessProfile_success_savesAndReturnsMappedResponse() {
        User me = user(1L, "owner");
        BusinessProfile saved = profile(10L, me, "Rev Biz");
        BusinessProfileResponse response = BusinessProfileResponse.builder().id(10L).businessName("Rev Biz").build();

        when(authService.getCurrentUser()).thenReturn(me);
        when(businessProfileRepository.existsByUserId(1L)).thenReturn(false);
        when(businessProfileRepository.save(any(BusinessProfile.class))).thenReturn(saved);
        when(businessProfileMapper.toResponse(saved)).thenReturn(response);

        BusinessProfileResponse result = businessService.createBusinessProfile(request());

        assertEquals(10L, result.getId());
        verify(businessProfileRepository).save(any(BusinessProfile.class));
    }

    @Test
    void getBusinessProfile_notFound_throwsResourceNotFound() {
        when(businessProfileRepository.findByUserId(77L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> businessService.getBusinessProfile(77L));
    }

    @Test
    void updateBusinessProfile_success_updatesAllEditableFields() {
        User me = user(1L, "owner");
        BusinessProfile existing = profile(10L, me, "Old Name");
        BusinessProfileResponse response = BusinessProfileResponse.builder().id(10L).businessName("Rev Biz").build();
        BusinessProfileRequest req = request();

        when(authService.getCurrentUser()).thenReturn(me);
        when(businessProfileRepository.findByUserId(1L)).thenReturn(Optional.of(existing));
        when(businessProfileRepository.save(existing)).thenReturn(existing);
        when(businessProfileMapper.toResponse(existing)).thenReturn(response);

        BusinessProfileResponse result = businessService.updateBusinessProfile(req);

        assertEquals("Rev Biz", existing.getBusinessName());
        assertEquals(BusinessCategory.TECHNOLOGY, existing.getCategory());
        assertEquals("https://rev.example", existing.getWebsiteUrl());
        assertEquals(10L, result.getId());
    }

    @Test
    void deleteBusinessProfile_success_deletesProfile() {
        User me = user(1L, "owner");
        BusinessProfile existing = profile(10L, me, "Rev Biz");
        when(authService.getCurrentUser()).thenReturn(me);
        when(businessProfileRepository.findByUserId(1L)).thenReturn(Optional.of(existing));

        businessService.deleteBusinessProfile();

        verify(businessProfileRepository).delete(existing);
    }

    @Test
    void getBusinessesByCategory_returnsPagedResponse() {
        User me = user(1L, "owner");
        BusinessProfile p = profile(10L, me, "Rev Biz");
        BusinessProfileResponse dto = BusinessProfileResponse.builder().id(10L).businessName("Rev Biz").build();
        Page<BusinessProfile> page = new PageImpl<>(List.of(p), PageRequest.of(0, 5), 1);
        when(businessProfileRepository.findByCategory(BusinessCategory.TECHNOLOGY, PageRequest.of(0, 5))).thenReturn(page);
        when(businessProfileMapper.toResponse(p)).thenReturn(dto);

        PagedResponse<BusinessProfileResponse> result = businessService.getBusinessesByCategory(BusinessCategory.TECHNOLOGY, 0, 5);

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getTotalElements());
    }

    @Test
    void searchBusinesses_returnsPagedResponse() {
        User me = user(1L, "owner");
        BusinessProfile p = profile(10L, me, "Rev Biz");
        BusinessProfileResponse dto = BusinessProfileResponse.builder().id(10L).businessName("Rev Biz").build();
        Page<BusinessProfile> page = new PageImpl<>(List.of(p), PageRequest.of(1, 2), 3);
        when(businessProfileRepository.findByBusinessNameContainingIgnoreCase("rev", PageRequest.of(1, 2))).thenReturn(page);
        when(businessProfileMapper.toResponse(p)).thenReturn(dto);

        PagedResponse<BusinessProfileResponse> result = businessService.searchBusinesses("rev", 1, 2);

        assertEquals(1, result.getContent().size());
        assertEquals(3L, result.getTotalElements());
        assertEquals(1, result.getPageNumber());
    }

    @Test
    void getAnalytics_calculatesTotalsAndEngagementRate() {
        User me = user(1L, "owner");
        PostAnalytics day1 = PostAnalytics.builder()
                .date(LocalDate.now().minusDays(1))
                .views(100).likes(20).comments(5).shares(2).impressions(200)
                .build();
        PostAnalytics day2 = PostAnalytics.builder()
                .date(LocalDate.now())
                .views(90).likes(10).comments(3).shares(1).impressions(100)
                .build();

        when(authService.getCurrentUser()).thenReturn(me);
        when(postAnalyticsRepository.findByUserIdAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(day1, day2));
        when(postAnalyticsRepository.getTotalViewsByUser(1L)).thenReturn(190L);
        when(postAnalyticsRepository.getTotalImpressionsByUser(1L)).thenReturn(300L);
        when(connectionRepository.countByFollowingIdAndStatus(1L, ConnectionStatus.ACCEPTED)).thenReturn(12L);
        when(postRepository.findByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 1)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 7));

        AnalyticsResponse result = businessService.getAnalytics(30);

        assertEquals(190L, result.getTotalViews());
        assertEquals(30L, result.getTotalLikes());
        assertEquals(8L, result.getTotalComments());
        assertEquals(3L, result.getTotalShares());
        assertEquals(300L, result.getTotalImpressions());
        assertEquals(12L, result.getTotalFollowers());
        assertEquals(7L, result.getTotalPosts());
        assertEquals(13.67, result.getEngagementRate());
        assertEquals(2, result.getDailyData().size());
    }

    @Test
    void getAnalytics_whenNullTotals_returnsZeros() {
        User me = user(1L, "owner");
        when(authService.getCurrentUser()).thenReturn(me);
        when(postAnalyticsRepository.findByUserIdAndDateRange(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(postAnalyticsRepository.getTotalViewsByUser(1L)).thenReturn(null);
        when(postAnalyticsRepository.getTotalImpressionsByUser(1L)).thenReturn(null);
        when(connectionRepository.countByFollowingIdAndStatus(1L, ConnectionStatus.ACCEPTED)).thenReturn(0L);
        when(postRepository.findByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 1)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 1), 0));

        AnalyticsResponse result = businessService.getAnalytics(7);

        assertEquals(0L, result.getTotalViews());
        assertEquals(0L, result.getTotalImpressions());
        assertEquals(0.0, result.getEngagementRate());
    }

    @Test
    void recordPostView_existingAnalytics_incrementsViews() {
        PostAnalytics analytics = PostAnalytics.builder()
                .date(LocalDate.now())
                .views(2).likes(0).comments(0).shares(0).impressions(0)
                .build();
        when(postAnalyticsRepository.findByPostIdAndDate(5L, LocalDate.now())).thenReturn(Optional.of(analytics));
        when(postAnalyticsRepository.save(analytics)).thenReturn(analytics);

        businessService.recordPostView(5L);

        assertEquals(3, analytics.getViews());
        verify(postAnalyticsRepository).save(analytics);
    }

    @Test
    void recordPostImpression_whenAnalyticsMissingAndPostExists_createsAndSaves() {
        User me = user(1L, "owner");
        Post post = Post.builder().id(5L).content("c").user(me).build();
        when(postAnalyticsRepository.findByPostIdAndDate(5L, LocalDate.now())).thenReturn(Optional.empty());
        when(postRepository.findById(5L)).thenReturn(Optional.of(post));
        when(postAnalyticsRepository.save(any(PostAnalytics.class))).thenAnswer(i -> i.getArgument(0));

        businessService.recordPostImpression(5L);

        ArgumentCaptor<PostAnalytics> captor = ArgumentCaptor.forClass(PostAnalytics.class);
        verify(postAnalyticsRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getImpressions());
        assertEquals(LocalDate.now(), captor.getValue().getDate());
    }

    @Test
    void recordPostView_whenPostNotFound_doesNothing() {
        when(postAnalyticsRepository.findByPostIdAndDate(5L, LocalDate.now())).thenReturn(Optional.empty());
        when(postRepository.findById(5L)).thenReturn(Optional.empty());

        businessService.recordPostView(5L);

        verify(postAnalyticsRepository, never()).save(any(PostAnalytics.class));
    }

    @Test
    void showcase_addUpdateRemove_flowWorksAndPersistsDescription() {
        User me = user(1L, "owner");
        BusinessProfile profile = profile(10L, me, "Rev Biz");
        profile.setDescription("bio text");
        when(authService.getCurrentUser()).thenReturn(me);
        when(businessProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(businessProfileRepository.save(profile)).thenReturn(profile);

        List<Map<String, Object>> added = businessService.addShowcaseItem(Map.of(
                "name", " Product 1 ", "type", " Service ", "price", " 499 ", "link", " https://rev.example/p1 "));
        assertEquals(1, added.size());
        assertEquals("Product 1", added.get(0).get("name"));

        List<Map<String, Object>> updated = businessService.updateShowcaseItem(0, Map.of(
                "name", "Product X", "type", "Service", "price", "999", "link", "https://rev.example/x"));
        assertEquals("Product X", updated.get(0).get("name"));

        List<Map<String, Object>> remaining = businessService.removeShowcaseItem(0);
        assertEquals(0, remaining.size());
        assertNotNull(profile.getDescription());
    }

    @Test
    void showcase_updateInvalidIndex_throwsBadRequest() {
        User me = user(1L, "owner");
        BusinessProfile profile = profile(10L, me, "Rev Biz");
        profile.setDescription("bio");
        when(authService.getCurrentUser()).thenReturn(me);
        when(businessProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        assertThrows(BadRequestException.class,
                () -> businessService.updateShowcaseItem(2, Map.of("name", "x")));
    }

    private User user(Long id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .name(username)
                .email(username + "@test.com")
                .password("pwd")
                .privacy(Privacy.PUBLIC)
                .build();
    }

    private BusinessProfileRequest request() {
        return BusinessProfileRequest.builder()
                .businessName("Rev Biz")
                .category(BusinessCategory.TECHNOLOGY)
                .description("We build software")
                .websiteUrl("https://rev.example")
                .contactEmail("hello@rev.example")
                .contactPhone("9999999999")
                .address("Hyderabad")
                .logoUrl("https://rev.example/logo.png")
                .coverImageUrl("https://rev.example/cover.png")
                .build();
    }

    private BusinessProfile profile(Long id, User user, String name) {
        return BusinessProfile.builder()
                .id(id)
                .user(user)
                .businessName(name)
                .category(BusinessCategory.TECHNOLOGY)
                .description("desc")
                .websiteUrl("https://old.example")
                .contactEmail("old@example.com")
                .build();
    }
}
