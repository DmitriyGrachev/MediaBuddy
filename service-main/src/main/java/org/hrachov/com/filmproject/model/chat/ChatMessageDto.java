package org.hrachov.com.filmproject.model.chat;
// Create a new file: ChatMessageDto.java

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChatMessageDto {
    private Long id;
    private String message;
    private String senderUsername;
    private LocalDateTime dateTime;

    // Constructors, Getters, and Setters
    public ChatMessageDto(Long id, String message, String senderUsername, LocalDateTime dateTime) {
        this.id = id;
        this.message = message;
        this.senderUsername = senderUsername;
        this.dateTime = dateTime;
    }
    // Getters and Setters ...
}