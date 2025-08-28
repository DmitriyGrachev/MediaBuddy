package com.hrachovcompressionservice.microserviceforvideocompression.model.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RabbitNotificationDTO {
    private String url;
    private LocalDateTime createdAt;
}
