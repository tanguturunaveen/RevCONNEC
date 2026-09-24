package org.revature.revconnect.repository;

import org.revature.revconnect.model.Comment;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable pageable);

    Page<Comment> findByPostIdAndParentIsNullOrderByCreatedAtDesc(Long postId, Pageable pageable);

    long countByPostId(Long postId);

    void deleteByPostId(Long postId);

    java.util.List<Comment> findByPostId(Long postId);

    long countByParentId(Long parentId);

    Page<Comment> findByParentIdOrderByCreatedAtAsc(Long parentId, Pageable pageable);

    List<Comment> findByParentId(Long parentId);
}
