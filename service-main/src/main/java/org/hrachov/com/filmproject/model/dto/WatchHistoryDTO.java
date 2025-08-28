package org.hrachov.com.filmproject.model.dto;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.N;
import org.hrachov.com.filmproject.model.Film;
import org.hrachov.com.filmproject.model.Movie;
import org.hrachov.com.filmproject.model.User;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchHistoryDTO{
    private Long id;
    private LocalDateTime date;
    private Long userId; // Добавляем для получения из микросервиса
    private Long filmId;
}
