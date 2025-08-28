package com.hrachovhistoryservice.microserviceforhistory.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


import java.time.LocalDateTime;

@Entity
@Table(name = "interactions")
@Getter
@Setter
public class Interaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "movie_id", nullable = false)
    private String movieId;

    @Column(name = "interaction_type", nullable = false)
    private String interactionType;

    @Column(name = "value", nullable = false)
    private Double value;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public Interaction() {}

    public Interaction(String userId, String movieId, String interactionType, Double value, LocalDateTime timestamp) {
        this.userId = userId;
        this.movieId = movieId;
        this.interactionType = interactionType;
        this.value = value;
        this.timestamp = timestamp;
    }

    // Геттеры и сеттеры
}
