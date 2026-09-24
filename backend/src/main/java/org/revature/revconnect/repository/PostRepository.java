package org.revature.revconnect.repository;

import org.revature.revconnect.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Post> findByUserId(Long userId);

    @Query("SELECT p FROM Post p WHERE p.user.privacy = 'PUBLIC' ORDER BY p.createdAt DESC")
    Page<Post> findPublicPosts(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user.privacy = 'PUBLIC' " +
            "ORDER BY (p.likeCount + p.commentCount + p.shareCount) DESC, p.createdAt DESC")
    Page<Post> findTrendingPublicPosts(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user.id IN :userIds ORDER BY p.createdAt DESC")
    Page<Post> findByUserIdIn(@Param("userIds") List<Long> userIds, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user.id IN :userIds AND " +
            "(:postType IS NULL OR p.postType = :postType) AND " +
            "(:userType IS NULL OR p.user.userType = :userType) " +
            "ORDER BY p.createdAt DESC")
    Page<Post> findPersonalizedFeed(@Param("userIds") List<Long> userIds,
                                    @Param("postType") org.revature.revconnect.enums.PostType postType,
                                    @Param("userType") org.revature.revconnect.enums.UserType userType,
                                    Pageable pageable);

    @Query("SELECT p FROM Post p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :tag, '%')) ORDER BY p.createdAt DESC")
    Page<Post> findByContentContainingTag(@Param("tag") String tag, Pageable pageable);

    Page<Post> findByContentContainingIgnoreCase(String query, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user.privacy = 'PUBLIC' AND " +
            "(:query IS NULL OR LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:author IS NULL OR LOWER(p.user.username) LIKE LOWER(CONCAT('%', :author, '%'))) AND " +
            "(:postType IS NULL OR p.postType = :postType) AND " +
            "(:minLikes IS NULL OR p.likeCount >= :minLikes) AND " +
            "(:dateFrom IS NULL OR p.createdAt >= :dateFrom) AND " +
            "(:dateTo IS NULL OR p.createdAt <= :dateTo) " +
            "ORDER BY p.createdAt DESC")
    Page<Post> searchPosts(@Param("query") String query,
                           @Param("author") String author,
                           @Param("postType") org.revature.revconnect.enums.PostType postType,
                           @Param("minLikes") Integer minLikes,
                           @Param("dateFrom") java.time.LocalDateTime dateFrom,
                           @Param("dateTo") java.time.LocalDateTime dateTo,
                           Pageable pageable);

    List<Post> findByUserIdAndPinnedTrueOrderByCreatedAtDesc(Long userId);

    @Query("SELECT p FROM Post p WHERE p.user.id = :userId ORDER BY p.pinned DESC, p.createdAt DESC")
    Page<Post> findByUserIdWithPinnedFirst(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.likeCount), 0) FROM Post p WHERE p.user.id = :userId")
    Long getTotalLikesByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(p.commentCount), 0) FROM Post p WHERE p.user.id = :userId")
    Long getTotalCommentsByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(p.shareCount), 0) FROM Post p WHERE p.user.id = :userId")
    Long getTotalSharesByUserId(@Param("userId") Long userId);

    @Query("SELECT p FROM Post p WHERE p.user.id = :userId ORDER BY (p.likeCount + p.commentCount + p.shareCount) DESC, p.createdAt DESC")
    Page<Post> findTopPostsByUserId(@Param("userId") Long userId, Pageable pageable);

    Optional<Post> findByIdAndUserId(Long postId, Long userId);

    @Query("SELECT p FROM Post p JOIN Like l ON p.id = l.post.id WHERE l.user.id = :userId ORDER BY l.createdAt DESC")
    Page<Post> findLikedPostsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.user.id = :userId AND SIZE(p.mediaUrls) > 0 ORDER BY p.createdAt DESC")
    Page<Post> findMediaPostsByUserId(@Param("userId") Long userId, Pageable pageable);
}
