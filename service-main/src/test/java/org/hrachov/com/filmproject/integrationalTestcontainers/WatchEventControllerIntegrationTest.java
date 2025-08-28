package org.hrachov.com.filmproject.integrationalTestcontainers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.extern.slf4j.Slf4j;
import org.hrachov.com.filmproject.exception.MoviesNotFound;
import org.hrachov.com.filmproject.model.Movie;
import org.hrachov.com.filmproject.model.Role;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.dto.ProfileWatchHistoryDto;
import org.hrachov.com.filmproject.model.dto.WatchHistoryDTO;
import org.hrachov.com.filmproject.repository.jpa.MovieRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.security.JwtUtils;
import org.hrachov.com.filmproject.security.UserDetailsImpl;
import org.hrachov.com.filmproject.service.MovieService;
import org.hrachov.com.filmproject.service.impl.SerialService;
import org.hrachov.com.filmproject.utils.HistoryEventSender;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.test.web.reactive.server.WebTestClient;
import redis.clients.jedis.JedisPooled;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


@Slf4j
public class WatchEventControllerIntegrationTest extends IntegrationTestBase {

        @Autowired
        private WebTestClient webTestClient;

        static WireMockServer wireMockServer = new WireMockServer(8082);

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

        /*@MockBean
        private HistoryEventSender historyEventSender;

        @MockBean
        private MovieService movieService;

        @MockBean
        private SerialService serialService;

         */
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
        private  JedisPooled jedis;

    private User testUser;
        private Movie testMovie;
        @BeforeAll
        static void startServers() {
            wireMockServer.start();
            WireMock.configureFor("localhost", 8082);
        }
        @AfterAll
        static void stopServers() {
           wireMockServer.stop();
        }

        @AfterEach
        void resetWireMock() {
           wireMockServer.resetAll();
        }

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
        }


    @Test
    void watchFilm_shouldReturnOk_whenJwtIsValid() {
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser);
        String jwt = jwtUtils.generateToken(userDetails);
        String queueName = "history.queue";

        //rabbitTemplate.receive(queueName);

        // Act
        webTestClient.post() // Используем WebTestClient, он удобнее
                .uri("/api/watch/" + testMovie.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .exchange()
                // Assert - 1: Проверяем HTTP ответ
                .expectStatus().isOk();


        Object receivedMessage = rabbitTemplate.receiveAndConvert(queueName, 2000);
        assertThat(receivedMessage).isNotNull();
    }
    @Test
    void watchFilm_shouldReturnOk_whenJwtIsInvalid() {
        webTestClient.post()
                .uri("/api/watch/" + testMovie.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer I-AM-A-FAKE-TOKEN")
                .exchange()
                .expectStatus().isUnauthorized();
    }
    @Test
    void watchFilm_shouldReturnUnauthorized_whenNoJwt() {
        // Act
        webTestClient.post()
                .uri("/api/watch/" + testMovie.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }
    @Test
    void watchHistory_ShouldGetHistory() throws JsonProcessingException {
        //заодно проверб что контейнер возвращает фильм
        Movie movie = movieRepository.findById(testMovie.getId()).orElseThrow(()-> new MoviesNotFound("Movie not found"));
        System.out.println(movie);

        WatchHistoryDTO watchHistoryDTO = new WatchHistoryDTO();
        watchHistoryDTO.setId(movie.getId());
        watchHistoryDTO.setFilmId(testMovie.getId());
        watchHistoryDTO.setUserId(testUser.getId());
        watchHistoryDTO.setDate(LocalDateTime.now());


        String response = objectMapper.writeValueAsString(List.of(watchHistoryDTO));
        wireMockServer.stubFor(WireMock.get("/api/history/" + testUser.getId())
                .willReturn(WireMock.aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(response)));

        String jwt = jwtUtils.generateToken(new UserDetailsImpl(testUser));

        webTestClient.get()
                .uri("/api/watch/history")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .exchange()
                // Assert
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].title").isEqualTo("Test Movie Title");
        wireMockServer.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/api/history/" + testUser.getId())));
        }

        @Test
    @DisplayName("Api eatch history Should return no content")
    void watchHistory_shouldReturnNoContent_whenJwtIsInvalid() throws JsonProcessingException {
            String jwt = jwtUtils.generateToken(new UserDetailsImpl(testUser));
            String response = objectMapper.writeValueAsString(Collections.emptyList());

            wireMockServer.stubFor(WireMock.get("/api/history/" + testUser.getId())
                    .willReturn(WireMock.aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(response)));

            webTestClient.get()
                    .uri("/api/watch/history")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .exchange()
                    .expectStatus().isNoContent();

            wireMockServer.verify(1, WireMock.getRequestedFor(WireMock.urlEqualTo("/api/history/" + testUser.getId())));
        }

        @Test
        @DisplayName("Get reccomandations from cache")
    void getReccomandations_shouldReturnOk_whenJwtIsValid() {
            String response = null;
            String jwt = jwtUtils.generateToken(new UserDetailsImpl(testUser));

            try {
                String key = "user" + testUser.getId() + ":recommendations";

                ProfileWatchHistoryDto profileWatchHistoryDto = new ProfileWatchHistoryDto();
                profileWatchHistoryDto.setFilmId(testMovie.getId().toString());
                profileWatchHistoryDto.setDate(LocalDateTime.now());
                profileWatchHistoryDto.setType("movie");
                profileWatchHistoryDto.setPosterUrl("/poster.jpg");

                response = objectMapper.writeValueAsString(List.of(profileWatchHistoryDto));

                redisTemplate.opsForValue().set(key
                        ,response
                        , Duration.ofMinutes(10));

            }catch (Exception e) {
                log.error(e.getMessage());
            }

            webTestClient.get()
                    .uri("/api/watch/recommendations")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)

                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$[0].type").isEqualTo("movie")
                    .jsonPath("$[0].posterUrl").isEqualTo("/poster.jpg");

        }
        @Test
        @DisplayName("Should return user recommendations With empty cahce")
        void getReccomandations_shouldReturnOkWithEmptyCache_whenJwtIsValid() throws JsonProcessingException {
            String jwt = jwtUtils.generateToken(new UserDetailsImpl(testUser));
            String key = "user" + testUser.getId() + ":recommendations";

            WatchHistoryDTO watchHistoryDTO = new WatchHistoryDTO();
            watchHistoryDTO.setId(testMovie.getId());
            watchHistoryDTO.setFilmId(testMovie.getId());
            watchHistoryDTO.setUserId(testUser.getId());
            watchHistoryDTO.setDate(LocalDateTime.now());


            String response = objectMapper.writeValueAsString(List.of(watchHistoryDTO));

            wireMockServer.stubFor(WireMock.get("/api/recommendations/" + testUser.getId())
                    .willReturn(WireMock.aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(response)));

            webTestClient.get()
                    .uri("/api/watch/recommendations")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$[0].type").isEqualTo("movie")
                    .jsonPath("$[0].title").isEqualTo("Test Movie Title")
                    .jsonPath("$[0].filmId").isEqualTo(testMovie.getId().toString())
                    .jsonPath("$[0].posterUrl").isEqualTo("/poster.jpg");


            String cached = redisTemplate.opsForValue().get(key);
            assertNotNull(cached);
            assertTrue(cached.contains("Test Movie Title"));
        }

}
