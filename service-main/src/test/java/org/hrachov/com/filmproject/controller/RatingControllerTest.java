package org.hrachov.com.filmproject.controller;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hrachov.com.filmproject.model.Film;
import org.hrachov.com.filmproject.model.Role;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.UserRating;
import org.hrachov.com.filmproject.model.dto.RatingRequestDto;
import org.hrachov.com.filmproject.repository.jpa.FilmRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRatingRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.security.JwtUtils;
import org.hrachov.com.filmproject.security.UserDetailsImpl;
import org.hrachov.com.filmproject.service.UserRatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Указываем, что это тест для RatingController
@WebMvcTest(controllers = RatingController.class)
// Отключаем Spring Security фильтры для упрощения тестов
@AutoConfigureMockMvc(addFilters = false)
public class RatingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Мокаем все зависимости контроллера
    @MockBean
    private UserRatingService ratingService;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private UserRatingRepository userRatingRepository;
    @MockBean
    private FilmRepository filmRepository;

    // Обязательные моки для Spring Security
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private CurrentUserService currentUserService;

    private User testUser;
    private UserDetailsImpl userDetails;
    private Film testFilm;

    @BeforeEach
    void setUp() {
        // Создаем тестового пользователя
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setFirstName("testuser");
        testUser.setLastName("testuser");
        testUser.setRoles(new HashSet<>(List.of(Role.ROLE_REGULAR)));
        userDetails = new UserDetailsImpl(testUser);

        // Создаем тестовый фильм
        testFilm = new Film();
        testFilm.setId(10L);
        testFilm.setRating(8.5);
        testFilm.setUserRatingSum(850L);
        testFilm.setUserRatingCount(100L);
    }

    @Test
    void rateMovie_shouldReturnOk_whenUserIsAuthenticated() throws Exception {
        // Arrange
        Long filmId = 10L;
        RatingRequestDto requestDto = new RatingRequestDto();
        requestDto.setRating(9);

        // Настраиваем моки
        when(currentUserService.getCurrentUser()).thenReturn(userDetails);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        // doNothing() используется для методов, которые ничего не возвращают (void)
        doNothing().when(ratingService).rateMovie(testUser.getId(), filmId, requestDto.getRating());

        // Act & Assert
        mockMvc.perform(post("/api/rating/{filmId}", filmId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk());

        // Проверяем, что метод сервиса был вызван ровно один раз с правильными аргументами
        verify(ratingService, times(1)).rateMovie(1L, 10L, 9);
    }

    @Test
    void getRating_shouldReturnRatingDetails_forAuthenticatedUser() throws Exception {
        // Arrange
        Long filmId = 10L;
        UserRating userRating = new UserRating();
        userRating.setRating(8);
        userRating.setUser(testUser);
        userRating.setFilm(testFilm);

        // Настраиваем моки
        when(filmRepository.findById(filmId)).thenReturn(Optional.of(testFilm));
        when(currentUserService.getCurrentUser()).thenReturn(userDetails);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRatingRepository.findByUserIdAndFilmId(testUser.getId(), filmId))
                .thenReturn(Optional.of(userRating));

        // Act & Assert
        mockMvc.perform(get("/api/rating/{filmId}", filmId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.filmRating").value(8.5))
                .andExpect(jsonPath("$.votes").value(100L))
                .andExpect(jsonPath("$.userRating").value(8));
    }

    @Test
    void getRating_shouldReturnGeneralRating_forUnauthenticatedUser() throws Exception {
        // Arrange
        Long filmId = 10L;

        // Настраиваем моки
        when(filmRepository.findById(filmId)).thenReturn(Optional.of(testFilm));
        // Имитируем отсутствие аутентифицированного пользователя
        when(currentUserService.getCurrentUser()).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/rating/{filmId}", filmId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.filmRating").value(8.5))
                .andExpect(jsonPath("$.votes").value(100))
                // Убеждаемся, что персональный рейтинг отсутствует
                .andExpect(jsonPath("$.userRating").doesNotExist());
    }

    @Test
    void getRating_shouldThrowException_whenFilmNotFound() throws Exception {
        // Arrange
        Long filmId = 99L; // Несуществующий ID
        when(filmRepository.findById(filmId)).thenThrow(new NoSuchElementException("Film not found"));

        // Act & Assert
        mockMvc.perform(get("/api/rating/{filmId}", filmId))
                // В зависимости от @ControllerAdvice, может быть 404 или 500
                .andExpect(status().isNotFound());
    }
}