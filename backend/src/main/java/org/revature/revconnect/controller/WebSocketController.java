package org.revature.revconnect.controller;

import org.revature.revconnect.model.Message;
import org.revature.revconnect.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload Map<String, Object> payload) {
        Long receiverId = Long.valueOf(payload.get("receiverId").toString());
        String content = (String) payload.get("content");
        String mediaUrl = payload.containsKey("mediaUrl") ? (String) payload.get("mediaUrl") : null;

        log.info("WebSocket message received for user: {}", receiverId);

        Message saved = messageService.sendMessage(receiverId, content, mediaUrl);

        messagingTemplate.convertAndSend(
                "/topic/messages/" + receiverId,
                Map.of(
                        "messageId", saved.getId(),
                        "senderId", saved.getSender().getId(),
                        "senderUsername", saved.getSender().getUsername(),
                        "content", saved.getContent(),
                        "timestamp", saved.getTimestamp().toString()));

        log.info("Message pushed to /topic/messages/{}", receiverId);
    }
    @MessageMapping("/chat.read")
    public void markAsRead(@Payload Map<String, Object> payload) {
        Long messageId = Long.valueOf(payload.get("messageId").toString());
        messageService.markAsRead(messageId);
        log.info("Message {} marked as read via WebSocket", messageId);
    }
}