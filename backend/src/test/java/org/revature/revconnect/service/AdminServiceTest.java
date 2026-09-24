package org.revature.revconnect.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.dto.response.PagedResponse;
import org.revature.revconnect.dto.response.UserResponse;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.mapper.UserMapper;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.PostRepository;
import org.revature.revconnect.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PostRepository postRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private AdminService adminService;

    @Test
    void getAllUsers_returnsMappedPagedResponse() {
        User u = user(1L, "u1", false, UserType.PERSONAL);
        UserResponse dto = UserResponse.builder().id(1L).username("u1").build();
        Page<User> page = new PageImpl<>(List.of(u), PageRequest.of(0, 5), 1);
        when(userRepository.findAll(PageRequest.of(0, 5))).thenReturn(page);
        when(userMapper.toResponse(u)).thenReturn(dto);

        PagedResponse<UserResponse> result = adminService.getAllUsers(0, 5);

        assertEquals(1, result.getContent().size());
        assertEquals("u1", result.getContent().get(0).getUsername());
    }

    @Test
    void verifyUser_setsVerifiedTrueAndSaves() {
        User u = user(1L, "u1", false, UserType.PERSONAL);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        adminService.verifyUser(1L);

        assertEquals(true, u.getIsVerified());
        verify(userRepository).save(u);
    }

    @Test
    void unverifyUser_setsVerifiedFalseAndSaves() {
        User u = user(1L, "u1", true, UserType.PERSONAL);
        when(userRepository.findById(1L)).thenReturn(Optional.of(u));

        adminService.unverifyUser(1L);

        assertEquals(false, u.getIsVerified());
        verify(userRepository).save(u);
    }

    @Test
    void verifyUser_whenUserMissing_throwsResourceNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminService.verifyUser(99L));
    }

    @Test
    void deleteUser_deletesFoundUser() {
        User u = user(3L, "u3", false, UserType.CREATOR);
        when(userRepository.findById(3L)).thenReturn(Optional.of(u));

        adminService.deleteUser(3L);

        verify(userRepository).delete(u);
    }

    @Test
    void deletePost_deletesFoundPost() {
        Post p = Post.builder().id(10L).content("c").user(user(1L, "u1", false, UserType.PERSONAL)).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(p));

        adminService.deletePost(10L);

        verify(postRepository).delete(p);
    }

    @Test
    void deletePost_whenMissing_throwsResourceNotFound() {
        when(postRepository.findById(44L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> adminService.deletePost(44L));
    }

    @Test
    void getReport_returnsNotImplementedShape() {
        Map<String, Object> report = adminService.getReport(22L);

        assertEquals(22L, report.get("id"));
        assertEquals("NOT_AVAILABLE", report.get("status"));
    }

    @Test
    void getReports_and_getFlaggedPosts_returnEmptyList() {
        assertEquals(0, adminService.getReports(0, 10).size());
        assertEquals(0, adminService.getFlaggedPosts().size());
    }

    @Test
    void getPlatformStats_returnsAggregateCounts() {
        when(userRepository.count()).thenReturn(5L);
        when(postRepository.count()).thenReturn(8L);
        when(userRepository.findAll()).thenReturn(List.of(
                user(1L, "u1", true, UserType.PERSONAL),
                user(2L, "u2", false, UserType.PERSONAL),
                user(3L, "u3", true, UserType.BUSINESS)
        ));

        Map<String, Object> stats = adminService.getPlatformStats();

        assertEquals(5L, stats.get("totalUsers"));
        assertEquals(8L, stats.get("totalPosts"));
        assertEquals(2L, stats.get("verifiedUsers"));
        assertEquals(5L, stats.get("activeUsers"));
    }

    @Test
    void getUserStats_returnsTypeWiseCounts() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByUserType(UserType.PERSONAL)).thenReturn(6L);
        when(userRepository.countByUserType(UserType.CREATOR)).thenReturn(2L);
        when(userRepository.countByUserType(UserType.BUSINESS)).thenReturn(2L);
        when(userRepository.findAll()).thenReturn(List.of(
                user(1L, "u1", true, UserType.PERSONAL),
                user(2L, "u2", true, UserType.CREATOR),
                user(3L, "u3", false, UserType.BUSINESS)
        ));

        Map<String, Object> stats = adminService.getUserStats();

        assertEquals(10L, stats.get("total"));
        assertEquals(6L, stats.get("personal"));
        assertEquals(2L, stats.get("creator"));
        assertEquals(2L, stats.get("business"));
        assertEquals(2L, stats.get("verified"));
    }

    @Test
    void getPostStats_handlesNullCountsAndSumsEngagement() {
        Post p1 = Post.builder().id(1L).likeCount(5).commentCount(2).shareCount(1).build();
        Post p2 = Post.builder().id(2L).likeCount(null).commentCount(3).shareCount(null).build();
        when(postRepository.findAll()).thenReturn(List.of(p1, p2));

        Map<String, Object> stats = adminService.getPostStats();

        assertEquals(2, stats.get("totalPosts"));
        assertEquals(5L, stats.get("totalLikes"));
        assertEquals(5L, stats.get("totalComments"));
        assertEquals(1L, stats.get("totalShares"));
    }

    @Test
    void getEngagementStats_returnsRoundedAverage() {
        Post p1 = Post.builder().id(1L).likeCount(3).commentCount(1).shareCount(0).build();
        Post p2 = Post.builder().id(2L).likeCount(1).commentCount(2).shareCount(2).build();
        when(postRepository.findAll()).thenReturn(List.of(p1, p2));

        Map<String, Object> stats = adminService.getEngagementStats();

        assertEquals(9L, stats.get("totalEngagement"));
        assertEquals(4.5, stats.get("avgEngagementPerPost"));
        assertEquals(2L, stats.get("posts"));
    }

    @Test
    void getAuditLogs_returnsEmptyList() {
        assertEquals(0, adminService.getAuditLogs(0, 20).size());
    }

    private User user(Long id, String username, boolean verified, UserType userType) {
        return User.builder()
                .id(id)
                .username(username)
                .email(username + "@test.com")
                .name(username)
                .password("pwd")
                .privacy(Privacy.PUBLIC)
                .userType(userType)
                .isVerified(verified)
                .build();
    }
}
