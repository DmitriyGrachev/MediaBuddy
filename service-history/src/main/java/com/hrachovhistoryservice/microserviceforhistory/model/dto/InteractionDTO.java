package com.hrachovhistoryservice.microserviceforhistory.model.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
public class InteractionDTO {
    private String userId;
    private String filmId;
    private String interactionType;
    private Double value;
    private LocalDateTime timestamp;

    // Конструкторы, геттеры и сеттеры
}
