package org.hrachov.com.filmproject.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hrachov.com.filmproject.model.Film;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.dto.RatingRequestDto;
import org.hrachov.com.filmproject.model.dto.UserRatingDto;
import org.hrachov.com.filmproject.repository.jpa.FilmRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRatingRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.service.UserRatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@Slf4j
@RestController
@RequestMapping("/api/rating") // Базовый путь для всех эндпоинтов, связанных с фильмами
@RequiredArgsConstructor // Lombok для инъекции зависимостей через конструктор
public class RatingController {
    private final UserRatingService ratingService;
    private final UserRepository userRepository; // Репозиторий для поиска ID пользователя по имени
    private final UserRatingRepository userRatingRepository;
    private final CurrentUserService currentUserService;
    private final FilmRepository filmRepository;

    @PostMapping("/{filmId}")
    public ResponseEntity<Void> rateMovie(
            @PathVariable Long filmId,
            @RequestBody @Valid RatingRequestDto request) { // Безопасное получение текущего пользователя
        // 1. Получаем ID пользователя из сессии
        // Никогда не передавайте userId в теле запроса!
        User currentUser = userRepository.findByUsername(currentUserService.getCurrentUser().getUsername()).orElseThrow(() -> new NoSuchElementException("User not found"));
        // 2. Вызываем логику сервиса

        ratingService.rateMovie(currentUser.getId(), filmId, request.getRating());
        // 3. Возвращаем успешный ответ без тела
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{filmId}")
    public ResponseEntity<UserRatingDto> getRating(@PathVariable Long filmId){
        try {
            Film film = filmRepository.findById(filmId)
                    .orElseThrow(() -> new NoSuchElementException("Film not found"));
            UserRatingDto userRatingDto = new UserRatingDto();
            userRatingDto.setVotes(film.getUserRatingCount());
            userRatingDto.setFilmRating(film.getUserRating());
            if (currentUserService.getCurrentUser() != null) {
                User user = userRepository.findByUsername(currentUserService.getCurrentUser().getUsername()).orElseThrow(() -> new NoSuchElementException("User not found"));
                log.info("User: {}", user);
                userRatingRepository.findByUserIdAndFilmId(
                        user.getId(),
                        filmId
                ).ifPresent(
                        userRating -> {
                            log.info("UserRating: {}", userRating);
                            userRatingDto.setUserRating(userRating.getRating());
                            log.info("UserRatingDto: {}", userRatingDto);
                        }
                );
            } else {
                log.info("No authenticated user found");
            }
            return ResponseEntity.ok(userRatingDto);
        }catch (NoSuchElementException e){

            return ResponseEntity.notFound().build();
        }
    }
}
