package com.hrachovhistoryservice.microserviceforhistory.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "genres")
public class Genre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name;

    @JsonIgnore // This is critical to prevent JSON recursion
    @ManyToMany(mappedBy = "genres")
    private Set<Film> films = new HashSet<>();

    // Only include essential getters/setters to avoid serialization issues
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Exclude getFilms/setFilms from Lombok's @Data generation
    @JsonIgnore
    public Set<Film> getFilms() {
        return films;
    }

    // Helper methods should not modify collections during iteration
    public void addFilm(Film film) {
        if (film != null && !films.contains(film)) {
            films.add(film);
            if (!film.getGenres().contains(this)) {
                film.getGenres().add(this);
            }
        }
    }

    public void removeFilm(Film film) {
        if (film != null && films.contains(film)) {
            films.remove(film);
            if (film.getGenres().contains(this)) {
                film.getGenres().remove(this);
            }
        }
    }

    // Override equals and hashCode to only use id
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Genre genre = (Genre) o;
        return Objects.equals(id, genre.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}