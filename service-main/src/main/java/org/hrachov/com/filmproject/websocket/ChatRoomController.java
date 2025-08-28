package org.hrachov.com.filmproject.websocket;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.chat.*;
import org.hrachov.com.filmproject.model.notification.WebSocketEventListener;
import org.hrachov.com.filmproject.repository.jpa.ChatMessageRepository;
import org.hrachov.com.filmproject.repository.jpa.ChatRoomRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@AllArgsConstructor
public class ChatRoomController {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ChatMessageRepository chatMessageRepository;
    private final WebSocketEventListener webSocketEventListener;


    @PostMapping("/create")
    public ResponseEntity<Long> createChatRoom(@RequestParam Long user1Id, @RequestParam Long user2Id) {
        // Проверяем, что пользователи существуют
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new IllegalArgumentException("User1 not found: " + user1Id));
        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new IllegalArgumentException("User2 not found: " + user2Id));

        // Проверяем, что комната ещё не существует
        Optional<ChatRoom> existingRoom = chatRoomRepository.findChatRoomByUsers(user1Id, user2Id);
        if (existingRoom.isPresent()) {
            return ResponseEntity.ok(existingRoom.get().getId());
        }

        // Создаём новую комнату
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setUser1(user1);
        chatRoom.setUser2(user2);
        chatRoom.setCreatedAt(LocalDateTime.now());
        chatRoom = chatRoomRepository.save(chatRoom);

        log.info("Created chat room: " + chatRoom.getId());

        return ResponseEntity.ok(chatRoom.getId());
    }
    @GetMapping("/getAllChats")
    public ResponseEntity<List<ChatRoomDto>> getAllChats() {

        String username = currentUserService.getCurrentUser().getUsername();
        User user = userRepository.findByUsername(username).orElseThrow(()-> new IllegalArgumentException("User not found: " + username));

        // Используем новый, правильный метод
        List<ChatRoom> chatRooms = chatRoomRepository.findChatRoomsByUser(user).orElse(new ArrayList<>());
        // --- Вот магия преобразования ---
        List<ChatRoomDto> chatRoomDtos = chatRooms.stream()
                .map(this::convertToDto) // Используем вспомогательный метод
                .collect(Collectors.toList());

        return ResponseEntity.ok(chatRoomDtos);
    }
    // Вспомогательный метод для конвертации Entity -> DTO
    private ChatRoomDto convertToDto(ChatRoom chatRoom) {
        UserSimpleDto user1Dto = new UserSimpleDto(
                chatRoom.getUser1().getId(),
                chatRoom.getUser1().getUsername(),
                chatRoom.getUser1().getFirstName(),
                chatRoom.getUser1().getLastName()
        );

        UserSimpleDto user2Dto = new UserSimpleDto(
                chatRoom.getUser2().getId(),
                chatRoom.getUser2().getUsername(),
                chatRoom.getUser2().getFirstName(),
                chatRoom.getUser2().getLastName()
        );

        return new ChatRoomDto(
                chatRoom.getId(),
                user1Dto,
                user2Dto,
                chatRoom.getCreatedAt()
        );
    }
    @GetMapping("/chat/{chatRoomId}")
    public ResponseEntity<List<ChatMessageDto>> getChatMessages(@PathVariable Long chatRoomId) {
        List<ChatMessage> chatMessageHistory = chatMessageRepository.findAllByChatRoom_Id(chatRoomId)
                .orElse(Collections.emptyList()); // Use .orElse to avoid Optional nesting

        // Manually map the entity list to a DTO list
        List<ChatMessageDto> chatMessageDtos = chatMessageHistory.stream()
                .map(message -> new ChatMessageDto(
                        message.getId(),
                        message.getMessage(),
                        message.getSender().getUsername(), // Safely access only what you need
                        message.getDateTime()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(chatMessageDtos);
    }
    @PostMapping("/onlineStatusesBulk")
    public ResponseEntity<Map<String, Boolean>> getOnlineStatusesBulk(@RequestBody List<String> usernames) {
        Map<String, Boolean> statuses = new HashMap<>();
        for (String username : usernames) {
            statuses.put(username, webSocketEventListener.isUserConnected(username));
        }
        return ResponseEntity.ok(statuses);
    }
}