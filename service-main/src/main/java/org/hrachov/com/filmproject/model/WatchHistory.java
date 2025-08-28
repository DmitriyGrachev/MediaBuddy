package org.hrachov.com.filmproject.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class WatchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Film film;

    private LocalDateTime date;

    private double lastPositionInSeconds; // Новое поле для хранения времени остановки

}
