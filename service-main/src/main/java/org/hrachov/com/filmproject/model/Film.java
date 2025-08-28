package org.hrachov.com.filmproject.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "films")
public class Film {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(name = "release_year")  // Explicit column name
    private Integer releaseYear;

    @Column(columnDefinition = "TEXT")
    private String description;
    private String director;
    private Double rating;
    private Double popularity;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "film_genres",
            joinColumns = @JoinColumn(name = "film_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private Set<Genre> genres = new HashSet<>(); // Initialize to an empty set

    // Сумма всех оценок от пользователей
    //New start
    @Column(name = "user_rating_sum")
    private Long userRatingSum = 0L;

    // Количество пользователей, поставивших оценку
    @Column(name = "user_rating_count")
    private Long userRatingCount = 0L;

    // Пользовательский рейтинг можно вычислять "на лету"
    @Transient // Это поле не будет сохранено в базу данных
    public Double getUserRating() {
        if (userRatingCount == null || userRatingCount == 0) {
            return 0.0;
        }
        // Округляем до одного знака после запятой
        return Math.round(((double) userRatingSum / userRatingCount) * 10.0) / 10.0;
    }
    //New end
    // Helper methods to maintain bidirectional consistency
    public void addGenre(Genre genre) {
        genres.add(genre);
        genre.getFilms().add(this);
    }

    public void removeGenre(Genre genre) {
        genres.remove(genre);
        genre.getFilms().remove(this);
    }
}