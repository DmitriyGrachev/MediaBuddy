package org.hrachov.com.filmproject.model.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Getter
@Setter
public class RabbitNotificationDTO {
    private String url;
    private LocalDateTime createdAt;
}
