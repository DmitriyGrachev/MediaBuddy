package com.hrachovhistoryservice.microserviceforhistory.controller;

import com.hrachovhistoryservice.microserviceforhistory.model.Film;
import com.hrachovhistoryservice.microserviceforhistory.model.Genre;
import com.hrachovhistoryservice.microserviceforhistory.model.dto.WatchHistoryDTO;
import com.hrachovhistoryservice.microserviceforhistory.repo.FilmRepository;
import com.hrachovhistoryservice.microserviceforhistory.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;
    @Autowired
    private FilmRepository filmRepository;

    //            @RequestParam(required = false) List<String> genres
    @GetMapping("/{userId}")
    public ResponseEntity<List<WatchHistoryDTO>> getRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "5") int k,
            @RequestParam(defaultValue = "10") int maxRecommendations) {

        List<String> genres = recommendationService.getUserGenres(userId).stream().map(genre ->{
            return genre.getName();
        }).toList();

        List<String> ids = recommendationService.getRecommendations(userId, k, maxRecommendations, genres);
        List<Film> films = ids.stream().map(id ->{
            return filmRepository.findById(Integer.valueOf(id)).orElse(null);
        }).filter(film -> {
            return film != null;
        }).collect(Collectors.toList());
        List<WatchHistoryDTO> result = films.stream().map(film ->{
            WatchHistoryDTO watchHistoryDTO = new WatchHistoryDTO();
            watchHistoryDTO.setId(film.getId());
            watchHistoryDTO.setFilmId(film.getId());
            return watchHistoryDTO;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
}