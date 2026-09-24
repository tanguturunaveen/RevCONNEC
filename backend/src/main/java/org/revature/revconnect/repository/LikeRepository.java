package org.revature.revconnect.repository;

import org.revature.revconnect.model.Like;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserIdAndPostId(Long userId, Long postId);

    boolean existsByUserIdAndPostId(Long userId, Long postId);

    Page<Like> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable pageable);

    long countByPostId(Long postId);

    void deleteByUserIdAndPostId(Long userId, Long postId);

    void deleteByPostId(Long postId);
}
