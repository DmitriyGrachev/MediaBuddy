package org.hrachov.com.filmproject.repository.jpa;

import org.hrachov.com.filmproject.model.Comment;
import org.hrachov.com.filmproject.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query(value = "SELECT * FROM comments WHERE film_id = ?1" , nativeQuery = true)
    List<Comment> findAllByMovieId(long movieId);

    Page<Comment> findAllByFilm_IdAndParentCommentIsNull(long id, Pageable pageable);
    //parent_comment_id

    Comment getCommentById(Long id);

    Page<Comment> findAllCommentsByUser(User user, Pageable pageable);
    // Fetch only top-level comments for a specific film
    @Query("SELECT c FROM Comment c WHERE c.film.id = :filmId AND c.parentComment IS NULL")
    Page<Comment> findTopLevelCommentsByFilmId(@Param("filmId") Long filmId, Pageable pageable);
}
