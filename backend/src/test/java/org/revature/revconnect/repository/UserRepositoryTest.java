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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UserRepositoryTest {

    @Autowired private UserRepository userRepository;
    @Autowired private ConnectionRepository connectionRepository;

    @Test
    void basicFindersAndExists_workByUsernameAndEmail() {
        User u = saveUser("john", "john@test.com", "John", Privacy.PUBLIC, UserType.PERSONAL);

        assertTrue(userRepository.findByUsername("john").isPresent());
        assertTrue(userRepository.findByEmail("john@test.com").isPresent());
        assertTrue(userRepository.findByUsernameOrEmail("john", "x").isPresent());
        assertTrue(userRepository.existsByUsername("john"));
        assertTrue(userRepository.existsByEmail("john@test.com"));
    }

    @Test
    void searchByUsernameOrName_and_searchPublicUsers_respectPrivacy() {
        saveUser("alpha", "a@test.com", "Alice Alpha", Privacy.PUBLIC, UserType.PERSONAL);
        saveUser("beta", "b@test.com", "Alice Beta", Privacy.PRIVATE, UserType.PERSONAL);

        Page<User> broad = userRepository.searchByUsernameOrName("alice", PageRequest.of(0, 10));
        Page<User> publicOnly = userRepository.searchPublicUsers("alice", PageRequest.of(0, 10));

        assertEquals(2, broad.getTotalElements());
        assertEquals(1, publicOnly.getTotalElements());
        assertEquals("alpha", publicOnly.getContent().get(0).getUsername());
    }

    @Test
    void findSuggestedUsers_excludesCurrentUser_and_nonPublicUsers() {
        User me = saveUser("me", "me@test.com", "Me", Privacy.PUBLIC, UserType.PERSONAL);
        saveUser("pub", "pub@test.com", "Public", Privacy.PUBLIC, UserType.PERSONAL);
        saveUser("priv", "priv@test.com", "Private", Privacy.PRIVATE, UserType.PERSONAL);

        Page<User> result = userRepository.findSuggestedUsers(me.getId(), PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("pub", result.getContent().get(0).getUsername());
    }

    @Test
    void findMutualConnections_returnsCommonFollowingUsers() {
        User u1 = saveUser("u1", "u1@test.com", "U1", Privacy.PUBLIC, UserType.PERSONAL);
        User u2 = saveUser("u2", "u2@test.com", "U2", Privacy.PUBLIC, UserType.PERSONAL);
        User common = saveUser("common", "c@test.com", "Common", Privacy.PUBLIC, UserType.PERSONAL);

        connectionRepository.save(conn(u1, common, ConnectionStatus.ACCEPTED));
        connectionRepository.save(conn(u2, common, ConnectionStatus.ACCEPTED));

        Page<User> result = userRepository.findMutualConnections(u1.getId(), u2.getId(), PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
        assertEquals("common", result.getContent().get(0).getUsername());
    }

    @Test
    void advancedSearchPublicUsers_filtersByLocationTypeAndVerified() {
        User p1 = saveUser("tech", "tech@test.com", "Tech User", Privacy.PUBLIC, UserType.CREATOR);
        p1.setLocation("Hyderabad");
        p1.setIsVerified(true);
        userRepository.save(p1);

        User p2 = saveUser("biz", "biz@test.com", "Biz User", Privacy.PUBLIC, UserType.BUSINESS);
        p2.setLocation("Mumbai");
        p2.setIsVerified(false);
        userRepository.save(p2);

        User p3 = saveUser("hidden", "h@test.com", "Hidden", Privacy.PRIVATE, UserType.CREATOR);
        p3.setLocation("Hyderabad");
        p3.setIsVerified(true);
        userRepository.save(p3);

        Page<User> result = userRepository.advancedSearchPublicUsers(
                "user", "hyder", UserType.CREATOR, true, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("tech", result.getContent().get(0).getUsername());
    }

    @Test
    void countHelpers_countByUserType_and_countByIdInAndUserType() {
        User p = saveUser("p", "p@test.com", "P", Privacy.PUBLIC, UserType.PERSONAL);
        User c = saveUser("c", "c@test.com", "C", Privacy.PUBLIC, UserType.CREATOR);
        saveUser("b", "b@test.com", "B", Privacy.PUBLIC, UserType.BUSINESS);

        assertEquals(1, userRepository.countByUserType(UserType.PERSONAL));
        assertEquals(1, userRepository.countByIdInAndUserType(List.of(p.getId(), c.getId()), UserType.CREATOR));
    }

    private User saveUser(String username, String email, String name, Privacy privacy, UserType userType) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password("pwd")
                .name(name)
                .privacy(privacy)
                .userType(userType)
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
