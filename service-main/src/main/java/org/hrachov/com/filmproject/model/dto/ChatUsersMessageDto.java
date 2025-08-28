package org.hrachov.com.filmproject.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatUsersMessageDto {
    private Long id;
    private Long chatRoomId;
    private String senderUsername;
    private String message;
    private String dateTime;
}
