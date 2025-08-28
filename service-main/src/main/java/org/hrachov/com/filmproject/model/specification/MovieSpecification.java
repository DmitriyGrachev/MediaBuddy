package org.hrachov.com.filmproject.model.specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.hrachov.com.filmproject.model.Genre;
import org.hrachov.com.filmproject.model.Movie;
import org.hrachov.com.filmproject.model.dto.SearchRequestDto;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;


public class MovieSpecification {
    public static Specification<Movie> withFilters(SearchRequestDto dto) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (dto.getTitle() != null && !dto.getTitle().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + dto.getTitle().toLowerCase() + "%"));
            }

            if (dto.getYear() != null) {
                predicates.add(cb.equal(root.get("releaseYear"), dto.getYear()));
            }

            if (dto.getGenre() != null && !dto.getGenre().isBlank()) {
                Join<Movie, Genre> genreJoin = root.join("genres", JoinType.INNER);
                predicates.add(cb.like(cb.lower(genreJoin.get("name")), "%" + dto.getGenre().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    public static Specification<Movie> byGenre(String genre) {
        return (root, query, cb) -> {
            if (genre == null || genre.isEmpty()) {
                System.out.println("Жанр не указан, возвращаем все фильмы");
                return cb.conjunction();
            }

            System.out.println("Фильтруем по жанру: " + genre);

            // Join with genres table to properly filter
            Join<Movie, Genre> genreJoin = root.join("genres", JoinType.INNER);
            return cb.equal(genreJoin.get("name"), genre);
        };
    }

    public static Specification<Movie> byPopularity() {
        return (root, query, cb) -> {
            query.orderBy(cb.desc(root.get("popularity")), cb.desc(root.get("rating")));
            return cb.conjunction();
        };
    }
}
