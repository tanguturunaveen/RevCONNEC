package org.revature.revconnect.repository;

import org.revature.revconnect.model.Comment;
import org.revature.revconnect.model.CommentLike;
import org.revature.revconnect.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByUserAndComment(User user, Comment comment);

    boolean existsByUserAndComment(User user, Comment comment);

    boolean existsByUserIdAndCommentId(Long userId, Long commentId);

    Page<CommentLike> findByComment(Comment comment, Pageable pageable);

    void deleteByUserAndComment(User user, Comment comment);

    void deleteByCommentId(Long commentId);
}
