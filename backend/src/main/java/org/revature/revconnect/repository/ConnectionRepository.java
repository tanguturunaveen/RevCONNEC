package org.revature.revconnect.repository;

import org.revature.revconnect.enums.ConnectionStatus;
import org.revature.revconnect.model.Connection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConnectionRepository extends JpaRepository<Connection, Long> {

    Optional<Connection> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingIdAndStatus(Long followerId, Long followingId, ConnectionStatus status);

    @Query("SELECT c FROM Connection c WHERE c.following.id = :userId AND c.status = :status")
    Page<Connection> findFollowersByUserId(@Param("userId") Long userId, @Param("status") ConnectionStatus status,
                                           Pageable pageable);

    @Query("SELECT c FROM Connection c WHERE c.follower.id = :userId AND c.status = :status")
    Page<Connection> findFollowingByUserId(@Param("userId") Long userId, @Param("status") ConnectionStatus status,
                                           Pageable pageable);

    @Query("SELECT c FROM Connection c WHERE c.following.id = :userId AND c.status = 'PENDING'")
    Page<Connection> findPendingRequestsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c FROM Connection c WHERE c.follower.id = :userId AND c.status = 'PENDING'")
    Page<Connection> findSentPendingRequestsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c FROM Connection c WHERE c.following.id = :userId AND (c.status = 'ACCEPTED' OR c.status = 'REJECTED') ORDER BY c.updatedAt DESC")
    Page<Connection> findPastRequestsByUserId(@Param("userId") Long userId, Pageable pageable);

    long countByFollowingIdAndStatus(Long userId, ConnectionStatus status);

    long countByFollowerIdAndStatus(Long userId, ConnectionStatus status);

    @Query("SELECT c.following.id FROM Connection c WHERE c.follower.id = :userId AND c.status = 'ACCEPTED'")
    List<Long> findFollowingUserIds(@Param("userId") Long userId);

    @Query("SELECT c.follower.id FROM Connection c WHERE c.following.id = :userId AND c.status = 'ACCEPTED'")
    List<Long> findFollowerUserIds(@Param("userId") Long userId);

    @Query("SELECT c FROM Connection c WHERE ((c.follower.id = :userId AND c.following.id = :otherUserId) OR " +
            "(c.follower.id = :otherUserId AND c.following.id = :userId))")
    List<Connection> findBetweenUsers(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);
}
