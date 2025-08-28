package org.hrachov.com.filmproject.service.impl;

import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.model.*;
import org.hrachov.com.filmproject.model.dto.CommentDTO;
import org.hrachov.com.filmproject.repository.jpa.CommentRepository;
import org.hrachov.com.filmproject.repository.jpa.ReactionRepository;
import org.hrachov.com.filmproject.service.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import redis.clients.jedis.JedisPooled;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class CommentServiceImpl implements CommentService {
    private final CommentRepository commentRepository;
    private final ReactionRepository reactionRepository;

    @Override
    public Comment addComment(Comment comment) {
        return commentRepository.save(comment);
    }

    @Override
    public List<Comment> getCommentsByMovie(long id){
        return commentRepository.findAllByMovieId(id);
    }
    @Override
    public Page<Comment> getCommentsByMovie(Film movie, Pageable pageable){
        return commentRepository.findAllByFilm_IdAndParentCommentIsNull(movie.getId(), pageable);
    }
    @Override
    public Comment getCommentById(long id) {
        return commentRepository.getCommentById(id);
    }
    @Override
    public Reaction addReaction(User user, Comment comment, Reaction.ReactionType type) {
        Optional<Reaction> existing = reactionRepository.findByUserAndComment(user, comment);
        if (existing.isPresent()) {
            Reaction reaction = existing.get();
            if (reaction.getType() == type) {
                reactionRepository.delete(reaction); // Удаляем, если тот же тип
                return null;
            } else {
                reaction.setType(type); // Меняем тип
                return reactionRepository.save(reaction);
            }
        } else {
            Reaction reaction = new Reaction();
            reaction.setUser(user);
            reaction.setComment(comment);
            reaction.setType(type);
            reaction.setCreatedAt(LocalDateTime.now());
            return reactionRepository.save(reaction);
        }
    }
    @Override
    public Page<Comment> getRecentComments(Pageable pageable){
        return commentRepository.findAll(pageable);
    }
    @Override
    public long getLikesCount(Comment comment) {
        return reactionRepository.countByCommentAndType(comment, Reaction.ReactionType.LIKE);
    }
    @Override
    public long getDislikesCount(Comment comment) {
        return reactionRepository.countByCommentAndType(comment, Reaction.ReactionType.DISLIKE);
    }

    @Transactional // Make sure this is from org.springframework.transaction.annotation.Transactional
    @Override
    public Comment addReply(Comment reply, long parentCommentId, User currentUser) {
        Comment parentComment = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent comment not found"));

        reply.setUser(currentUser);
        reply.setFilm(parentComment.getFilm()); // Reply belongs to the same film as parent
        reply.setParentComment(parentComment);
        // The text for reply is already set in the controller before calling this service method

        return commentRepository.save(reply);
    }

    // This method will fetch top-level comments and map them to DTOs, including replies
    @Transactional(readOnly = true)
    @Override
    public Page<CommentDTO> getCommentDTOsByFilm(Film film, Pageable pageable) {
        Page<Comment> commentPage = commentRepository.findTopLevelCommentsByFilmId(film.getId(), pageable);
        return commentPage.map(this::mapCommentToFullDTO);
    }

    // Recursive helper method to map Comment entity to CommentDTO, including replies
    public CommentDTO mapCommentToFullDTO(Comment comment) {
        if (comment == null) {
            return null;
        }
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setFilmId(comment.getFilm().getId());
        dto.setText(comment.getText());
        dto.setTime(comment.getTime());
        dto.setLikes(getLikesCount(comment));
        dto.setDislikes(getDislikesCount(comment));
        dto.setUsername(comment.getUser().getUsername());
        if(comment.getParentComment() != null) {
            dto.setParentId(comment.getParentComment().getId());
        }


        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            // Due to @Transactional(readOnly = true) on getCommentDTOsByFilm,
            // accessing comment.getReplies() here will trigger lazy loading if necessary.
            List<CommentDTO> replyDTOs = comment.getReplies().stream()
                    .sorted(Comparator.comparing(Comment::getTime)) // Sort replies by time
                    .map(this::mapCommentToFullDTO) // Recursive call
                    .collect(Collectors.toList());
            dto.setReplies(replyDTOs);
        } else {
            dto.setReplies(Collections.emptyList());
        }
        return dto;
    }
}
