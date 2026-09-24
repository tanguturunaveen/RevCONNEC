package org.revature.revconnect.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.revature.revconnect.dto.request.MessageRequest;
import org.revature.revconnect.model.Message;
import org.revature.revconnect.model.User;
import org.revature.revconnect.service.MessageService;
import org.revature.revconnect.repository.UserRepository;
import org.revature.revconnect.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MessageService messageService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void getConversations_ShouldReturnOk() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        List<User> users = Collections.singletonList(user);
        when(messageService.getConversationPartners()).thenReturn(users);

        mockMvc.perform(get("/api/messages/conversations")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void createConversation_ShouldReturnCreated() throws Exception {
        User recipient = new User();
        recipient.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(recipient));

        mockMvc.perform(post("/api/messages/conversations")
                        .param("recipientId", "1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Conversation opened"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void getMessages_ShouldReturnOk() throws Exception {
        User sender = new User();
        sender.setId(1L);
        User receiver = new User();
        receiver.setId(2L);

        Message message = new Message();
        message.setId(1L);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent("test");
        message.setTimestamp(LocalDateTime.now());

        Page<Message> page = new PageImpl<>(Collections.singletonList(message));
        when(messageService.getConversation(anyLong(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/messages/conversations/{conversationId}", 1L)
                        .param("page", "0")
                        .param("size", "50")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void sendMessage_ShouldReturnCreated() throws Exception {
        Message message = new Message();
        message.setId(1L);
        when(messageService.sendMessage(anyLong(), anyString(), any())).thenReturn(message);

        MessageRequest request = MessageRequest.builder()
                .content("hello")
                .mediaUrl("http://example.com/image.jpg")
                .build();

        mockMvc.perform(post("/api/messages/conversations/{conversationId}", 1L)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Message sent"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void deleteConversation_ShouldReturnOk() throws Exception {
        doNothing().when(messageService).deleteConversationWithUser(anyLong());

        mockMvc.perform(delete("/api/messages/conversations/{conversationId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Conversation deleted"));
    }

    @Test
    void deleteMessage_ShouldReturnOk() throws Exception {
        doNothing().when(messageService).deleteMessage(anyLong());

        mockMvc.perform(delete("/api/messages/messages/{messageId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Message deleted"));
    }

    @Test
    void editMessage_ShouldReturnOk() throws Exception {
        when(messageService.editMessage(anyLong(), anyString())).thenReturn(new Message());

        MessageRequest request = MessageRequest.builder()
                .content("edited content")
                .build();

        mockMvc.perform(patch("/api/messages/messages/{messageId}", 1L)
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Message edited"));
    }

    @Test
    void markAsRead_ShouldReturnOk() throws Exception {
        doNothing().when(messageService).markAllAsRead(anyLong());

        mockMvc.perform(post("/api/messages/conversations/{conversationId}/read", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Marked as read"));
    }

    @Test
    void getUnreadCount_ShouldReturnOk() throws Exception {
        when(messageService.getUnreadCount()).thenReturn(5L);

        mockMvc.perform(get("/api/messages/unread/count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void reactToMessage_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/messages/messages/{messageId}/react", 1L)
                        .param("reaction", "like")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reaction added"));
    }

    @Test
    void removeReaction_ShouldReturnOk() throws Exception {
        mockMvc.perform(delete("/api/messages/messages/{messageId}/react", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reaction removed"));
    }

    @Test
    void muteConversation_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/messages/conversations/{conversationId}/mute", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Conversation muted"));
    }

    @Test
    void unmuteConversation_ShouldReturnOk() throws Exception {
        mockMvc.perform(delete("/api/messages/conversations/{conversationId}/mute", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Conversation unmuted"));
    }

    @Test
    void searchMessages_ShouldReturnOk() throws Exception {
        User sender = new User();
        sender.setId(1L);
        User receiver = new User();
        receiver.setId(2L);

        Message message = new Message();
        message.setId(1L);
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent("test");
        message.setTimestamp(LocalDateTime.now());

        Page<Message> page = new PageImpl<>(Collections.singletonList(message));
        when(messageService.searchMessages(anyString(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/messages/search")
                        .param("query", "test")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void sendAttachment_ShouldReturnCreated() throws Exception {
        Message message = new Message();
        message.setId(1L);
        when(messageService.sendMessage(anyLong(), anyString(), anyString())).thenReturn(message);

        mockMvc.perform(post("/api/messages/conversations/{conversationId}/attachment", 1L)
                        .param("attachmentUrl", "http://example.com/file.pdf")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Attachment sent"))
                .andExpect(jsonPath("$.data").exists());
    }
}
