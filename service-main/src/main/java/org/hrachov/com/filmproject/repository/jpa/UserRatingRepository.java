package org.hrachov.com.filmproject.repository.jpa;

import org.hrachov.com.filmproject.model.Film;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.UserRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRatingRepository extends JpaRepository<UserRating, Long> {
    Optional<UserRating> findByUserIdAndFilmId(Long userId, Long filmId);
    Optional<UserRating> findByFilmId(Long filmId);

    Optional<UserRating> findByUserAndFilm(User user, Film film);
}
