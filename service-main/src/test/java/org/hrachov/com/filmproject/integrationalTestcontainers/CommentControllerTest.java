package org.hrachov.com.filmproject.integrationalTestcontainers;

import lombok.extern.slf4j.Slf4j;
import org.hrachov.com.filmproject.controller.NotificationWebSocketController;
import org.hrachov.com.filmproject.model.*;
import org.hrachov.com.filmproject.model.dto.CommentDTO;
import org.hrachov.com.filmproject.model.dto.ReplyDTO;
import org.hrachov.com.filmproject.model.dto.UserDTO;
import org.hrachov.com.filmproject.repository.jpa.*;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.security.JwtUtils;
import org.hrachov.com.filmproject.security.UserDetailsImpl;
import org.hrachov.com.filmproject.service.CommentService;
import org.hrachov.com.filmproject.service.FilmService;
import org.hrachov.com.filmproject.service.UserService;
import org.hrachov.com.filmproject.service.impl.NotificationsServiceMono;
import org.hrachov.com.filmproject.utils.HistoryEventSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class CommentControllerTest extends IntegrationTestBase {

    @Autowired
    private WebTestClient webClient;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CommentService commentService;
    @Autowired
    private FilmRepository filmRepository;
    @Autowired
    private JwtUtils jwtUtils;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private CommentRepository commentRepository;

    private User testUser;
    private Movie testMovie;
    private String token;
    @Autowired
    private ReactionRepository reactionRepository;

    @BeforeEach
    void setUp() {
        // Очистка репозиториев для изоляции тестов
        reactionRepository.deleteAll();
        commentRepository.deleteAll();
        userRepository.deleteAll();
        filmRepository.deleteAll();
        genreRepository.deleteAll();

        // Создание тестового пользователя
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password"); // В реальном проекте пароль должен быть захеширован
        testUser.setEmail("testuser@gmail.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRegistrationDate(LocalDateTime.now());
        testUser.setRoles(new HashSet<>(List.of(Role.ROLE_REGULAR)));
        testUser = userRepository.save(testUser);

        token = jwtUtils.generateToken(new UserDetailsImpl(testUser));
        // Создание тестового фильма
        Genre genre = genreRepository.save(new Genre("Action"));
        testMovie = new Movie();
        testMovie.setTitle("Film 1");
        testMovie.setDescription("Film 1 Description");
        testMovie.setReleaseYear(2020);
        testMovie.setGenres(new HashSet<>(List.of(genre)));
        filmRepository.save(testMovie);
    }

    @Test
    @DisplayName("POST /comments - 201 CREATED | Успешное добавление комментария с валидным JWT")
    void addComment_shouldSucceed() {
        //String jwt = jwtUtils.generateToken(new UserDetailsImpl(testUser));

        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setFilmId(testMovie.getId());
        commentDTO.setUsername(testUser.getUsername());
        commentDTO.setTime(LocalDateTime.now());
        commentDTO.setText("This is a valid comment.");

        EntityExchangeResult<Map> response = webClient.post()
                .uri("/api/comments")
                .header("Authorization", "Bearer " + token)
                .body(BodyInserters.fromValue(commentDTO))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult();

        Map<?, ?> data = response.getResponseBody();
        assertNotNull(data);
        assertEquals("Comment added", data.get("message"));

        Long commentId = Long.valueOf((Integer) data.get("commentId"));
        Comment comment = commentRepository.findById(commentId).orElseThrow();

        assertNotNull(comment);
        assertEquals(commentDTO.getText(), comment.getText());
        assertEquals(testUser.getUsername(), comment.getUser().getUsername());

        // Проверка отправки сообщения в RabbitMQ
        assertNotNull(rabbitTemplate.receive("history.queue", 1000));
    }

    @Test
    @DisplayName("POST /comments - 400 BAD_REQUEST | Текст комментария пустой")
    void addComment_shouldReturnBadRequest_whenTextIsEmpty() {
        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setFilmId(testMovie.getId());
        commentDTO.setUsername(testUser.getUsername());
        commentDTO.setText(""); // Пустой текст

        webClient.post()
                .uri("/api/comments")
                .header("Authorization", "Bearer " + token)
                // Заголовок Authorization не нужен, @WithUserDetails все делает за нас
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(commentDTO))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /comments - 401 UNAUTHORIZED | Невалидный токен")
    void addComment_shouldReturnUnauthorized_forInvalidToken() {
        String jwt = "FAKE_JWT_TOKEN";
        CommentDTO commentDTO = new CommentDTO();
        commentDTO.setFilmId(testMovie.getId());
        commentDTO.setText("some text");

        webClient.post()
                .uri("/api/comments")
                .header("Authorization", "Bearer " + jwt)
                .body(BodyInserters.fromValue(commentDTO))
                .exchange()
                .expectStatus().isUnauthorized(); // 💡 Ожидаем 401, а не 403
    }

    @Test
    @DisplayName("POST /comments/reply - 201 CREATED | Успешное добавление ответа на комментарий")
    @WithUserDetails("testuser")
    void addReplyToComment_shouldSucceed() {
        // Создаем родительский комментарий
        Comment parentComment = new Comment();
        parentComment.setText("This is a parent comment");
        parentComment.setUser(testUser);
        parentComment.setFilm(testMovie);
        parentComment.setTime(LocalDateTime.now());
        parentComment = commentRepository.save(parentComment);

        ReplyDTO replyDTO = new ReplyDTO();
        replyDTO.setText("This is a child comment");

        Comment finalParentComment = parentComment;
        EntityExchangeResult<Map> response = webClient.post()
                .uri(uriBuilder ->
                        uriBuilder.path("/api/comments/reply")
                                .queryParam("parentId", finalParentComment.getId())
                                .build()
                )
                .header("Authorization", "Bearer " + token)
                .body(BodyInserters.fromValue(replyDTO))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Map.class)
                .returnResult();

        Map<?, ?> data = response.getResponseBody();
        assertNotNull(data);
        assertEquals("Reply added successfully", data.get("message"));

        Long replyId = Long.valueOf((Integer) data.get("replyId"));
        Comment reply = commentRepository.findById(replyId).orElseThrow();

        assertNotNull(reply.getParentComment());
        assertEquals(parentComment.getId(), reply.getParentComment().getId());
    }

    @Test
    @DisplayName("POST /comments/reply - 400 BAD_REQUEST | Текст ответа пустой")
    @WithUserDetails("testuser")
    void addReply_shouldReturnBadRequest_whenTextIsNull() {
        Comment parentComment = new Comment();
        parentComment.setText("Parent comment");
        parentComment.setUser(testUser);
        parentComment.setFilm(testMovie);
        parentComment = commentRepository.save(parentComment);

        ReplyDTO replyDTO = new ReplyDTO(); // Текст не установлен (null)


        Comment finalParentComment = parentComment;
        webClient.post()
                .uri(uriBuilder ->
                        uriBuilder.path("/api/comments/reply")
                                .queryParam("parentId", finalParentComment.getId())
                                .build()
                )
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(replyDTO))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /comments/reply - 401 UNAUTHORIZED | Невалидный токен при добавлении ответа")
    void addReply_shouldReturnUnauthorized_forInvalidToken() {
        ReplyDTO replyDTO = new ReplyDTO();
        replyDTO.setText("This is a child comment");

        webClient.post()
                .uri(uriBuilder ->
                        uriBuilder.path("/api/comments/reply")
                                .queryParam("parentId", 1L) // ID не важен, запрос не дойдет до контроллера
                                .build()
                )
                .header("Authorization", "Bearer FAKE_TOKEN")
                .body(BodyInserters.fromValue(replyDTO))
                .exchange()
                .expectStatus().isUnauthorized(); // 💡 Ожидаем 401
    }


    @Test
    @DisplayName("GET /comments - 200 OK | Успешное получение комментариев для фильма")
    void getAllComments_shouldSucceed() {
        // Создание тестовых данных
        Comment comment1 = new Comment();
        comment1.setText("Comment 1");
        comment1.setUser(testUser);
        comment1.setFilm(testMovie);
        comment1.setTime(LocalDateTime.now().minusDays(1));
        commentRepository.save(comment1);

        Comment comment2 = new Comment();
        comment2.setText("Comment 2");
        comment2.setUser(testUser);
        comment2.setFilm(testMovie);
        comment2.setTime(LocalDateTime.now());
        commentRepository.save(comment2);

        // Вызов метода
        EntityExchangeResult<Map<String, Object>> response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/comments")
                        .queryParam("movieId", testMovie.getId())
                        .queryParam("page", 0)
                        .queryParam("size", 5)
                        .queryParam("sort", "time,desc")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult();

        // Проверка ответа
        Map<String, Object> page = response.getResponseBody();
        assertNotNull(page);
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertEquals(2, content.size());
        assertEquals("Comment 2", content.get(0).get("text"));
        assertEquals("Comment 1", content.get(1).get("text"));
    }

    @Test
    @DisplayName("GET /comments - 404 NOT_FOUND | Фильм не найден")
    void getAllComments_shouldReturnNotFound_whenMovieNotFound() {
        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/comments")
                        .queryParam("movieId", 999L)
                        .queryParam("page", 0)
                        .queryParam("size", 5)
                        .queryParam("sort", "time,desc")
                        .build())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("GET /comments - 200 OK | Нет комментариев для фильма")
    void getAllComments_shouldReturnEmptyPage_whenNoComments() {
        EntityExchangeResult<Map<String, Object>> response = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/api/comments")
                        .queryParam("movieId", testMovie.getId())
                        .queryParam("page", 0)
                        .queryParam("size", 5)
                        .queryParam("sort", "time,desc")
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, Object>>() {})
                .returnResult();

        Map<String, Object> page = response.getResponseBody();
        assertNotNull(page);
        List<Map<String, Object>> content = (List<Map<String, Object>>) page.get("content");
        assertEquals(0, content.size());
    }

    // Тесты для getRecentlyComments
    @Test
    @DisplayName("GET /comments/recently - 200 OK | Успешное получение последних 5 комментариев")
    void getRecentlyComments_shouldSucceed() {


        // Создание тестовых данных
        for (int i = 0; i < 7; i++) {
            Comment comment = new Comment();
            comment.setText("Comment " + i);
            comment.setUser(testUser);
            comment.setFilm(testMovie);
            comment.setTime(LocalDateTime.now().minusMinutes(7 - i));
            commentRepository.save(comment);
        }

        // Вызов метода
        EntityExchangeResult<List<Map<String, Object>>> response = webClient.get()
                .uri("/api/comments/recently")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .returnResult();

        // Проверка ответа
        List<Map<String, Object>> comments = response.getResponseBody();
        assertNotNull(comments);
        assertEquals(5, comments.size());
        assertEquals("Comment 6", comments.get(0).get("text"));
        assertEquals("Comment 2", comments.get(4).get("text"));
    }

    @Test
    @DisplayName("GET /comments/recently - 200 OK | Комментариев меньше 5")
    void getRecentlyComments_shouldReturnAll_whenLessThanFive() {
        // Создание 3 комментариев
        for (int i = 0; i < 3; i++) {
            Comment comment = new Comment();
            comment.setText("Comment " + i);
            comment.setUser(testUser);
            comment.setFilm(testMovie);
            comment.setTime(LocalDateTime.now().minusMinutes(3-i));
            commentRepository.save(comment);
        }

        // Вызов метода
        EntityExchangeResult<List<Map<String, Object>>> response = webClient.get()
                .uri("/api/comments/recently")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .returnResult();

        // Проверка ответа
        List<Map<String, Object>> comments = response.getResponseBody();
        assertNotNull(comments);
        assertEquals(3, comments.size());
        assertEquals("Comment 2", comments.get(0).get("text"));
        assertEquals("Comment 0", comments.get(2).get("text"));
    }

    @Test
    @DisplayName("GET /comments/recently - 200 OK | Нет комментариев")
    void getRecentlyComments_shouldReturnEmptyList_whenNoComments() {
        EntityExchangeResult<List<Map<String, Object>>> response = webClient.get()
                .uri("/api/comments/recently")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                .returnResult();

        List<Map<String, Object>> comments = response.getResponseBody();
        assertNotNull(comments);
        assertEquals(0, comments.size());
    }

    // Тесты для addReaction
    @Test
    @DisplayName("POST /comments/{commentId}/reactions - 200 OK | Успешное добавление реакции")
    void addReaction_shouldSucceed() {
        // Создание комментария
        Comment comment = new Comment();
        comment.setText("Test comment");
        comment.setUser(testUser);
        comment.setFilm(testMovie);
        comment.setTime(LocalDateTime.now());
        comment = commentRepository.save(comment);

        // Создание запроса
        Map<String, String> request = Map.of("type", "LIKE");

        // Вызов метода
        EntityExchangeResult<Map<String, String>> response = webClient.post()
                .uri("/api/comments/" + comment.getId() + "/reactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<Map<String, String>>() {})
                .returnResult();

        // Проверка ответа
        Map<String, String> data = response.getResponseBody();
        assertNotNull(data);
        assertEquals("Reaction added", data.get("message"));
    }

    @Test
    @DisplayName("POST /comments/{commentId}/reactions - 404 NOT_FOUND | Комментарий не найден")
    void addReaction_shouldReturnNotFound_whenCommentNotFound() {
        Map<String, String> request = Map.of("type", "LIKE");

        webClient.post()
                .uri("/api/comments/999/reactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("POST /comments/{commentId}/reactions - 401 UNAUTHORIZED | Пользователь не аутентифицирован")
    void addReaction_shouldReturnUnauthorized_whenNotAuthenticated() {
        Comment comment = new Comment();
        comment.setText("Test comment");
        comment.setUser(testUser);
        comment.setFilm(testMovie);
        comment.setTime(LocalDateTime.now());
        comment = commentRepository.save(comment);

        Map<String, String> request = Map.of("type", "LIKE");

        webClient.post()
                .uri("/api/comments/" + comment.getId() + "/reactions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("POST /comments/{commentId}/reactions - 400 BAD_REQUEST | Недопустимый тип реакции")
    void addReaction_shouldReturnBadRequest_whenInvalidReactionType() {
        Comment comment = new Comment();
        comment.setText("Test comment");
        comment.setUser(testUser);
        comment.setFilm(testMovie);
        comment.setTime(LocalDateTime.now());
        comment = commentRepository.save(comment);

        Map<String, String> request = Map.of("type", "INVALID");

        webClient.post()
                .uri("/api/comments/" + comment.getId() + "/reactions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(request))
                .exchange()
                .expectStatus().isBadRequest();
    }
}