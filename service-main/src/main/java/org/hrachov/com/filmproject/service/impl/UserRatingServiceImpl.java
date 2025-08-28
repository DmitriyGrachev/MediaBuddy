package org.hrachov.com.filmproject.service.impl;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.hrachov.com.filmproject.model.Film;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.UserRating;
import org.hrachov.com.filmproject.repository.jpa.FilmRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRatingRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.service.UserRatingService;
import org.hrachov.com.filmproject.utils.HistoryEventSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserRatingServiceImpl implements UserRatingService {
    private final UserRatingRepository userRatingRepository;
    private final FilmRepository filmRepository;
    private final UserRepository userRepository;
    // private final EntityManager entityManager; // Для native query
    private final HistoryEventSender historyEventSender;

    @Transactional
    public void rateMovie(Long userId, Long filmId,int newRatingValue) {
        if (newRatingValue < 1 || newRatingValue > 10) {
            throw new IllegalArgumentException("Рейтинг должен быть между 1 и 10.");
        }

        // Находим сущности, с которыми будем работать
        Film film = filmRepository.findById(filmId)
                .orElseThrow(() -> new NoSuchElementException("Фильм с id=" + filmId + " не найден."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Пользователь с id=" + userId + " не найден."));

        // Проверяем, ставил ли пользователь оценку ранее
        Optional<UserRating> existingRatingOpt = userRatingRepository.findByUserAndFilm(user, film);

        if (existingRatingOpt.isPresent()) {
            // Сценарий 1: Пользователь МЕНЯЕТ свою оценку
            UserRating existingRating = existingRatingOpt.get();
            int oldRatingValue = existingRating.getRating();

            // Обновляем общую сумму оценок в таблице films
            long newSum = film.getUserRatingSum() - oldRatingValue + newRatingValue;
            film.setUserRatingSum(newSum);

            // Обновляем саму оценку в таблице user_ratings
            existingRating.setRating(newRatingValue);
            userRatingRepository.save(existingRating); // Сохраняем измененную оценку

        } else {
            // Сценарий 2: Пользователь ГОЛОСУЕТ в первый раз
            // Увеличиваем счетчик голосов и общую сумму
            film.setUserRatingCount(film.getUserRatingCount() + 1);
            film.setUserRatingSum(film.getUserRatingSum() + newRatingValue);

            // Создаем новую запись в таблице user_ratings
            UserRating newRating = new UserRating();
            newRating.setUser(user);
            newRating.setFilm(film);
            newRating.setRating(newRatingValue);
            //TODO покачто только один раз при выставлении рейтинга
            historyEventSender.sendWatchEvent(userId,filmId,"rating", (double) newRatingValue);
            userRatingRepository.save(newRating); // Сохраняем новую оценку
        }

        // Сохраняем обновленные данные фильма (сумму и/или количество)
        filmRepository.save(film);
    }
}
