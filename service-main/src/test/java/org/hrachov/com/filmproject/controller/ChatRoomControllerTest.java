package org.hrachov.com.filmproject.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hrachov.com.filmproject.model.Role;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.chat.ChatMessage;
import org.hrachov.com.filmproject.model.chat.ChatRoom;
import org.hrachov.com.filmproject.model.notification.WebSocketEventListener;
import org.hrachov.com.filmproject.repository.jpa.ChatMessageRepository;
import org.hrachov.com.filmproject.repository.jpa.ChatRoomRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.security.JwtUtils;
import org.hrachov.com.filmproject.security.UserDetailsImpl;
import org.hrachov.com.filmproject.websocket.ChatRoomController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = ChatRoomController.class)
@AutoConfigureMockMvc(addFilters = false)
public class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Моки зависимостей контроллера
    @MockBean
    private ChatRoomRepository chatRoomRepository;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private ChatMessageRepository chatMessageRepository;
    @MockBean
    private WebSocketEventListener webSocketEventListener;

    // Обязательные моки
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private CurrentUserService currentUserService;

    private User user1;
    private User user2;
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");
        user1.setFirstName("Alice");
        user1.setLastName("Smith");
        user1.setPassword("password");
        user1.setRoles(new HashSet<>(List.of(Role.ROLE_REGULAR)));

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");
        user2.setFirstName("Bob");
        user2.setLastName("Smith");
        user2.setPassword("password");
        user2.setRoles(new HashSet<>(List.of(Role.ROLE_REGULAR)));

        userDetails = new UserDetailsImpl(user1);
    }

    @Test
    void createChatRoom_shouldReturnNewRoomId_whenRoomDoesNotExist() throws Exception {
        // Arrange
        ChatRoom newRoom = new ChatRoom();
        newRoom.setId(100L);
        newRoom.setUser1(user1);
        newRoom.setUser2(user2);
        newRoom.setCreatedAt(LocalDateTime.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(chatRoomRepository.findChatRoomByUsers(1L, 2L)).thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(newRoom);

        // Act & Assert
        mockMvc.perform(post("/api/chat/create")
                        .param("user1Id", "1")
                        .param("user2Id", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("100"));
    }

    @Test
    void createChatRoom_shouldReturnExistingRoomId_whenRoomExists() throws Exception {
        // Arrange
        ChatRoom existingRoom = new ChatRoom();
        existingRoom.setId(50L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user2));
        when(chatRoomRepository.findChatRoomByUsers(1L, 2L)).thenReturn(Optional.of(existingRoom));

        // Act & Assert
        mockMvc.perform(post("/api/chat/create")
                        .param("user1Id", "1")
                        .param("user2Id", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("50"));
    }

    @Test
    void getAllChats_shouldReturnUserChats() throws Exception {
        // Arrange
        ChatRoom room = new ChatRoom(1L, user1, user2, LocalDateTime.now());
        when(currentUserService.getCurrentUser()).thenReturn(userDetails);
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user1));
        when(chatRoomRepository.findChatRoomsByUser(user1)).thenReturn(Optional.of(List.of(room)));

        // Act & Assert
        mockMvc.perform(get("/api/chat/getAllChats"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].user1.username").value("user1"))
                .andExpect(jsonPath("$[0].user2.username").value("user2"));
    }

    @Test
    void getChatMessages_shouldReturnMessageHistory() throws Exception {
        // Arrange
        Long chatRoomId = 1L;
        ChatMessage message = new ChatMessage(1L, null, user1, "Hello", LocalDateTime.now());
        when(chatMessageRepository.findAllByChatRoom_Id(chatRoomId)).thenReturn(Optional.of(List.of(message)));

        // Act & Assert
        mockMvc.perform(get("/api/chat/chat/{chatRoomId}", chatRoomId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].senderUsername").value("user1"))
                .andExpect(jsonPath("$[0].message").value("Hello"));
    }

    @Test
    void getOnlineStatusesBulk_shouldReturnStatusesMap() throws Exception {
        // Arrange
        List<String> usernames = List.of("user1", "user2", "user3");
        when(webSocketEventListener.isUserConnected("user1")).thenReturn(true);
        when(webSocketEventListener.isUserConnected("user2")).thenReturn(false);
        when(webSocketEventListener.isUserConnected("user3")).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/chat/onlineStatusesBulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(usernames)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user1").value(true))
                .andExpect(jsonPath("$.user2").value(false))
                .andExpect(jsonPath("$.user3").value(true));
    }
}