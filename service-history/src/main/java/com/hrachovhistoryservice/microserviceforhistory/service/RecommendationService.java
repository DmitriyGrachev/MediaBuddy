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
                similarities.put(otherUserId, similarity);
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

        // Фильтруем по жанрам
        return movieScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .filter(movieId -> {
                    if (preferredGenres == null || preferredGenres.isEmpty()) return true;
                    Film film = filmRepository.findById(Integer.parseInt(movieId)).orElse(null);
                    Set<Genre> genres = film.getGenres();
                    return genres.stream().anyMatch(g -> preferredGenres.contains(g.getName()));
                })
                .limit(maxRecommendations)
                .collect(Collectors.toList());
    }
    public Set<Genre> getUserGenres(String userId) {

        Pageable pageable = PageRequest.of(0, 10, Sort.by("date"));
        Page<WatchHistory> watchHistories = watchHistoryRepository.findAll(pageable);
        Set<Genre> genres = new HashSet<>();

        watchHistories.get().map(historySlot ->{
            Film film = filmRepository.findById(Math.toIntExact(historySlot.getFilmId())).orElse(null);
            Set<Genre> filmsGenres = film.getGenres();
            return genres;
        }).forEach(genres::addAll);
        return genres;
    }
}