package org.revature.revconnect.repository;

import org.revature.revconnect.model.Message;
import org.revature.revconnect.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

        @Query("SELECT m FROM Message m WHERE " +
                        "(m.sender = :user1 AND m.receiver = :user2) OR " +
                        "(m.sender = :user2 AND m.receiver = :user1) " +
                        "ORDER BY m.timestamp DESC")
        Page<Message> findConversation(@Param("user1") User user1, @Param("user2") User user2, Pageable pageable);

        @Query("SELECT DISTINCT u FROM User u WHERE EXISTS (" +
                        "SELECT 1 FROM Message m WHERE (m.sender = u AND m.receiver = :user) OR (m.receiver = u AND m.sender = :user))")
        List<User> findConversationPartners(@Param("user") User user);

        List<Message> findByReceiverAndIsReadFalse(User receiver);

        long countByReceiverAndIsReadFalse(User receiver);

        long countBySenderAndReceiverAndIsReadFalse(User sender, User receiver);

        @Query("SELECT m FROM Message m WHERE (m.sender = :user OR m.receiver = :user) AND " +
                        "LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY m.timestamp DESC")
        Page<Message> searchMessages(@Param("user") User user, @Param("query") String query, Pageable pageable);
}
