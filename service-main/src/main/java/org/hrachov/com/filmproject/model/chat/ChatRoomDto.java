package org.hrachov.com.filmproject.model.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDto {
    private Long id;
    private UserSimpleDto user1;
    private UserSimpleDto user2;
    private LocalDateTime createdAt;
}
