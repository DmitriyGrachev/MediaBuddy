package org.hrachov.com.filmproject.model;

import jakarta.persistence.*;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "user_ratings", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "movie_id"}) // Гарантирует, что пара пользователь-фильм уникальна
})
public class UserRating {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "film_id", nullable = false)
    private Film film;

    @Column(name = "rating", nullable = false)
    private Integer rating; // Оценка от 1 до 10
}
