package org.revature.revconnect.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.PostResponse;
import org.revature.revconnect.enums.PostType;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.mapper.PostMapper;
import org.revature.revconnect.model.Hashtag;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.HashtagRepository;
import org.revature.revconnect.repository.PostRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HashtagServiceTest {

    @Mock private HashtagRepository hashtagRepository;
    @Mock private PostRepository postRepository;
    @Mock private PostMapper postMapper;
    @Mock private AuthService authService;

    @InjectMocks
    private HashtagService hashtagService;

    @Test
    void createOrIncrement_whenExisting_incrementsAndSaves() {
        Hashtag existing = Hashtag.builder().id(1L).name("java").usageCount(5L).build();
        when(hashtagRepository.findByName("java")).thenReturn(Optional.of(existing));

        hashtagService.createOrIncrement("#Java");

        assertEquals(6L, existing.getUsageCount());
        verify(hashtagRepository).save(existing);
    }

    @Test
    void createOrIncrement_whenMissing_createsNewHashtag() {
        when(hashtagRepository.findByName("spring")).thenReturn(Optional.empty());

        hashtagService.createOrIncrement(" spring ");

        verify(hashtagRepository).save(org.mockito.ArgumentMatchers.any(Hashtag.class));
    }

    @Test
    void getTrending_returnsTopList() {
        List<Hashtag> list = List.of(
                Hashtag.builder().id(1L).name("java").usageCount(10L).build(),
                Hashtag.builder().id(2L).name("spring").usageCount(8L).build()
        );
        when(hashtagRepository.findTrending(PageRequest.of(0, 2))).thenReturn(list);

        List<Hashtag> result = hashtagService.getTrending(2);

        assertEquals(2, result.size());
        assertEquals("java", result.get(0).getName());
    }

    @Test
    void getHashtag_notFound_throwsResourceNotFound() {
        when(hashtagRepository.findByName("missing")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> hashtagService.getHashtag("#missing"));
    }

    @Test
    void getPostsByHashtag_normalizesNameAndMapsResponse() {
        User author = user(1L, "u1");
        Post post = Post.builder().id(10L).content("#java tips").user(author).postType(PostType.TEXT).build();
        Page<Post> page = new PageImpl<>(List.of(post), PageRequest.of(0, 5), 1);
        PostResponse dto = PostResponse.builder().id(10L).content("#java tips").build();

        when(postRepository.findByContentContainingTag("#java", PageRequest.of(0, 5))).thenReturn(page);
        when(postMapper.toResponse(post)).thenReturn(dto);

        PagedResponse<PostResponse> result = hashtagService.getPostsByHashtag(" #Java ", 0, 5);

        assertEquals(1, result.getContent().size());
        assertEquals(10L, result.getContent().get(0).getId());
    }

    @Test
    void search_returnsMatchingTags() {
        List<Hashtag> list = List.of(Hashtag.builder().name("java").build());
        when(hashtagRepository.findByNameContainingIgnoreCase("ja")).thenReturn(list);

        List<Hashtag> result = hashtagService.search("ja");
        assertEquals(1, result.size());
    }

    @Test
    void followAndUnfollowHashtag_updatesFollowedView() {
        User me = user(101L, "me101");
        Hashtag java = Hashtag.builder().id(1L).name("java").usageCount(20L).build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(hashtagRepository.findByName("java")).thenReturn(Optional.of(java));

        hashtagService.followHashtag("#Java");
        List<Map<String, Object>> followed = hashtagService.getFollowedHashtagsView();
        assertEquals(1, followed.size());
        assertEquals("java", followed.get(0).get("tag"));
        assertEquals(21L, followed.get(0).get("usageCount"));

        hashtagService.unfollowHashtag("java");
        List<Map<String, Object>> afterUnfollow = hashtagService.getFollowedHashtagsView();
        assertEquals(0, afterUnfollow.size());
    }

    @Test
    void getSuggestedHashtags_excludesFollowedTags() {
        User me = user(102L, "me102");
        when(authService.getCurrentUser()).thenReturn(me);
        when(hashtagRepository.findByName("java")).thenReturn(Optional.of(Hashtag.builder().name("java").usageCount(9L).build()));

        hashtagService.followHashtag("java");

        List<Hashtag> trending = List.of(
                Hashtag.builder().name("java").usageCount(100L).build(),
                Hashtag.builder().name("spring").usageCount(90L).build(),
                Hashtag.builder().name("boot").usageCount(80L).build()
        );
        when(hashtagRepository.findTrending(PageRequest.of(0, 4))).thenReturn(trending);

        List<Hashtag> suggested = hashtagService.getSuggestedHashtags(2);

        assertEquals(2, suggested.size());
        assertEquals("spring", suggested.get(0).getName());
        assertEquals("boot", suggested.get(1).getName());
    }

    @Test
    void processHashtagsFromContent_extractsOnlyValidHashWords() {
        when(hashtagRepository.findByName("java")).thenReturn(Optional.empty());
        when(hashtagRepository.findByName("spring")).thenReturn(Optional.empty());

        hashtagService.processHashtagsFromContent("Learn #Java and #Spring today");

        verify(hashtagRepository, times(2)).save(org.mockito.ArgumentMatchers.any(Hashtag.class));
    }

    @Test
    void processHashtagsFromContent_nullContent_doesNothing() {
        hashtagService.processHashtagsFromContent(null);
        verifyNoInteractions(hashtagRepository);
    }

    private User user(Long id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .name(username)
                .email(username + "@test.com")
                .password("pwd")
                .privacy(Privacy.PUBLIC)
                .userType(UserType.PERSONAL)
                .build();
    }
}
