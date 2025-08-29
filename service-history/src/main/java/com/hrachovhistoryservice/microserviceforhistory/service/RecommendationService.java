package com.hrachovhistoryservice.microserviceforhistory.service;

import com.hrachovhistoryservice.microserviceforhistory.model.Film;
import com.hrachovhistoryservice.microserviceforhistory.model.Genre;
import com.hrachovhistoryservice.microserviceforhistory.model.Interaction;
import com.hrachovhistoryservice.microserviceforhistory.model.WatchHistory;
import com.hrachovhistoryservice.microserviceforhistory.repo.FilmRepository;
import com.hrachovhistoryservice.microserviceforhistory.repo.GenreRepository;
import com.hrachovhistoryservice.microserviceforhistory.repo.InteractionRepository;
import com.hrachovhistoryservice.microserviceforhistory.repo.WatchHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
@Service
public class RecommendationService {

    @Autowired
    private InteractionRepository interactionRepository;

    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private FilmRepository filmRepository;
    @Autowired
    private WatchHistoryRepository watchHistoryRepository;

    // Косинусное сходство
    private double cosineSimilarity(Map<String, Double> user1Ratings, Map<String, Double> user2Ratings) {
        double dotProduct = 0.0;
        double norm1 = 0.0, norm2 = 0.0;

        for (String movieId : user1Ratings.keySet()) {
            if (user2Ratings.containsKey(movieId)) {
                dotProduct += user1Ratings.get(movieId) * user2Ratings.get(movieId);
            }
            norm1 += Math.pow(user1Ratings.get(movieId), 2);
        }
        for (String movieId : user2Ratings.keySet()) {
            norm2 += Math.pow(user2Ratings.get(movieId), 2);
        }

        return (norm1 == 0 || norm2 == 0) ? 0.0 : dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    // Получение рекомендаций
    public List<String> getRecommendations(String userId, int k, int maxRecommendations, List<String> preferredGenres) {
        // Получаем взаимодействия пользователя
        List<Interaction> userInteractions = interactionRepository.findByUserId(userId);

        // Если у пользователя нет взаимодействий, возвращаем популярные фильмы
        if (userInteractions.isEmpty()) {
            return getPopularFilmsByGenres(preferredGenres, maxRecommendations);
        }

        Map<String, Double> userRatingMap = userInteractions.stream()
                .collect(Collectors.groupingBy(
                        Interaction::getMovieId,
                        Collectors.summingDouble(Interaction::getValue)
                ));

        // Получаем все взаимодействия
        List<Interaction> allInteractions = interactionRepository.findAll();
        Map<String, Map<String, Double>> allUserRatings = new HashMap<>();
        for (Interaction interaction : allInteractions) {
            allUserRatings.computeIfAbsent(interaction.getUserId(), id -> new HashMap<>())
                    .merge(interaction.getMovieId(), interaction.getValue(), Double::sum);
        }

        // Вычисляем сходство с другими пользователями
        Map<String, Double> similarities = new HashMap<>();
        for (String otherUserId : allUserRatings.keySet()) {
            if (!otherUserId.equals(userId)) {
                double similarity = cosineSimilarity(userRatingMap, allUserRatings.get(otherUserId));
                if (similarity > 0) { // Только положительные сходства
                    similarities.put(otherUserId, similarity);
                }
            }
        }

        // Находим K ближайших соседей
        List<String> nearestNeighbors = similarities.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(k)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // Собираем рекомендации
        Map<String, Double> movieScores = new HashMap<>();
        for (String neighborId : nearestNeighbors) {
            Map<String, Double> neighborRatings = allUserRatings.get(neighborId);
            for (Map.Entry<String, Double> entry : neighborRatings.entrySet()) {
                String movieId = entry.getKey();
                if (!userRatingMap.containsKey(movieId)) {
                    movieScores.merge(movieId, entry.getValue() * similarities.get(neighborId), Double::sum);
                }
            }
        }

        // Если нет рекомендаций через collaborative filtering, возвращаем популярные
        if (movieScores.isEmpty()) {
            return getPopularFilmsByGenres(preferredGenres, maxRecommendations);
        }

        // Фильтруем по жанрам
        return movieScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .filter(movieId -> {
                    if (preferredGenres == null || preferredGenres.isEmpty()) return true;
                    try {
                        Film film = filmRepository.findById(Integer.parseInt(movieId)).orElse(null);
                        if (film == null) return false;
                        Set<Genre> genres = film.getGenres();
                        return genres.stream().anyMatch(g -> preferredGenres.contains(g.getName()));
                    } catch (NumberFormatException e) {
                        return false;
                    }
                })
                .limit(maxRecommendations)
                .collect(Collectors.toList());
    }

    // Вспомогательный метод для получения популярных фильмов
    private List<String> getPopularFilmsByGenres(List<String> preferredGenres, int maxRecommendations) {
        // Если жанры не указаны, возвращаем просто популярные фильмы
        if (preferredGenres == null || preferredGenres.isEmpty()) {
            return filmRepository.findAll().stream()
                    .sorted((f1, f2) -> Double.compare(f2.getPopularity() != null ? f2.getPopularity() : 0.0,
                            f1.getPopularity() != null ? f1.getPopularity() : 0.0))
                    .limit(maxRecommendations)
                    .map(film -> String.valueOf(film.getId()))
                    .collect(Collectors.toList());
        }

        // Возвращаем популярные фильмы указанных жанров
        return filmRepository.findAll().stream()
                .filter(film -> film.getGenres().stream()
                        .anyMatch(genre -> preferredGenres.contains(genre.getName())))
                .sorted((f1, f2) -> Double.compare(f2.getPopularity() != null ? f2.getPopularity() : 0.0,
                        f1.getPopularity() != null ? f1.getPopularity() : 0.0))
                .limit(maxRecommendations)
                .map(film -> String.valueOf(film.getId()))
                .collect(Collectors.toList());
    }

    // ИСПРАВЛЕННЫЙ метод получения жанров пользователя
    public Set<Genre> getUserGenres(String userId) {
        try {
            Long userIdLong = Long.valueOf(userId);

            // Получаем историю просмотров конкретного пользователя
            List<WatchHistory> watchHistories = watchHistoryRepository.findByUserId(userIdLong);

            if (watchHistories.isEmpty()) {
                return new HashSet<>(); // Возвращаем пустой набор, если нет истории
            }

            Set<Genre> genres = new HashSet<>();

            // Собираем жанры из всех просмотренных фильмов
            for (WatchHistory history : watchHistories) {
                Film film = filmRepository.findById(Math.toIntExact(history.getFilmId())).orElse(null);
                if (film != null && film.getGenres() != null) {
                    genres.addAll(film.getGenres());
                }
            }

            return genres;
        } catch (NumberFormatException e) {
            System.err.println("Invalid userId format: " + userId);
            return new HashSet<>();
        } catch (Exception e) {
            System.err.println("Error getting user genres: " + e.getMessage());
            return new HashSet<>();
        }
    }
}