package org.hrachov.com.filmproject.model.notification;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class NotificationDTO {
    private String username; // кому адресовано

    private String message; // текст уведомления

    private String link; // куда ведёт уведомление (например, /film/123)

    private boolean read;

    private LocalDateTime createdAt;

    private NotificationType type; // LIKE, COMMENT, NEWS и т.д.
}
