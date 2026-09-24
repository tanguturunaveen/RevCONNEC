package org.revature.revconnect.repository;

import org.revature.revconnect.model.PostAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostAnalyticsRepository extends JpaRepository<PostAnalytics, Long> {

    Optional<PostAnalytics> findByPostIdAndDate(Long postId, LocalDate date);

    List<PostAnalytics> findByPostIdAndDateBetweenOrderByDateAsc(Long postId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(pa.views) FROM PostAnalytics pa WHERE pa.post.id = :postId")
    Long getTotalViews(@Param("postId") Long postId);

    @Query("SELECT SUM(pa.views) FROM PostAnalytics pa WHERE pa.post.user.id = :userId")
    Long getTotalViewsByUser(@Param("userId") Long userId);

    @Query("SELECT SUM(pa.impressions) FROM PostAnalytics pa WHERE pa.post.user.id = :userId")
    Long getTotalImpressionsByUser(@Param("userId") Long userId);

    @Query("SELECT pa FROM PostAnalytics pa WHERE pa.post.user.id = :userId AND pa.date BETWEEN :startDate AND :endDate ORDER BY pa.date ASC")
    List<PostAnalytics> findByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    void deleteByPostId(Long postId);
}
