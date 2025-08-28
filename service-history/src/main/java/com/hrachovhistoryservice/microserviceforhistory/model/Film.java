package com.hrachovhistoryservice.microserviceforhistory.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

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