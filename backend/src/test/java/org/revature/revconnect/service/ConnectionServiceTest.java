package org.revature.revconnect.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.enums.ConnectionStatus;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.exception.BadRequestException;
import org.revature.revconnect.mapper.ConnectionMapper;
import org.revature.revconnect.model.Connection;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.ConnectionRepository;
import org.revature.revconnect.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionServiceTest {

    @Mock
    private ConnectionRepository connectionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthService authService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ConnectionMapper connectionMapper;

    @InjectMocks
    private ConnectionService connectionService;

    @Test
    void followUser_personal_createsPending() {
        User me = user(1L, "me", UserType.PERSONAL);
        User target = user(2L, "target", UserType.PERSONAL);

        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(connectionRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.empty());

        connectionService.followUser(2L);

        verify(connectionRepository).save(any(Connection.class));
        verify(notificationService).notifyConnectionRequest(target, me);
    }

    @Test
    void followUser_business_createsAccepted() {
        User me = user(1L, "me", UserType.PERSONAL);
        User target = user(3L, "biz", UserType.BUSINESS);

        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(3L)).thenReturn(Optional.of(target));
        when(connectionRepository.findByFollowerIdAndFollowingId(1L, 3L)).thenReturn(Optional.empty());

        connectionService.followUser(3L);

        verify(connectionRepository).save(any(Connection.class));
        verify(notificationService).notifyFollow(target, me);
    }

    @Test
    void followUser_self_throws() {
        User me = user(1L, "me", UserType.PERSONAL);
        when(authService.getCurrentUser()).thenReturn(me);

        assertThrows(BadRequestException.class, () -> connectionService.followUser(1L));
    }

    @Test
    void followUser_duplicate_throws() {
        User me = user(1L, "me", UserType.PERSONAL);
        User target = user(2L, "t", UserType.PERSONAL);
        Connection existing = Connection.builder().status(ConnectionStatus.ACCEPTED).build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(connectionRepository.findByFollowerIdAndFollowingId(1L, 2L)).thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class, () -> connectionService.followUser(2L));
    }

    @Test
    void unfollowUser_notFollowing_throws() {
        User me = user(1L, "me", UserType.PERSONAL);
        User target = user(9L, "target", UserType.PERSONAL);
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(9L)).thenReturn(Optional.of(target));
        when(connectionRepository.findByFollowerIdAndFollowingId(1L, 9L)).thenReturn(Optional.empty());
        when(connectionRepository.findByFollowerIdAndFollowingId(9L, 1L)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> connectionService.unfollowUser(9L));
    }

    @Test
    void acceptRequest_notReceiver_throws() {
        User me = user(1L, "me", UserType.PERSONAL);
        User follower = user(2L, "f", UserType.PERSONAL);
        User otherFollowing = user(3L, "x", UserType.PERSONAL);
        Connection c = Connection.builder().id(10L).follower(follower).following(otherFollowing)
                .status(ConnectionStatus.PENDING).build();

        when(authService.getCurrentUser()).thenReturn(me);
        when(connectionRepository.findById(10L)).thenReturn(Optional.of(c));

        assertThrows(BadRequestException.class, () -> connectionService.acceptRequest(10L));
    }

    @Test
    void acceptRequest_nonPending_throws() {
        User me = user(1L, "me", UserType.PERSONAL);
        User follower = user(2L, "f", UserType.PERSONAL);
        Connection c = Connection.builder().id(10L).follower(follower).following(me)
                .status(ConnectionStatus.ACCEPTED).build();

        when(authService.getCurrentUser()).thenReturn(me);
        when(connectionRepository.findById(10L)).thenReturn(Optional.of(c));

        assertThrows(BadRequestException.class, () -> connectionService.acceptRequest(10L));
    }

    @Test
    void acceptRequest_success_updatesStatus() {
        User me = user(1L, "me", UserType.PERSONAL);
        User follower = user(2L, "f", UserType.PERSONAL);
        Connection c = Connection.builder().id(10L).follower(follower).following(me)
                .status(ConnectionStatus.PENDING).build();

        when(authService.getCurrentUser()).thenReturn(me);
        when(connectionRepository.findById(10L)).thenReturn(Optional.of(c));

        connectionService.acceptRequest(10L);

        assertEquals(ConnectionStatus.ACCEPTED, c.getStatus());
        verify(connectionRepository).save(c);
        verify(notificationService).notifyConnectionAccepted(follower, me);
    }

    @Test
    void isFollowing_true() {
        User me = user(1L, "alice", UserType.PERSONAL);
        User target = user(2L, "target", UserType.PERSONAL);
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(connectionRepository.existsByFollowerIdAndFollowingIdAndStatus(1L, 2L, ConnectionStatus.ACCEPTED))
                .thenReturn(true);
        when(connectionRepository.existsByFollowerIdAndFollowingIdAndStatus(2L, 1L, ConnectionStatus.ACCEPTED))
                .thenReturn(false);

        assertTrue(connectionService.isFollowing(2L));
    }

    @Test
    void isFollowing_false() {
        User me = user(1L, "alice", UserType.PERSONAL);
        User target = user(3L, "target", UserType.PERSONAL);
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(3L)).thenReturn(Optional.of(target));
        when(connectionRepository.existsByFollowerIdAndFollowingIdAndStatus(1L, 3L, ConnectionStatus.ACCEPTED))
                .thenReturn(false);
        when(connectionRepository.existsByFollowerIdAndFollowingIdAndStatus(3L, 1L, ConnectionStatus.ACCEPTED))
                .thenReturn(false);

        assertFalse(connectionService.isFollowing(3L));
    }

    @Test
    void removeConnection_whenMissing_throws() {
        User me = user(1L, "alice", UserType.PERSONAL);
        when(authService.getCurrentUser()).thenReturn(me);
        when(connectionRepository.findBetweenUsers(1L, 9L)).thenReturn(List.of());

        assertThrows(BadRequestException.class, () -> connectionService.removeConnection(9L));
    }

    @Test
    void removeConnection_whenExists_deletesAll() {
        User me = user(1L, "alice", UserType.PERSONAL);
        Connection c = Connection.builder().id(1L).build();
        when(authService.getCurrentUser()).thenReturn(me);
        when(connectionRepository.findBetweenUsers(1L, 9L)).thenReturn(List.of(c));

        connectionService.removeConnection(9L);
        verify(connectionRepository).deleteAll(List.of(c));
    }

    private User user(Long id, String username, UserType type) {
        return User.builder().id(id).username(username).name(username)
                .email(username + "@test.com").password("x").userType(type).build();
    }
}
