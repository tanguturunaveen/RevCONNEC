package org.revature.revconnect.repository;

import org.revature.revconnect.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE (u.isActive = true OR u.isActive IS NULL) AND u.isVerified = true AND " +
            "(LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchByUsernameOrName(@Param("query") String query, Pageable pageable);

    @Query("SELECT u FROM User u WHERE (u.isActive = true OR u.isActive IS NULL) AND u.isVerified = true AND u.privacy = org.revature.revconnect.enums.Privacy.PUBLIC AND " +
            "(LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<User> searchPublicUsers(@Param("query") String query, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.id != :currentUserId AND (u.isActive = true OR u.isActive IS NULL) AND u.isVerified = true AND u.privacy = org.revature.revconnect.enums.Privacy.PUBLIC " +
            "AND u.id NOT IN (SELECT c.following.id FROM Connection c WHERE c.follower.id = :currentUserId) " +
            "ORDER BY u.createdAt DESC")
    Page<User> findSuggestedUsers(@Param("currentUserId") Long currentUserId, Pageable pageable);

    @Query("SELECT DISTINCT f FROM Connection c1 " +
            "JOIN Connection c2 ON c1.following = c2.following " +
            "JOIN User f ON f.id = c1.following.id " +
            "WHERE c1.follower.id = :userId1 AND c2.follower.id = :userId2 " +
            "AND c1.status = org.revature.revconnect.enums.ConnectionStatus.ACCEPTED " +
            "AND c2.status = org.revature.revconnect.enums.ConnectionStatus.ACCEPTED")
    Page<User> findMutualConnections(@Param("userId1") Long userId1, @Param("userId2") Long userId2,
                                     Pageable pageable);

    @Query("SELECT u FROM User u WHERE (u.isActive = true OR u.isActive IS NULL) AND u.isVerified = true AND u.privacy = org.revature.revconnect.enums.Privacy.PUBLIC AND " +
            "(:query IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
            "(:location IS NULL OR LOWER(u.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
            "(:userType IS NULL OR u.userType = :userType) AND " +
            "(:verified IS NULL OR u.isVerified = :verified) " +
            "ORDER BY u.createdAt DESC")
    Page<User> advancedSearchPublicUsers(@Param("query") String query,
                                         @Param("location") String location,
                                         @Param("userType") org.revature.revconnect.enums.UserType userType,
                                         @Param("verified") Boolean verified,
                                         Pageable pageable);

    long countByUserType(org.revature.revconnect.enums.UserType userType);

    long countByIdInAndUserType(List<Long> ids, org.revature.revconnect.enums.UserType userType);

    @Query("SELECT u FROM User u WHERE u.isVerified = false OR u.isVerified IS NULL")
    List<User> findAllUnverifiedUsers();

    @Query("SELECT u FROM User u WHERE (u.isVerified = false OR u.isVerified IS NULL) AND u.createdAt < :cutoff")
    List<User> findUnverifiedUsersCreatedBefore(@Param("cutoff") java.time.LocalDateTime cutoff);
}
