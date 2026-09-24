package org.revature.revconnect.repository;

import org.revature.revconnect.model.PostViewer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostViewerRepository extends JpaRepository<PostViewer, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);
}
