package com.hrachovcompressionservice.microserviceforvideocompression.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Bean;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class NotificationDTO {
    private String username; // кому адресовано

    private String message; // текст уведомления

    private String link; // куда ведёт уведомление (например, /film/123)

    private boolean read;

    private LocalDateTime createdAt;

    private NotificationType type; // LIKE, COMMENT, NEWS и т.д.
}
