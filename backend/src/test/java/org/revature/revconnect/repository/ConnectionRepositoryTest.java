package org.revature.revconnect.repository;

import org.junit.jupiter.api.Test;
import org.revature.revconnect.enums.ConnectionStatus;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.model.Connection;
import org.revature.revconnect.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class ConnectionRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ConnectionRepository connectionRepository;

    @Test
    void findByFollowerIdAndFollowingId_shouldReturnConnection() {
        User u1 = saveUser("user1", "user1@test.com", "User One");
        User u2 = saveUser("user2", "user2@test.com", "User Two");

        connectionRepository.save(conn(u1, u2, ConnectionStatus.PENDING));

        Optional<Connection> result = connectionRepository.findByFollowerIdAndFollowingId(u1.getId(), u2.getId());
        assertTrue(result.isPresent());
        assertEquals(u1.getId(), result.get().getFollower().getId());
        assertEquals(u2.getId(), result.get().getFollowing().getId());
        assertEquals(ConnectionStatus.PENDING, result.get().getStatus());
    }

    @Test
    void existsByFollowerIdAndFollowingId_shouldReturnTrueWhenExists() {
        User u1 = saveUser("user1", "user1@test.com", "User One");
        User u2 = saveUser("user2", "user2@test.com", "User Two");

        connectionRepository.save(conn(u1, u2, ConnectionStatus.ACCEPTED));

        assertTrue(connectionRepository.existsByFollowerIdAndFollowingId(u1.getId(), u2.getId()));
        assertFalse(connectionRepository.existsByFollowerIdAndFollowingId(u2.getId(), u1.getId()));
    }

    @Test
    void existsByFollowerIdAndFollowingIdAndStatus_shouldReturnMatches() {
        User u1 = saveUser("user1", "user1@test.com", "User One");
        User u2 = saveUser("user2", "user2@test.com", "User Two");

        connectionRepository.save(conn(u1, u2, ConnectionStatus.ACCEPTED));

        assertTrue(connectionRepository.existsByFollowerIdAndFollowingIdAndStatus(u1.getId(), u2.getId(),
                ConnectionStatus.ACCEPTED));
        assertFalse(connectionRepository.existsByFollowerIdAndFollowingIdAndStatus(u1.getId(), u2.getId(),
                ConnectionStatus.PENDING));
    }

    @Test
    void findFollowersByUserId_shouldReturnFollowersWithStatus() {
        User target = saveUser("target", "target@test.com", "Target User");
        User f1 = saveUser("f1", "f1@test.com", "Follower 1");
        User f2 = saveUser("f2", "f2@test.com", "Follower 2");

        connectionRepository.save(conn(f1, target, ConnectionStatus.ACCEPTED));
        connectionRepository.save(conn(f2, target, ConnectionStatus.PENDING));

        Page<Connection> acceptedFollowers = connectionRepository.findFollowersByUserId(target.getId(),
                ConnectionStatus.ACCEPTED, PageRequest.of(0, 10));
        assertEquals(1, acceptedFollowers.getTotalElements());
        assertEquals(f1.getId(), acceptedFollowers.getContent().get(0).getFollower().getId());
    }

    @Test
    void findFollowingByUserId_shouldReturnFollowingWithStatus() {
        User user = saveUser("user", "user@test.com", "User");
        User t1 = saveUser("t1", "t1@test.com", "Target 1");
        User t2 = saveUser("t2", "t2@test.com", "Target 2");

        connectionRepository.save(conn(user, t1, ConnectionStatus.ACCEPTED));
        connectionRepository.save(conn(user, t2, ConnectionStatus.REJECTED));

        Page<Connection> following = connectionRepository.findFollowingByUserId(user.getId(), ConnectionStatus.ACCEPTED,
                PageRequest.of(0, 10));
        assertEquals(1, following.getTotalElements());
        assertEquals(t1.getId(), following.getContent().get(0).getFollowing().getId());
    }

    @Test
    void findPendingRequestsByUserId_shouldReturnPendingReceived() {
        User target = saveUser("target", "target@test.com", "Target");
        User requester = saveUser("requester", "req@test.com", "Requester");

        connectionRepository.save(conn(requester, target, ConnectionStatus.PENDING));
        connectionRepository.save(conn(target, requester, ConnectionStatus.PENDING));

        Page<Connection> pendingRecv = connectionRepository.findPendingRequestsByUserId(target.getId(),
                PageRequest.of(0, 10));
        assertEquals(1, pendingRecv.getTotalElements());
        assertEquals(requester.getId(), pendingRecv.getContent().get(0).getFollower().getId());
    }

    @Test
    void findSentPendingRequestsByUserId_shouldReturnPendingSent() {
        User sender = saveUser("sender", "sender@test.com", "Sender");
        User target = saveUser("target", "target@test.com", "Target");

        connectionRepository.save(conn(sender, target, ConnectionStatus.PENDING));

        Page<Connection> pendingSent = connectionRepository.findSentPendingRequestsByUserId(sender.getId(),
                PageRequest.of(0, 10));
        assertEquals(1, pendingSent.getTotalElements());
        assertEquals(target.getId(), pendingSent.getContent().get(0).getFollowing().getId());
    }

    @Test
    void countHelpers_shouldReturnCorrectCounts() {
        User u1 = saveUser("u1", "u1@test.com", "U1");
        User u2 = saveUser("u2", "u2@test.com", "U2");
        User u3 = saveUser("u3", "u3@test.com", "U3");

        connectionRepository.save(conn(u2, u1, ConnectionStatus.ACCEPTED));
        connectionRepository.save(conn(u3, u1, ConnectionStatus.ACCEPTED));
        connectionRepository.save(conn(u1, u3, ConnectionStatus.PENDING));

        assertEquals(2, connectionRepository.countByFollowingIdAndStatus(u1.getId(), ConnectionStatus.ACCEPTED));
        assertEquals(1, connectionRepository.countByFollowerIdAndStatus(u1.getId(), ConnectionStatus.PENDING));
    }

    @Test
    void findFollowingUserIds_shouldReturnAcceptedIds() {
        User u = saveUser("u", "u@test.com", "U");
        User f1 = saveUser("f1", "f1@test.com", "F1");
        User f2 = saveUser("f2", "f2@test.com", "F2");

        connectionRepository.save(conn(u, f1, ConnectionStatus.ACCEPTED));
        connectionRepository.save(conn(u, f2, ConnectionStatus.PENDING));

        List<Long> followingIds = connectionRepository.findFollowingUserIds(u.getId());
        assertEquals(1, followingIds.size());
        assertTrue(followingIds.contains(f1.getId()));
    }

    @Test
    void findFollowerUserIds_shouldReturnAcceptedIds() {
        User target = saveUser("target", "target@test.com", "Target");
        User f1 = saveUser("f1", "f1@test.com", "F1");
        User f2 = saveUser("f2", "f2@test.com", "F2");

        connectionRepository.save(conn(f1, target, ConnectionStatus.ACCEPTED));
        connectionRepository.save(conn(f2, target, ConnectionStatus.PENDING));

        List<Long> followerIds = connectionRepository.findFollowerUserIds(target.getId());
        assertEquals(1, followerIds.size());
        assertTrue(followerIds.contains(f1.getId()));
    }

    @Test
    void findBetweenUsers_shouldReturnBidirectionalConnections() {
        User u1 = saveUser("u1", "u1@test.com", "U1");
        User u2 = saveUser("u2", "u2@test.com", "U2");
        User u3 = saveUser("u3", "u3@test.com", "U3");

        connectionRepository.save(conn(u1, u2, ConnectionStatus.ACCEPTED));
        connectionRepository.save(conn(u2, u1, ConnectionStatus.PENDING));
        connectionRepository.save(conn(u1, u3, ConnectionStatus.ACCEPTED));

        List<Connection> between1and2 = connectionRepository.findBetweenUsers(u1.getId(), u2.getId());
        assertEquals(2, between1and2.size());

        List<Connection> between1and3 = connectionRepository.findBetweenUsers(u1.getId(), u3.getId());
        assertEquals(1, between1and3.size());
    }

    private User saveUser(String username, String email, String name) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password("pwd")
                .name(name)
                .privacy(Privacy.PUBLIC)
                .userType(UserType.PERSONAL)
                .build());
    }

    private Connection conn(User follower, User following, ConnectionStatus status) {
        return Connection.builder()
                .follower(follower)
                .following(following)
                .status(status)
                .build();
    }
}