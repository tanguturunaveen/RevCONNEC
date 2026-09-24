package org.revature.revconnect.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.revature.revconnect.enums.Privacy;
import org.revature.revconnect.enums.UserType;
import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.model.Message;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.MessageRepository;
import org.revature.revconnect.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthService authService;

    @InjectMocks
    private MessageService messageService;

    @Test
    void sendMessage_receiverMissing_throwsNotFound() {
        User me = user(1L, "me");
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> messageService.sendMessage(2L, "hello", null));
    }

    @Test
    void sendMessage_success_savesMessage() {
        User me = user(1L, "me");
        User other = user(2L, "other");
        Message saved = message(10L, me, other, "hello");
        saved.setMediaUrl("m.jpg");

        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));
        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        Message result = messageService.sendMessage(2L, "hello", "m.jpg");

        assertEquals(10L, result.getId());
        assertEquals("hello", result.getContent());
        assertEquals("m.jpg", result.getMediaUrl());
    }

    @Test
    void getConversation_otherUserMissing_throwsNotFound() {
        User me = user(1L, "me");
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> messageService.getConversation(2L, PageRequest.of(0, 10)));
    }

    @Test
    void getConversation_success_returnsPage() {
        User me = user(1L, "me");
        User other = user(2L, "other");
        Message m = message(20L, me, other, "hi");
        Page<Message> page = new PageImpl<>(List.of(m), PageRequest.of(0, 5), 1);

        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));
        when(messageRepository.findConversation(me, other, PageRequest.of(0, 5))).thenReturn(page);

        Page<Message> result = messageService.getConversation(2L, PageRequest.of(0, 5));

        assertEquals(1, result.getContent().size());
        assertEquals(20L, result.getContent().get(0).getId());
    }

    @Test
    void getConversationPartners_returnsList() {
        User me = user(1L, "me");
        User other = user(2L, "other");
        when(authService.getCurrentUser()).thenReturn(me);
        when(messageRepository.findConversationPartners(me)).thenReturn(List.of(other));

        List<User> result = messageService.getConversationPartners();

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
    }

    @Test
    void markAsRead_messageMissing_throwsNotFound() {
        when(messageRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> messageService.markAsRead(1L));
    }

    @Test
    void markAsRead_byReceiver_marksTrueAndSaves() {
        User sender = user(1L, "sender");
        User receiver = user(2L, "receiver");
        Message m = message(3L, sender, receiver, "x");
        m.setRead(false);
        when(messageRepository.findById(3L)).thenReturn(Optional.of(m));
        when(authService.getCurrentUser()).thenReturn(receiver);

        messageService.markAsRead(3L);

        assertTrue(m.isRead());
        verify(messageRepository).save(m);
    }

    @Test
    void markAsRead_notReceiver_doesNothing() {
        User sender = user(1L, "sender");
        User receiver = user(2L, "receiver");
        User other = user(3L, "other");
        Message m = message(3L, sender, receiver, "x");
        m.setRead(false);
        when(messageRepository.findById(3L)).thenReturn(Optional.of(m));
        when(authService.getCurrentUser()).thenReturn(other);

        messageService.markAsRead(3L);

        assertFalse(m.isRead());
        verify(messageRepository, never()).save(m);
    }

    @Test
    void markAllAsRead_senderMissing_throwsNotFound() {
        User me = user(2L, "me");
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> messageService.markAllAsRead(1L));
    }

    @Test
    void markAllAsRead_marksOnlyMatchingSenderMessages() {
        User sender1 = user(1L, "sender1");
        User sender2 = user(3L, "sender2");
        User me = user(2L, "me");
        Message m1 = message(11L, sender1, me, "a");
        Message m2 = message(12L, sender2, me, "b");
        m1.setRead(false);
        m2.setRead(false);

        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sender1));
        when(messageRepository.findByReceiverAndIsReadFalse(me)).thenReturn(List.of(m1, m2));

        messageService.markAllAsRead(1L);

        assertTrue(m1.isRead());
        assertFalse(m2.isRead());
        verify(messageRepository).saveAll(List.of(m1, m2));
    }

    @Test
    void getUnreadCount_returnsRepositoryCount() {
        User me = user(2L, "me");
        when(authService.getCurrentUser()).thenReturn(me);
        when(messageRepository.countByReceiverAndIsReadFalse(me)).thenReturn(9L);

        assertEquals(9L, messageService.getUnreadCount());
    }

    @Test
    void searchMessages_returnsPage() {
        User me = user(1L, "me");
        Message m = message(1L, me, user(2L, "other"), "hello");
        Page<Message> page = new PageImpl<>(List.of(m), PageRequest.of(0, 5), 1);
        when(authService.getCurrentUser()).thenReturn(me);
        when(messageRepository.searchMessages(me, "hell", PageRequest.of(0, 5))).thenReturn(page);

        Page<Message> result = messageService.searchMessages("hell", PageRequest.of(0, 5));

        assertEquals(1, result.getContent().size());
    }

    @Test
    void editMessage_missing_throwsNotFound() {
        when(messageRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> messageService.editMessage(1L, "new"));
    }

    @Test
    void editMessage_notSender_throwsUnauthorized() {
        User sender = user(1L, "sender");
        User current = user(2L, "me");
        Message m = message(1L, sender, current, "old");
        when(messageRepository.findById(1L)).thenReturn(Optional.of(m));
        when(authService.getCurrentUser()).thenReturn(current);

        assertThrows(UnauthorizedException.class, () -> messageService.editMessage(1L, "new"));
    }

    @Test
    void editMessage_sender_successUpdatesContent() {
        User sender = user(1L, "sender");
        User receiver = user(2L, "rec");
        Message m = message(1L, sender, receiver, "old");
        when(messageRepository.findById(1L)).thenReturn(Optional.of(m));
        when(authService.getCurrentUser()).thenReturn(sender);
        when(messageRepository.save(m)).thenReturn(m);

        Message result = messageService.editMessage(1L, "new");

        assertEquals("new", result.getContent());
    }

    @Test
    void deleteMessage_sender_softDeletes() {
        User sender = user(1L, "sender");
        User receiver = user(2L, "rec");
        Message m = message(1L, sender, receiver, "old");
        m.setDeleted(false);
        when(messageRepository.findById(1L)).thenReturn(Optional.of(m));
        when(authService.getCurrentUser()).thenReturn(sender);

        messageService.deleteMessage(1L);

        assertTrue(m.isDeleted());
        verify(messageRepository).save(m);
    }

    @Test
    void deleteMessage_notSender_doesNothing() {
        User sender = user(1L, "sender");
        User receiver = user(2L, "rec");
        User other = user(3L, "other");
        Message m = message(1L, sender, receiver, "old");
        when(messageRepository.findById(1L)).thenReturn(Optional.of(m));
        when(authService.getCurrentUser()).thenReturn(other);

        messageService.deleteMessage(1L);

        assertFalse(m.isDeleted());
        verify(messageRepository, never()).save(m);
    }

    @Test
    void deleteConversationWithUser_userMissing_throwsNotFound() {
        User me = user(1L, "me");
        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> messageService.deleteConversationWithUser(2L));
    }

    @Test
    void deleteConversationWithUser_softDeletesOnlyCurrentUserSentMessages() {
        User me = user(1L, "me");
        User other = user(2L, "other");
        Message sentByMe = message(1L, me, other, "a");
        Message sentByOther = message(2L, other, me, "b");
        Page<Message> page = new PageImpl<>(List.of(sentByMe, sentByOther), Pageable.unpaged(), 2);

        when(authService.getCurrentUser()).thenReturn(me);
        when(userRepository.findById(2L)).thenReturn(Optional.of(other));
        when(messageRepository.findConversation(me, other, Pageable.unpaged())).thenReturn(page);

        messageService.deleteConversationWithUser(2L);

        assertTrue(sentByMe.isDeleted());
        assertFalse(sentByOther.isDeleted());
        verify(messageRepository).saveAll(List.of(sentByMe, sentByOther));
    }

    private User user(Long id, String username) {
        return User.builder()
                .id(id)
                .username(username)
                .email(username + "@test.com")
                .name(username)
                .password("pwd")
                .privacy(Privacy.PUBLIC)
                .userType(UserType.PERSONAL)
                .build();
    }

    private Message message(Long id, User sender, User receiver, String content) {
        return Message.builder()
                .id(id)
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .build();
    }
}