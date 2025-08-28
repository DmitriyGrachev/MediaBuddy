package org.hrachov.com.filmproject.model;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Document("passwordResetTokens")
public class PasswordResetToken {
    @Id
    private String id;
    private String token;

    private long userId;

    @Field(value = "expiryDate", targetType = FieldType.DATE_TIME)
    @Indexed(name = "ttl-expire-passreset", expireAfterSeconds = 120)
    private Instant expiryDate;

    public boolean isExpired() {
        return expiryDate.isBefore(Instant.now());
    }
}
