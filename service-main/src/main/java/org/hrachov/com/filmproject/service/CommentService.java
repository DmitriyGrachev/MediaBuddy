package org.hrachov.com.filmproject.service;

import org.hrachov.com.filmproject.model.*;
import org.hrachov.com.filmproject.model.dto.CommentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CommentService {
    Comment addComment(Comment comment);
    List<Comment> getCommentsByMovie(long id);
    Page<Comment> getCommentsByMovie(Film movie, Pageable pageable);
    Page<Comment> getRecentComments(Pageable pageable);
    Comment getCommentById(long id);

    Reaction addReaction(User user, Comment comment, Reaction.ReactionType type);

    long getLikesCount(Comment comment);

    long getDislikesCount(Comment comment);
    Comment addReply(Comment reply, long parentCommentId, User currentUser);
    Page<CommentDTO> getCommentDTOsByFilm(Film film, Pageable pageable);
    CommentDTO mapCommentToFullDTO(Comment comment);

}
