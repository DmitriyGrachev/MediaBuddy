package org.hrachov.com.filmproject.service;
import org.hrachov.com.filmproject.model.Film;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.UserRating;
import org.hrachov.com.filmproject.repository.jpa.FilmRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRatingRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.service.impl.UserRatingServiceImpl;
import org.hrachov.com.filmproject.utils.HistoryEventSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRatingServiceImplTest {

    @Mock
    private UserRatingRepository userRatingRepository;

    @Mock
    private FilmRepository filmRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HistoryEventSender historyEventSender;

    @InjectMocks
    private UserRatingServiceImpl userRatingService;

    private User user;
    private Film film;
    private Long userId;
    private Long filmId;

    @BeforeEach
    void setUp() {
        userId = 1L;
        filmId = 100L;

        user = new User();
        user.setId(userId);

        film = new Film();
        film.setId(filmId);
        film.setUserRatingCount(5L);
        film.setUserRatingSum(40L);
    }

    @Test
    void rateMovie_whenNewRating_shouldCreateRating() {
        int newRatingValue = 8;

        when(filmRepository.findById(filmId)).thenReturn(Optional.of(film));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRatingRepository.findByUserAndFilm(user, film)).thenReturn(Optional.empty());

        userRatingService.rateMovie(userId, filmId, newRatingValue);

        verify(filmRepository).findById(filmId);
        verify(userRepository).findById(userId);
        verify(userRatingRepository).findByUserAndFilm(user, film);

        assertEquals(6, film.getUserRatingCount());
        assertEquals(48, film.getUserRatingSum());

        verify(userRatingRepository).save(any(UserRating.class));
        verify(filmRepository).save(film);
        verify(historyEventSender).sendWatchEvent(userId, filmId, "rating", (double) newRatingValue);
    }

    @Test
    void rateMovie_whenExistingRating_shouldUpdateRating() {
        int oldRatingValue = 5;
        int newRatingValue = 9;

        UserRating existingRating = new UserRating();
        existingRating.setUser(user);
        existingRating.setFilm(film);
        existingRating.setRating(oldRatingValue);

        when(filmRepository.findById(filmId)).thenReturn(Optional.of(film));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRatingRepository.findByUserAndFilm(user, film)).thenReturn(Optional.of(existingRating));

        userRatingService.rateMovie(userId, filmId, newRatingValue);

        verify(filmRepository).findById(filmId);
        verify(userRepository).findById(userId);
        verify(userRatingRepository).findByUserAndFilm(user, film);

        assertEquals(5, film.getUserRatingCount());
        long expectedSum = 40L - oldRatingValue + newRatingValue;
        assertEquals(expectedSum, film.getUserRatingSum());

        assertEquals(newRatingValue, existingRating.getRating());
        verify(userRatingRepository).save(existingRating);
        verify(filmRepository).save(film);
        verifyNoInteractions(historyEventSender);
    }

    @Test
    void rateMovie_whenRatingIsBelowMinimum_shouldThrowException() {
        int invalidRating = 0;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userRatingService.rateMovie(userId, filmId, invalidRating)
        );

        assertEquals("Рейтинг должен быть между 1 и 10.", exception.getMessage());
        verifyNoInteractions(filmRepository, userRepository, userRatingRepository, historyEventSender);
    }

    @Test
    void rateMovie_whenRatingIsAboveMaximum_shouldThrowException() {
        int invalidRating = 11;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userRatingService.rateMovie(userId, filmId, invalidRating)
        );

        assertEquals("Рейтинг должен быть между 1 и 10.", exception.getMessage());
        verifyNoInteractions(filmRepository, userRepository, userRatingRepository, historyEventSender);
    }

    @Test
    void rateMovie_whenFilmNotFound_shouldThrowException() {
        int newRatingValue = 7;
        when(filmRepository.findById(filmId)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> userRatingService.rateMovie(userId, filmId, newRatingValue)
        );

        assertEquals("Фильм с id=" + filmId + " не найден.", exception.getMessage());
        verify(filmRepository).findById(filmId);
        verifyNoInteractions(userRepository, userRatingRepository, historyEventSender);
    }

    @Test
    void rateMovie_whenUserNotFound_shouldThrowException() {
        int newRatingValue = 7;
        when(filmRepository.findById(filmId)).thenReturn(Optional.of(film));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> userRatingService.rateMovie(userId, filmId, newRatingValue)
        );

        assertEquals("Пользователь с id=" + userId + " не найден.", exception.getMessage());
        verify(filmRepository).findById(filmId);
        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(filmRepository);
        verifyNoInteractions(userRatingRepository, historyEventSender);
    }
}