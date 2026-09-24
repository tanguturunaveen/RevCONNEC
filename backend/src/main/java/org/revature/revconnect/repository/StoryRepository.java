package org.revature.revconnect.repository;

import org.revature.revconnect.model.Story;
import org.revature.revconnect.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {

    List<Story> findByUserAndExpiresAtAfterOrderByCreatedAtDesc(User user, LocalDateTime now);

    @Query("SELECT s FROM Story s WHERE s.user IN :users AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<Story> findActiveStoriesByUsers(@Param("users") List<User> users, @Param("now") LocalDateTime now);

    List<Story> findByUserAndIsHighlightTrueOrderByCreatedAtDesc(User user);

    List<Story> findByUserAndExpiresAtBeforeOrderByCreatedAtDesc(User user, LocalDateTime now);

    @Query("SELECT s FROM Story s WHERE s.expiresAt < :now")
    List<Story> findExpiredStories(@Param("now") LocalDateTime now);
}
