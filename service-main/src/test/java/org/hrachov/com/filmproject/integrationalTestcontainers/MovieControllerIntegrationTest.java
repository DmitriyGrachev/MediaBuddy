package org.hrachov.com.filmproject.integrationalTestcontainers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hrachov.com.filmproject.model.Comment;
import org.hrachov.com.filmproject.model.Movie;
import org.hrachov.com.filmproject.model.Role;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.dto.MovieDTO;
import org.hrachov.com.filmproject.repository.jpa.CommentRepository;
import org.hrachov.com.filmproject.repository.jpa.MovieRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.security.JwtUtils;
import org.hrachov.com.filmproject.service.CommentService;
import org.hrachov.com.filmproject.service.MovieService;
import org.hrachov.com.filmproject.service.impl.SerialService;
import org.hrachov.com.filmproject.utils.HistoryEventSender;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;
import redis.clients.jedis.JedisPooled;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class MovieControllerIntegrationTest extends IntegrationTestBase{
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HistoryEventSender historyEventSender;

    @Autowired
    private MovieService movieService;

    @Autowired
    private SerialService serialService;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private JedisPooled jedis;

    @Autowired
    private  CommentService commentService;

    private User testUser;
    private Movie testMovie;
    private Comment comment;
    @Autowired
    private CommentRepository commentRepository;

    @BeforeEach
    void setUp() throws JsonProcessingException {

        userRepository.deleteAll();
        movieRepository.deleteAll();
        // 1. Создаем пользователя в базе данных, чтобы Security мог его найти
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setRoles(new HashSet<Role>(List.of(Role.ROLE_REGULAR)));
        testUser.setEmail("testuser@gmail.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRegistrationDate(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // 2. Создаем фильм для тестов
        testMovie = new Movie();
        testMovie.setTitle("Test Movie Title");
        testMovie.setPosterPath("/poster.jpg");
        testMovie = movieRepository.save(testMovie);

         comment = new Comment();
        comment.setFilm(testMovie);
        comment.setUser(testUser);
        comment.setText("test comment");
        commentRepository.save(comment);
    }
    @Test
    @DisplayName("Api Movie should return movie by id")
    void getMovieById_WithCache() throws JsonProcessingException {
        //Записываю кеш
        MovieDTO movieDTO = objectMapper.convertValue(testMovie, MovieDTO.class);
        String key = "movie:id:" + testMovie.getId();
        jedis.set(key,objectMapper.writeValueAsString(movieDTO));
        System.out.println("MovvieDTO =" + movieDTO);
        webTestClient.get()
                .uri("/api/movie/{id}", testMovie.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.title").isEqualTo(testMovie.getTitle())
                .jsonPath("$.comments.[0].text").isEqualTo(comment.getText());
    }
    @Test
    @DisplayName("Api movie get Should return movie not found exception")
    void getMovieById_WithoutCache() throws JsonProcessingException {

        webTestClient.get()
                .uri("/api/movie/{id}", 99L)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.error").isEqualTo("Movie not found in DB")
                .jsonPath("$.message").isEqualTo("Movie not found");
    }
    @Test
    @DisplayName("Api movie get Should return moviDto withoutcache")
    void getMoviebyId_WithoutCache() throws JsonProcessingException {

        webTestClient.get()
                .uri("/api/movie/{id}", testMovie.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.title").isEqualTo(testMovie.getTitle())
                .jsonPath("$.comments.[0].text").isEqualTo(comment.getText());

        String key = "movie:id:" + testMovie.getId();
        String newCache = jedis.get(key);
        assertTrue(newCache.contains(testMovie.getTitle()));

    }
}


