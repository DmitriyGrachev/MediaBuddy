package org.hrachov.com.filmproject.repository.jpa;

import org.hrachov.com.filmproject.model.Film;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FilmRepository extends JpaRepository<Film, Long> {
    Page<Film> findAll(Specification<Film> spec, Pageable pageable);
    /*@Modifying
    @Query("UPDATE Movie m SET m.userRatingSum = m.userRatingSum + :difference WHERE m.id = :movieId")
    void updateRatingSum(@Param("movieId") Long movieId, @Param("difference") long difference);

    @Modifying
    @Query("UPDATE Movie m SET m.userRatingCount = m.userRatingCount + 1 WHERE m.id = :movieId")
    void incrementRatingCount(@Param("movieId") Long movieId);

     */
}
