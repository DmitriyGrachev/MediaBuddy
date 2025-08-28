package org.hrachov.com.filmproject.service;

import org.hrachov.com.filmproject.model.Comment;
import org.hrachov.com.filmproject.model.Film;
import org.hrachov.com.filmproject.model.Reaction;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.dto.CommentDTO;
import org.hrachov.com.filmproject.repository.jpa.CommentRepository;
import org.hrachov.com.filmproject.repository.jpa.ReactionRepository;
import org.hrachov.com.filmproject.service.impl.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ReactionRepository reactionRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private User user;
    private Film film;
    private Comment comment;

    @BeforeEach
    void setUp() {
        // Инициализация общих тестовых данных
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        film = new Film();
        film.setId(100L);
        film.setTitle("Test Film");

        comment = new Comment();
        comment.setId(1L);
        comment.setText("This is a comment");
        comment.setUser(user);
        comment.setFilm(film);
        comment.setTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("addComment: должен сохранять и возвращать комментарий")
    void addComment_shouldSaveAndReturnComment() {
        // Arrange
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // Act
        Comment savedComment = commentService.addComment(comment);

        // Assert
        assertNotNull(savedComment);
        assertEquals("This is a comment", savedComment.getText());
        verify(commentRepository, times(1)).save(comment);
    }

    @Test
    @DisplayName("addReply: должен успешно добавлять ответ на комментарий")
    void addReply_shouldSuccessfullyAddReply() {
        // Arrange
        long parentCommentId = 1L;
        Comment parentComment = comment; // Используем comment как родительский

        Comment reply = new Comment();
        reply.setText("This is a reply");

        when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Comment savedReply = commentService.addReply(reply, parentCommentId, user);

        // Assert
        assertNotNull(savedReply);
        assertEquals(parentComment, savedReply.getParentComment());
        assertEquals(user, savedReply.getUser());
        assertEquals(film, savedReply.getFilm());
        assertEquals("This is a reply", savedReply.getText());
        verify(commentRepository, times(1)).save(reply);
    }

    @Test
    @DisplayName("addReply: должен выбрасывать исключение, если родительский комментарий не найден")
    void addReply_shouldThrowException_whenParentNotFound() {
        // Arrange
        long nonExistentParentId = 99L;
        Comment reply = new Comment();
        when(commentRepository.findById(nonExistentParentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            commentService.addReply(reply, nonExistentParentId, user);
        });

        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("addReaction: должен добавлять новую реакцию, если ее не было")
    void addReaction_shouldAddNewReaction() {
        // Arrange
        when(reactionRepository.findByUserAndComment(user, comment)).thenReturn(Optional.empty());
        when(reactionRepository.save(any(Reaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Reaction newReaction = commentService.addReaction(user, comment, Reaction.ReactionType.LIKE);

        // Assert
        assertNotNull(newReaction);
        assertEquals(Reaction.ReactionType.LIKE, newReaction.getType());
        verify(reactionRepository, times(1)).save(any(Reaction.class));
    }

    @Test
    @DisplayName("addReaction: должен удалять реакцию, если тип совпадает")
    void addReaction_shouldDeleteReaction_whenSameType() {
        // Arrange
        Reaction existingReaction = new Reaction();
        existingReaction.setUser(user);
        existingReaction.setComment(comment);
        existingReaction.setType(Reaction.ReactionType.LIKE);

        when(reactionRepository.findByUserAndComment(user, comment)).thenReturn(Optional.of(existingReaction));
        doNothing().when(reactionRepository).delete(existingReaction);

        // Act
        Reaction result = commentService.addReaction(user, comment, Reaction.ReactionType.LIKE);

        // Assert
        assertNull(result);
        verify(reactionRepository, times(1)).delete(existingReaction);
        verify(reactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("addReaction: должен изменять тип реакции, если он отличается")
    void addReaction_shouldChangeReactionType_whenDifferent() {
        // Arrange
        Reaction existingReaction = new Reaction();
        existingReaction.setUser(user);
        existingReaction.setComment(comment);
        existingReaction.setType(Reaction.ReactionType.LIKE);

        when(reactionRepository.findByUserAndComment(user, comment)).thenReturn(Optional.of(existingReaction));
        when(reactionRepository.save(any(Reaction.class))).thenReturn(existingReaction);

        // Act
        Reaction result = commentService.addReaction(user, comment, Reaction.ReactionType.DISLIKE);

        // Assert
        assertNotNull(result);
        assertEquals(Reaction.ReactionType.DISLIKE, result.getType());
        verify(reactionRepository, times(1)).save(existingReaction);
        verify(reactionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getLikesCount: должен возвращать корректное количество лайков")
    void getLikesCount_shouldReturnCorrectCount() {
        // Arrange
        when(reactionRepository.countByCommentAndType(comment, Reaction.ReactionType.LIKE)).thenReturn(15L);

        // Act
        long likesCount = commentService.getLikesCount(comment);

        // Assert
        assertEquals(15L, likesCount);
    }

    @Test
    @DisplayName("getCommentDTOsByFilm: должен корректно мапить комментарии и их ответы в DTO")
    void getCommentDTOsByFilm_shouldMapCommentsAndRepliesToDTO() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        // Создаем ответ на основной комментарий
        Comment reply = new Comment();
        reply.setId(2L);
        reply.setText("This is a reply");
        reply.setUser(user);
        reply.setFilm(film);
        reply.setParentComment(comment);
        reply.setTime(LocalDateTime.now().plusMinutes(1));

        comment.setReplies(List.of(reply));

        Page<Comment> commentPage = new PageImpl<>(List.of(comment), pageable, 1);

        when(commentRepository.findTopLevelCommentsByFilmId(film.getId(), pageable)).thenReturn(commentPage);
        // Мокаем подсчет лайков/дизлайков для обоих комментариев
        when(reactionRepository.countByCommentAndType(comment, Reaction.ReactionType.LIKE)).thenReturn(10L);
        when(reactionRepository.countByCommentAndType(comment, Reaction.ReactionType.DISLIKE)).thenReturn(2L);
        when(reactionRepository.countByCommentAndType(reply, Reaction.ReactionType.LIKE)).thenReturn(5L);
        when(reactionRepository.countByCommentAndType(reply, Reaction.ReactionType.DISLIKE)).thenReturn(0L);

        // Act
        Page<CommentDTO> dtoPage = commentService.getCommentDTOsByFilm(film, pageable);

        // Assert
        assertNotNull(dtoPage);
        assertEquals(1, dtoPage.getTotalElements());

        CommentDTO parentDTO = dtoPage.getContent().get(0);
        assertEquals(comment.getId(), parentDTO.getId());
        assertEquals(10L, parentDTO.getLikes());
        assertEquals(2L, parentDTO.getDislikes());
        assertFalse(parentDTO.getReplies().isEmpty());
        assertEquals(1, parentDTO.getReplies().size());

        CommentDTO replyDTO = parentDTO.getReplies().get(0);
        assertEquals(reply.getId(), replyDTO.getId());
        assertEquals(5L, replyDTO.getLikes());
        assertEquals(0L, replyDTO.getDislikes());
        assertEquals(comment.getId(), replyDTO.getParentId());
    }
}