package org.hrachov.com.filmproject.integrationalTestcontainers;

import lombok.extern.slf4j.Slf4j;
import org.hrachov.com.filmproject.model.*;
import org.hrachov.com.filmproject.repository.jpa.FilmRepository;
import org.hrachov.com.filmproject.repository.jpa.GenreRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.repository.mongo.FavoriteRepository;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.security.JwtUtils;
import org.hrachov.com.filmproject.security.UserDetailsImpl;
import org.hrachov.com.filmproject.service.FavoritesService;
import org.hrachov.com.filmproject.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@Slf4j
public class FavoritesControllerTest extends IntegrationTestBase{
    @Autowired
    private  FavoritesService favoritesService;
    @Autowired
    private  CurrentUserService currentUserService;
    @Autowired
    private  UserService userService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private FilmRepository filmRepository;
    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private WebTestClient webTestClient;


    private User testUser;
    private Movie testMovie;
    @Autowired
    private GenreRepository genreRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        filmRepository.deleteAll();
        favoriteRepository.deleteAll();
        genreRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setRoles(new HashSet<Role>(List.of(Role.ROLE_REGULAR)));
        testUser.setEmail("testuser@gmail.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRegistrationDate(LocalDateTime.now());
        testUser.setRoles(new HashSet<>(List.of(Role.ROLE_REGULAR)));
        testUser = userRepository.save(testUser);

        testMovie = new Movie();
        testMovie.setTitle("Film 1");
        testMovie.setDescription("Film 1");
        testMovie.setReleaseYear(2020);
        testMovie.setRating(9.8);

        Genre genre = new Genre("Action");
        genreRepository.save(genre);

        testMovie.setGenres(new HashSet<>(List.of(genre)));

        filmRepository.save(testMovie);

    }
    @Test
    @DisplayName("/add to favorites should succeed")
    void addToFavoritesShouldSucceed() {
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser);
        String token = jwtUtils.generateToken(userDetails);

        webTestClient.post()
                .uri(uriBuilder ->
                        uriBuilder.path("/api/favorites/add")
                                .queryParam("filmId", testMovie.getId())
                                .queryParam("type", "movie")
                                .build())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("success");

        String queueName = "history.queue";
        //rabbitTemplate.receive(queueName); - убрал
        Object receivedMessage = rabbitTemplate.receive(queueName, 2000);
        log.info("Received message: {}", receivedMessage);

        assertThat(receivedMessage).isNotNull();

        Favorites favorites = favoriteRepository.findByUserIdAndFilmId(testUser.getId(), testMovie.getId());
        assertNotNull(favorites);
        assertEquals(testMovie.getId(),favorites.getFilmId());
        assertEquals(testUser.getId(),favorites.getUserId());
    }
    @Test
    @DisplayName("CheckIfFavorite Should return true")
    void checkIfFavoriteShouldSucceed() {
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser);
        String token = jwtUtils.generateToken(userDetails);

        Favorites favorites = new Favorites();
        favorites.setUserId(testUser.getId());
        favorites.setFilmId(testMovie.getId());
        favorites.setType("movie");
        favoriteRepository.save(favorites);

        webTestClient.get()
                .uri(uriBuilder ->
                        uriBuilder.path("/api/favorites/check")
                                .queryParam("filmId",testMovie.getId())
                                .build())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("success")
                .jsonPath("$.isFavorite").isEqualTo("true");

    }
    @Test
    @DisplayName("Romove from favorites")
    void romoveFromFavorites() {
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser);
        String token = jwtUtils.generateToken(userDetails);
        Favorites favorites = new Favorites();
        favorites.setUserId(testUser.getId());
        favorites.setFilmId(testMovie.getId());
        favorites.setType("movie");
        favoriteRepository.save(favorites);

        webTestClient.delete().uri(uriBuilder ->
                uriBuilder.path("/api/favorites/remove")
                        .queryParam("filmId",testMovie.getId())
                        .build())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("success")
                .jsonPath("$.message").isEqualTo("Favorite removed successfully.");

    }
    @Test
    @DisplayName("Remove from favorites should return Error")
    void removeFromFavoritesShouldReturnError() {
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser);
        String token = jwtUtils.generateToken(userDetails);

        webTestClient.delete().uri(uriBuilder ->
                uriBuilder.path("/api/favorites/remove")
                        .queryParam("filmId",99)
                        .build())
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo("error")
        .jsonPath("$.message").isEqualTo("Could not find favorite with film id " + 99);
    }

}
