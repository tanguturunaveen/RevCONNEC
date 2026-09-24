package org.revature.revconnect.service;

import org.revature.revconnect.exception.ResourceNotFoundException;
import org.revature.revconnect.exception.UnauthorizedException;
import org.revature.revconnect.model.Message;
import org.revature.revconnect.model.User;
import org.revature.revconnect.repository.MessageRepository;
import org.revature.revconnect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    @Transactional
    public Message sendMessage(Long receiverId, String content, String mediaUrl) {
        User sender = authService.getCurrentUser();
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", receiverId));

        log.info("User {} sending message to user {}", sender.getUsername(), receiver.getUsername());

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .mediaUrl(mediaUrl)
                .build();

        Message savedMessage = messageRepository.save(message);
        log.info("Message sent with ID: {}", savedMessage.getId());
        return savedMessage;
    }

    public Page<Message> getConversation(Long otherUserId, Pageable pageable) {
        User currentUser = authService.getCurrentUser();
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", otherUserId));

        log.info("Fetching conversation between {} and {}", currentUser.getUsername(), otherUser.getUsername());
        return messageRepository.findConversation(currentUser, otherUser, pageable);
    }

    public List<User> getConversationPartners() {
        User currentUser = authService.getCurrentUser();
        log.info("[TRACE] Fetching conversation partners for user: {}. ID: {}",
                currentUser.getUsername(), currentUser.getId());
        List<User> partners = messageRepository.findConversationPartners(currentUser);
        log.info("[TRACE] Found {} conversation partners for user {}", partners.size(), currentUser.getUsername());
        for (User p : partners) {
            log.info("[TRACE]   Partner: {} (ID: {})", p.getUsername(), p.getId());
        }
        return partners;
    }

    @Transactional
    public void markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        User currentUser = authService.getCurrentUser();
        if (message.getReceiver().getId().equals(currentUser.getId())) {
            message.setRead(true);
            messageRepository.save(message);
            log.info("Message {} marked as read", messageId);
        }
    }

    @Transactional
    public void markAllAsRead(Long senderId) {
        User currentUser = authService.getCurrentUser();
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));

        List<Message> unreadMessages = messageRepository.findByReceiverAndIsReadFalse(currentUser);
        unreadMessages.stream()
                .filter(m -> m.getSender().getId().equals(sender.getId()))
                .forEach(m -> m.setRead(true));

        messageRepository.saveAll(unreadMessages);
        log.info("All messages from {} marked as read", sender.getUsername());
    }

    public long getUnreadCount() {
        User currentUser = authService.getCurrentUser();
        return messageRepository.countByReceiverAndIsReadFalse(currentUser);
    }

    public long getUnreadCountWithPartner(User partner) {
        User currentUser = authService.getCurrentUser();
        return messageRepository.countBySenderAndReceiverAndIsReadFalse(partner, currentUser);
    }

    public Page<Message> searchMessages(String query, Pageable pageable) {
        User currentUser = authService.getCurrentUser();
        return messageRepository.searchMessages(currentUser, query, pageable);
    }

    @Transactional
    public Message editMessage(Long messageId, String content) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        User currentUser = authService.getCurrentUser();
        if (!message.getSender().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You can only edit your own messages");
        }

        message.setContent(content);
        return messageRepository.save(message);
    }

    @Transactional
    public void deleteMessage(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        User currentUser = authService.getCurrentUser();
        if (message.getSender().getId().equals(currentUser.getId())) {
            message.setDeleted(true);
            messageRepository.save(message);
            log.info("Message {} soft deleted", messageId);
        }
    }

    @Transactional
    public void deleteConversationWithUser(Long otherUserId) {
        User currentUser = authService.getCurrentUser();
        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", otherUserId));

        Page<Message> conversation = messageRepository.findConversation(currentUser, otherUser, Pageable.unpaged());
        conversation.getContent().stream()
                .filter(message -> message.getSender().getId().equals(currentUser.getId()))
                .forEach(message -> message.setDeleted(true));
        messageRepository.saveAll(conversation.getContent());
        log.info("Conversation messages soft deleted for user {}", currentUser.getUsername());
    }
}
