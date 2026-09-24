package org.revature.revconnect.repository;

import org.revature.revconnect.model.Bookmark;
import org.revature.revconnect.model.Post;
import org.revature.revconnect.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Page<Bookmark> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Optional<Bookmark> findByUserAndPost(User user, Post post);

    boolean existsByUserAndPost(User user, Post post);

    void deleteByUserAndPost(User user, Post post);

    long countByPost(Post post);

    void deleteByPostId(Long postId);
}
