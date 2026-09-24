package org.revature.revconnect.repository;

import org.junit.jupiter.api.Test;
import org.revature.revconnect.enums.PostType;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PostRepositoryTest {

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUserId_shouldReturnUserPosts() {
        User u1 = saveUser("u1", "u1@test.com", Privacy.PUBLIC, UserType.PERSONAL);
        savePost(u1, "Post 1", 0, 0, 0, false);
        savePost(u1, "Post 2", 0, 0, 0, false);

        List<Post> posts = postRepository.findByUserId(u1.getId());
        assertEquals(2, posts.size());
    }

    @Test
    void findPublicPosts_shouldReturnOnlyPublicUserPosts() {
        User publicUser = saveUser("public", "pub@test.com", Privacy.PUBLIC, UserType.PERSONAL);
        User privateUser = saveUser("private", "priv@test.com", Privacy.PRIVATE, UserType.PERSONAL);

        savePost(publicUser, "Public Post", 0, 0, 0, false);
        savePost(privateUser, "Private Post", 0, 0, 0, false);

        Page<Post> publicPosts = postRepository.findPublicPosts(PageRequest.of(0, 5));

        assertEquals(1, publicPosts.getTotalElements());
        assertEquals("Public Post", publicPosts.getContent().get(0).getContent());
    }

    @Test
    void findTrendingPublicPosts_shouldOrderByIdxMetrics() {
        User u1 = saveUser("u1", "u1@test.com", Privacy.PUBLIC, UserType.PERSONAL);

        savePost(u1, "Low Engagement", 5, 0, 0, false);
        savePost(u1, "High Engagement", 100, 50, 20, false);
        savePost(u1, "Medium Engagement", 20, 5, 1, false);

        Page<Post> trending = postRepository.findTrendingPublicPosts(PageRequest.of(0, 5));

        assertEquals(3, trending.getTotalElements());
        assertEquals("High Engagement", trending.getContent().get(0).getContent());
        assertEquals("Medium Engagement", trending.getContent().get(1).getContent());
        assertEquals("Low Engagement", trending.getContent().get(2).getContent());
    }

    @Test
    void findPersonalizedFeed_shouldFilterByTypeList() {
        User u1 = saveUser("u1", "u1@test.com", Privacy.PUBLIC, UserType.PERSONAL);
        User u2 = saveUser("u2", "u2@test.com", Privacy.PUBLIC, UserType.CREATOR);

        savePost(u1, "Text Post", 0, 0, 0, false);
        savePost(u2, "Image Post", 0, 0, 0, false).setPostType(PostType.IMAGE);

        Page<Post> feed = postRepository.findPersonalizedFeed(
                List.of(u1.getId(), u2.getId()), PostType.IMAGE, UserType.CREATOR, PageRequest.of(0, 5));

        assertEquals(1, feed.getTotalElements());
        assertEquals("Image Post", feed.getContent().get(0).getContent());
    }

    @Test
    void findByContentContainingTag_shouldFindPartialMatches() {
        User u1 = saveUser("u1", "u1@test.com", Privacy.PUBLIC, UserType.PERSONAL);
        savePost(u1, "Learning Java and Spring Boot", 0, 0, 0, false);
        savePost(u1, "Just drinking coffee", 0, 0, 0, false);

        Page<Post> posts = postRepository.findByContentContainingTag("java", PageRequest.of(0, 5));

        assertEquals(1, posts.getTotalElements());
        assertTrue(posts.getContent().get(0).getContent().contains("Java"));
    }

    @Test
    void searchPosts_shouldWorkWithMultipleFilters() {
        User u1 = saveUser("author1", "a1@test.com", Privacy.PUBLIC, UserType.PERSONAL);
        Post p1 = savePost(u1, "Awesome post", 50, 0, 0, false);

        Page<Post> searchResult = postRepository.searchPosts(
                "awesome", "author", PostType.TEXT, 10,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                PageRequest.of(0, 5));

        assertEquals(1, searchResult.getTotalElements());
        assertEquals("Awesome post", searchResult.getContent().get(0).getContent());
    }

    @Test
    void findByUserIdWithPinnedFirst_shouldReturnPinnedFirst() {
        User u1 = saveUser("u1", "u1@test.com", Privacy.PUBLIC, UserType.PERSONAL);

        savePost(u1, "Unpinned 1", 0, 0, 0, false);
        savePost(u1, "Pinned", 0, 0, 0, true);
        savePost(u1, "Unpinned 2", 0, 0, 0, false);

        Page<Post> posts = postRepository.findByUserIdWithPinnedFirst(u1.getId(), PageRequest.of(0, 5));

        assertEquals(3, posts.getTotalElements());
        assertTrue(posts.getContent().get(0).getPinned());
        assertEquals("Pinned", posts.getContent().get(0).getContent());
    }

    @Test
    void aggregateMethods_shouldReturnSumOfActivity() {
        User u1 = saveUser("u1", "u1@test.com", Privacy.PUBLIC, UserType.PERSONAL);

        savePost(u1, "P1", 10, 5, 2, false);
        savePost(u1, "P2", 20, 15, 3, false);

        assertEquals(30L, postRepository.getTotalLikesByUserId(u1.getId()));
        assertEquals(20L, postRepository.getTotalCommentsByUserId(u1.getId()));
        assertEquals(5L, postRepository.getTotalSharesByUserId(u1.getId()));
    }

    @Test
    void findByIdAndUserId_shouldReturnPostIfOwner() {
        User u1 = saveUser("u1", "u1@test.com", Privacy.PUBLIC, UserType.PERSONAL);
        User u2 = saveUser("u2", "u2@test.com", Privacy.PUBLIC, UserType.PERSONAL);

        Post p1 = savePost(u1, "Hello", 0, 0, 0, false);

        Optional<Post> own = postRepository.findByIdAndUserId(p1.getId(), u1.getId());
        Optional<Post> other = postRepository.findByIdAndUserId(p1.getId(), u2.getId());

        assertTrue(own.isPresent());
        assertFalse(other.isPresent());
    }

    private User saveUser(String username, String email, Privacy privacy, UserType userType) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password("pwd")
                .name(username)
                .privacy(privacy)
                .userType(userType)
                .build());
    }

    private Post savePost(User user, String content, int likes, int comments, int shares, boolean pinned) {
        return postRepository.save(Post.builder()
                .user(user)
                .content(content)
                .postType(PostType.TEXT)
                .likeCount(likes)
                .commentCount(comments)
                .shareCount(shares)
                .pinned(pinned)
                .build());
    }
}