package org.revature.revconnect.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.dto.request.ProfileUpdateRequest;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.UserResponse;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.mapper.UserMapper;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthService authService;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    @Test
    void getMyProfile_returnsMappedCurrentUser() {
        User me = user(1L, "me", Privacy.PUBLIC);
        UserResponse dto = UserResponse.builder().id(1L).username("me").build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(userMapper.toResponse(me)).thenReturn(dto);

        UserResponse result = userService.getMyProfile();

        assertEquals(1L, result.getId());
        assertEquals("me", result.getUsername());
    }

    @Test
    void getUserById_whenMissing_throwsNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    void getUserById_whenPrivateOtherUser_throwsUnauthorized() {
        User target = user(2L, "target", Privacy.PRIVATE);
        User me = user(1L, "me", Privacy.PUBLIC);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(authService.getCurrentUser()).thenReturn(me);

        assertThrows(UnauthorizedException.class, () -> userService.getUserById(2L));
    }

    @Test
    void getUserById_whenOwnPrivateProfile_allowsAccess() {
        User me = user(1L, "me", Privacy.PRIVATE);
        UserResponse dto = UserResponse.builder().id(1L).username("me").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(me));
        when(authService.getCurrentUser()).thenReturn(me);
        when(userMapper.toPublicResponse(me)).thenReturn(dto);

        UserResponse result = userService.getUserById(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void getUserByUsername_whenPrivateOtherUser_throwsUnauthorized() {
        User target = user(2L, "target", Privacy.PRIVATE);
        User me = user(1L, "me", Privacy.PUBLIC);
        when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
        when(authService.getCurrentUser()).thenReturn(me);

        assertThrows(UnauthorizedException.class, () -> userService.getUserByUsername("target"));
    }

    @Test
    void getUserByUsername_public_returnsMappedResponse() {
        User target = user(2L, "target", Privacy.PUBLIC);
        User me = user(1L, "me", Privacy.PUBLIC);
        UserResponse dto = UserResponse.builder().id(2L).username("target").build();
        when(userRepository.findByUsername("target")).thenReturn(Optional.of(target));
        when(authService.getCurrentUser()).thenReturn(me);
        when(userMapper.toPublicResponse(target)).thenReturn(dto);

        UserResponse result = userService.getUserByUsername("target");

        assertEquals("target", result.getUsername());
    }

    @Test
    void updateProfile_updatesAllProvidedFields() {
        User me = user(1L, "me", Privacy.PUBLIC);
        me.setUserType(UserType.BUSINESS);
        ProfileUpdateRequest req = ProfileUpdateRequest.builder()
                .name("New Name")
                .bio("New Bio")
                .profilePicture("pic.png")
                .location("Hyderabad")
                .website("https://example.com")
                .privacy(Privacy.PRIVATE)
                .businessName("Rev Biz")
                .category("Technology")
                .industry("Software")
                .contactInfo("9999999999")
                .businessAddress("Hitech City")
                .businessHours("9-6")
                .externalLinks("https://x.com/rev")
                .socialMediaLinks("https://instagram.com/rev")
                .build();
        UserResponse dto = UserResponse.builder().id(1L).username("me").build();

        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.save(me)).thenReturn(me);
        when(userMapper.toResponse(me)).thenReturn(dto);

        UserResponse result = userService.updateProfile(req);

        assertEquals("New Name", me.getName());
        assertEquals(Privacy.PRIVATE, me.getPrivacy());
        assertEquals("Rev Biz", me.getBusinessName());
        assertEquals("Technology", me.getCategory());
        assertEquals("Software", me.getIndustry());
        assertEquals(1L, result.getId());
    }

    @Test
    void searchUsers_returnsPagedPublicResponses() {
        User u = user(2L, "target", Privacy.PUBLIC);
        UserResponse dto = UserResponse.builder().id(2L).username("target").build();
        Page<User> page = new PageImpl<>(List.of(u), PageRequest.of(0, 5), 1);
        when(userRepository.searchPublicUsers("tar", PageRequest.of(0, 5))).thenReturn(page);
        when(userMapper.toPublicResponse(u)).thenReturn(dto);

        PagedResponse<UserResponse> result = userService.searchUsers("tar", 0, 5);

        assertEquals(1, result.getContent().size());
        assertEquals("target", result.getContent().get(0).getUsername());
    }

    @Test
    void updatePrivacy_updatesAndReturnsMappedUser() {
        User me = user(1L, "me", Privacy.PUBLIC);
        UserResponse dto = UserResponse.builder().id(1L).username("me").privacy(Privacy.PRIVATE).build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.save(me)).thenReturn(me);
        when(userMapper.toResponse(me)).thenReturn(dto);

        UserResponse result = userService.updatePrivacy(Privacy.PRIVATE);

        assertEquals(Privacy.PRIVATE, me.getPrivacy());
        assertEquals(Privacy.PRIVATE, result.getPrivacy());
    }

    @Test
    void getSuggestedUsers_returnsMappedPagedResponse() {
        User me = user(1L, "me", Privacy.PUBLIC);
        User u = user(2L, "target", Privacy.PUBLIC);
        UserResponse dto = UserResponse.builder().id(2L).username("target").build();
        Page<User> page = new PageImpl<>(List.of(u), PageRequest.of(0, 3), 1);
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findSuggestedUsers(1L, PageRequest.of(0, 3))).thenReturn(page);
        when(userMapper.toPublicResponse(u)).thenReturn(dto);

        PagedResponse<UserResponse> result = userService.getSuggestedUsers(0, 3);

        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getTotalElements());
    }

    @Test
    void blockUser_whenSelf_throwsBadRequest() {
        User me = user(1L, "me", Privacy.PUBLIC);
        when(authService.getCurrentUser()).thenReturn(me);

        assertThrows(BadRequestException.class, () -> userService.blockUser(1L));
        verify(userRepository, never()).findById(any());
    }

    @Test
    void blockUser_whenTargetMissing_throwsNotFound() {
        User me = user(1L, "me", Privacy.PUBLIC);
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.blockUser(99L));
    }

    @Test
    void unblockUser_whenTargetMissing_throwsNotFound() {
        User me = user(1L, "me", Privacy.PUBLIC);
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.unblockUser(99L));
    }

    @Test
    void getBlockedUsers_returnsEmptyPagedResponseShape() {
        User me = user(1L, "me", Privacy.PUBLIC);
        when(authService.getCurrentUser()).thenReturn(me);

        PagedResponse<UserResponse> result = userService.getBlockedUsers(2, 10);

        assertEquals(0, result.getContent().size());
        assertEquals(2, result.getPageNumber());
        assertEquals(10, result.getPageSize());
    }

    @Test
    void reportUser_whenSelf_throwsBadRequest() {
        User me = user(1L, "me", Privacy.PUBLIC);
        when(authService.getCurrentUser()).thenReturn(me);

        assertThrows(BadRequestException.class, () -> userService.reportUser(1L, "spam"));
    }

    @Test
    void reportUser_whenTargetMissing_throwsNotFound() {
        User me = user(1L, "me", Privacy.PUBLIC);
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.reportUser(2L, "spam"));
    }

    @Test
    void getMutualConnections_whenTargetMissing_throwsNotFound() {
        User me = user(1L, "me", Privacy.PUBLIC);
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getMutualConnections(2L, 0, 5));
    }

    @Test
    void getMutualConnections_returnsMappedPagedResponse() {
        User me = user(1L, "me", Privacy.PUBLIC);
        User target = user(2L, "target", Privacy.PUBLIC);
        User mutual = user(3L, "mutual", Privacy.PUBLIC);
        UserResponse dto = UserResponse.builder().id(3L).username("mutual").build();
        Page<User> page = new PageImpl<>(List.of(mutual), PageRequest.of(0, 5), 1);

        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.findMutualConnections(1L, 2L, PageRequest.of(0, 5))).thenReturn(page);
        when(userMapper.toPublicResponse(mutual)).thenReturn(dto);

        PagedResponse<UserResponse> result = userService.getMutualConnections(2L, 0, 5);

        assertEquals(1, result.getContent().size());
        assertEquals("mutual", result.getContent().get(0).getUsername());
    }

    private User user(Long id, String username, Privacy privacy) {
        return User.builder()
                .id(id)
                .username(username)
                .email(username + "@test.com")
                .name(username)
                .password("pwd")
                .privacy(privacy)
                .userType(UserType.PERSONAL)
                .build();
    }
}
