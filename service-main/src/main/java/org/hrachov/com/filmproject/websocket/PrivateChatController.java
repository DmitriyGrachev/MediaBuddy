package org.hrachov.com.filmproject.websocket;

import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.chat.ChatMessage;
import org.hrachov.com.filmproject.model.chat.ChatRoom;
import org.hrachov.com.filmproject.model.dto.ChatUsersMessageDto;
import org.hrachov.com.filmproject.model.notification.WebSocketEventListener;
import org.hrachov.com.filmproject.repository.jpa.ChatMessageRepository;
import org.hrachov.com.filmproject.repository.jpa.ChatRoomRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;

@RestController
@AllArgsConstructor
public class PrivateChatController {
    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketEventListener webSocketEventListener;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @MessageMapping("/private/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, @Payload String message, Principal principal) {

        String username = principal.getName();

        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        // 2. Находим комнату
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Chat room not found: " + roomId));

        // 3. Проверяем, что отправитель участвует в комнате
        if (!chatRoom.getUser1().getId().equals(sender.getId()) && !chatRoom.getUser2().getId().equals(sender.getId())) {
            throw new SecurityException("User " + username + " is not part of this chat room");
        }

        // 4. Сохраняем сообщение в базе
        ChatMessage savedMessage = new ChatMessage();
        savedMessage.setChatRoom(chatRoom);
        savedMessage.setSender(sender);
        savedMessage.setMessage(message);
        savedMessage.setDateTime(LocalDateTime.now());
        chatMessageRepository.save(savedMessage);

        // 5. пределяем получателя
        Long recipientId = chatRoom.getUser1().getId().equals(sender.getId())
                ? chatRoom.getUser2().getId()
                : chatRoom.getUser1().getId();

        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException("Recipient not found"));

        // 3. Создаем DTO для отправки клиентам
        ChatUsersMessageDto dto = new ChatUsersMessageDto(
                savedMessage.getId(),
                savedMessage.getChatRoom().getId(),
                sender.getUsername(),
                savedMessage.getMessage(),
                savedMessage.getDateTime().toString()
        );

        // 4. Определяем конечный адрес (без /user)
        String destination = "/queue/private." + roomId;

        // 5. Отправляем DTO обоим участникам чата
        messagingTemplate.convertAndSendToUser(recipient.getUsername(), destination, dto);
        messagingTemplate.convertAndSendToUser(sender.getUsername(), destination, dto);


    }
}
