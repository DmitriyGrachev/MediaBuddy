package org.hrachov.com.filmproject.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hrachov.com.filmproject.model.*;
import org.hrachov.com.filmproject.model.dto.CommentDTO;
import org.hrachov.com.filmproject.model.dto.ReplyDTO;
import org.hrachov.com.filmproject.model.notification.Notification;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.service.CommentService;
import org.hrachov.com.filmproject.service.FilmService;
import org.hrachov.com.filmproject.service.MovieService;
import org.hrachov.com.filmproject.service.UserService;
import org.hrachov.com.filmproject.service.impl.NotificationsServiceMono;
import org.hrachov.com.filmproject.utils.HistoryEventSender;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    private final CommentService commentService;
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final NotificationWebSocketController notificationWebSocketController;
    private final NotificationsServiceMono notificationsServiceMono;
    private final FilmService filmService;
    private final HistoryEventSender historyEventSender;


    @PostMapping
    public ResponseEntity<Map<String, Object>> addComment (@RequestBody CommentDTO commentDTO) {

        // Validate comment text
        log.info("Adding comment {}", commentDTO.toString());
        if (commentDTO.getText() == null || commentDTO.getText().trim().isEmpty() || commentDTO.getText().equals("")) {
            log.info("Invalid comment text = " + commentDTO.getText());
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("reason", "Comment text cannot be empty"));
        }

        // Validate film exists
        Film film = filmService.getFilmById(commentDTO.getFilmId());
        if (film == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found");
        }

        // Get the authenticated user
        User user;
        try {
             user = userService.findByUsername(currentUserService.getCurrentUser().getUsername());
        }catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not logged in");
        }
        // Create and save the comment
        Comment comment = new Comment();
        comment.setText(commentDTO.getText());
        comment.setFilm(film);
        comment.setUser(user);

        Comment savedComment = commentService.addComment(comment);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Comment added");
        response.put("commentId", savedComment.getId());

        //Отправляем для рекомендаций TODO не забыть что нужно включать микросервис
        historyEventSender.sendWatchEvent(user.getId(), film.getId(),"comment",2.0);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/reply") // Ensure the path is distinct, e.g., "/reply" or "/{parentId}/reply"
    public ResponseEntity<Map<String, Object>> addReply (@RequestBody ReplyDTO replyDTO,
                                                         @RequestParam Long parentId) { // Use parentId for clarity
        if (replyDTO.getText() == null || replyDTO.getText().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Reply text cannot be empty"));
        }

        User user;
        try {
             user = userService.findByUsername(currentUserService.getCurrentUser().getUsername());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "User not logged in"));
        }

        // Film validation for reply is implicitly handled by checking if parentComment exists
        // and belongs to a film. The service method will set the film from the parent.

        Comment reply = new Comment();
        reply.setText(replyDTO.getText());
        // User and Film will be set by the service method.

        try {
            Comment savedReply = commentService.addReply(reply, parentId, user);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Reply added successfully");
            response.put("replyId", savedReply.getId());
            response.put("parentId", parentId);
            // Optionally, return the DTO of the saved reply
            // response.put("reply", commentService.mapCommentToFullDTO(savedReply)); // Needs access to mapper
            //Отправка для рекомендаций
            historyEventSender.sendWatchEvent(user.getId(), reply.getFilm().getId(),"comment",2.0);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", e.getReason()));
        }
    }
    @GetMapping
    public ResponseEntity<Page<CommentDTO>> getAllComments(@RequestParam(required = false) Long movieId,
            @RequestParam(required = true, defaultValue = "0") int page,
            @RequestParam(required = true, defaultValue = "5") int size,
            @RequestParam(required = true,defaultValue = "time,desc") String sort) {

        Film film = filmService.getFilmById(movieId);

        if (film == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found");
        }
        String[] parts = sort.split(",");

        Sort sortDir = Sort.by(Sort.Direction.fromString(parts[1]),parts[0]);

        Pageable pageable = PageRequest.of(page, size, sortDir);

        Page<Comment> commentPage = commentService.getCommentsByMovie(film, pageable);
        Page<CommentDTO> commentDTOPage = commentPage.map(commentService::mapCommentToFullDTO); // Используем метод из сервиса
        return ResponseEntity.ok(commentDTOPage);

    }
    public CommentDTO mapCommentToFullDTO(Comment comment) {
        if (comment == null) return null;
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        if (comment.getFilm() != null) { // Проверка на null
            dto.setFilmId(comment.getFilm().getId());
        }
        dto.setText(comment.getText());
        dto.setTime(comment.getTime());
        dto.setLikes(commentService.getLikesCount(comment)); // Предполагается, что эти методы существуют
        dto.setDislikes(commentService.getDislikesCount(comment));
        if (comment.getUser() != null) { // Проверка на null
            dto.setUsername(comment.getUser().getUsername());
        }
        if (comment.getParentComment() != null) { // Проверка на null
            dto.setParentId(comment.getParentComment().getId());
        }

        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            dto.setReplies(comment.getReplies().stream()
                    .map(this::mapCommentToFullDTO) // Рекурсивный вызов
                    .collect(Collectors.toList()));
        } else {
            dto.setReplies(Collections.emptyList());
        }
        return dto;
    }
    @GetMapping("/recently")
    public ResponseEntity<List<CommentDTO>> getRecentlyComments(){
        // Ensure "time" (or "date") is the correct field name in your Comment entity for sorting
        Pageable pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "time")); // Assuming 'time' is correct
        List<CommentDTO> recentComments = commentService.getRecentComments(pageable).stream()
                .map(comment -> {
                    CommentDTO dto = new CommentDTO();
                    dto.setId(comment.getId());
                    dto.setFilmId(comment.getFilm().getId());
                    dto.setText(comment.getText());
                    dto.setUsername(comment.getUser().getUsername());
                    dto.setTime(comment.getTime()); // Make sure to set the time
                    return dto;
                })
                // .limit(5) // This is redundant if Pageable already has size 5 and getRecentComments returns a Page or limited List
                .collect(Collectors.toList());
        return ResponseEntity.ok(recentComments);
    }
    @PostMapping("/{commentId}/reactions")
    public ResponseEntity<Map<String, String>> addReaction(
            @PathVariable Long commentId,
            @RequestBody Map<String, String> request
    ) {

        Comment comment = commentService.getCommentById(commentId);
        if (comment == null) {
            return ResponseEntity.notFound().build();
            //throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
        }
        Film film = filmService.getFilmById(comment.getFilm().getId());

        User user = userService.findByUsername(currentUserService.getCurrentUser().getUsername());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user");
        }
        String type = request.get("type");
        if (!type.equals("LIKE") && !type.equals("DISLIKE")) {
            return  ResponseEntity.badRequest().body(Map.of("message", "Invalid type"));
            //throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid reaction type");
        }

        Reaction.ReactionType reactionType = Reaction.ReactionType.valueOf(type);
        commentService.addReaction(user, comment, reactionType);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Reaction added");
        System.out.println("Юзер отправляет коментарий - " + user.getUsername());

        notificationWebSocketController.sendNotification(comment.getUser().getUsername(),"Reaction added");
        //TODO незабудь что не только movie но и serial + Заглушка пока что все уведомления READ = TRUE
        Notification notification = Notification.builder()
                .userId(comment.getUser().getId())
                .message("Reaction added")
                .read(true)
                .createdAt(null)
                .type(type)
                .build();

        if(film instanceof Movie){
            notification.setLink("/movie/" + comment.getFilm().getId());

        }else{
            notification.setLink("/serial/" + comment.getFilm().getId());
        }

        Notification notification1 = notificationsServiceMono.createNotification(notification);
        System.out.println("Sent notification = " + notification1.toString());
        //TODO заглушка под просмотр уведомления ставлю read - true для тестирования TTL

        return ResponseEntity.ok(response);
    }
}

