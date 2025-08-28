package org.hrachov.com.filmproject.model.chat;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hrachov.com.filmproject.model.User;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chatroom_id")
    private ChatRoom chatRoom; // Переименовать для ясности

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender; // Переименовать для ясности

    private String message;

    @CreationTimestamp
    private LocalDateTime dateTime;
}
