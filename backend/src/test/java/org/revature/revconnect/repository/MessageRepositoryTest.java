package org.revature.revconnect.repository;

import org.junit.jupiter.api.Test;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.model.Message;
import org.revature.revconnect.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class MessageRepositoryTest {

    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void findConversation_ReturnsMessagesBetweenTwoUsers() {
        User u1 = saveUser("u1", "u1@test.com");
        User u2 = saveUser("u2", "u2@test.com");

        saveMessage(u1, u2, "Hello");

        Page<Message> conversation = messageRepository.findConversation(u1, u2, PageRequest.of(0, 10));

        assertEquals(1, conversation.getTotalElements());
    }

    @Test
    void findByReceiverAndIsReadFalse_ReturnsUnreadMessages() {
        User u1 = saveUser("u1", "u1@test.com");
        User u2 = saveUser("u2", "u2@test.com");

        saveMessage(u1, u2, "unread");
        Message m2 = saveMessage(u1, u2, "read");
        m2.setRead(true);
        messageRepository.save(m2);

        List<Message> unread = messageRepository.findByReceiverAndIsReadFalse(u2);
        long count = messageRepository.countByReceiverAndIsReadFalse(u2);

        assertEquals(1, unread.size());
        assertEquals(1, count);
    }

    @Test
    void searchMessages_ReturnsMatchingContent() {
        User me = saveUser("me", "me@test.com");
        User u1 = saveUser("u1", "u1@test.com");

        saveMessage(me, u1, "This is important");
        saveMessage(u1, me, "I see it");

        Page<Message> results = messageRepository.searchMessages(me, "important", PageRequest.of(0, 10));

        assertEquals(1, results.getTotalElements());
    }

    private User saveUser(String username, String email) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password("pwd")
                .name(username)
                .privacy(Privacy.PUBLIC)
                .userType(UserType.PERSONAL)
                .build());
    }

    private Message saveMessage(User sender, User receiver, String content) {
        return messageRepository.save(Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .build());
    }
}
