package org.hrachov.com.filmproject.model.notification;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.LocalDateTime;

@Document(collection = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    private String id;
    private Long userId; // кому адресовано
    private String message; // текст уведомления
    private String link; // куда ведёт уведомление (например, /film/123)
    private boolean read = false;

    @Field(value = "createdAt", targetType = FieldType.DATE_TIME)
    @Indexed(name = "ttl-expire-notification", expireAfterSeconds = 120)
    private LocalDateTime createdAt;
    private String type; // LIKE, COMMENT, NEWS и т.д.
}
