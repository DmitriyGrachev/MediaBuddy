package org.hrachov.com.filmproject.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hrachov.com.filmproject.model.*;
import org.hrachov.com.filmproject.model.dto.ProfileWatchHistoryDto;
import org.hrachov.com.filmproject.model.dto.WatchHistoryDTO;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.service.FilmService;
import org.hrachov.com.filmproject.service.MovieService;
import org.hrachov.com.filmproject.service.UserService;
import org.hrachov.com.filmproject.service.impl.SerialService;
import org.hrachov.com.filmproject.utils.HistoryEventSender;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/watch")
@RequiredArgsConstructor
public class WatchEventController {
    private final CurrentUserService currentUserService;
    private final HistoryEventSender historyEventSender;
    private final RestTemplate restTemplate;
    private final UserService userService; // Добавляем сервис пользователей
    private final MovieService movieService;
    private final SerialService serialService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;


    @PostMapping("/{filmId}")
    public ResponseEntity<?> watchFilm(@PathVariable Long filmId) {
        User currentUser = null;
        try {
            currentUser = userService.findByUsername(currentUserService.getCurrentUser().getUsername());
        } catch (Exception e) {
            // Заменяем log.error на System.out.println с выводом ошибки
            System.out.println("Error getting current user: " + e.getMessage());
            e.printStackTrace(); // Выводим полный стектрейс
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error getting user information.");
        }

        if (currentUser == null) {
            // Заменяем log.warn на System.out.println
            System.out.println("Current user is null after attempting to fetch. Cannot send watch event.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated or not found.");
        }

        Long userId = currentUser.getId();

        System.out.println("Attempting to send watch event for userId: " + userId + ", filmId: " + filmId);
        try {
            historyEventSender.sendWatchEvent(userId, filmId,"watch",4.0);
            // Заменяем log.info на System.out.println
            System.out.println("Watch event send call completed for userId: " + userId + ", filmId: " + filmId);
        } catch (Exception e) {
            // Заменяем log.error на System.out.println с выводом стектрейса
            System.out.println("Exception occurred in WatchEventController while trying to send watch event for userId: " + userId + ", filmId: " + filmId);
            e.printStackTrace(); // Выводим полный стектрейс
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process watch event.");
        }

        return ResponseEntity.ok().build();
    }
    @GetMapping("/history")
    public ResponseEntity<List<ProfileWatchHistoryDto>> history() {
        //TODO стоит также закешировать но нужно обновлять кеш на микросервисе
        User currentUser = userService.findByUsername(currentUserService.getCurrentUser().getUsername());
        ResponseEntity<List<WatchHistoryDTO>> response = restTemplate.exchange(
                "http://localhost:8082/api/history/" + currentUser.getId(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<WatchHistoryDTO>>() {}
        );

        List<WatchHistoryDTO> historyList = response.getBody();
        List<ProfileWatchHistoryDto> profileWatchHistoryDtos = recommendedHistory(historyList);

        if (profileWatchHistoryDtos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(profileWatchHistoryDtos);
    }
    @GetMapping("/recommendations")
    public ResponseEntity<List<ProfileWatchHistoryDto>> recommendations() throws JsonProcessingException {
        User currentUser = userService.findByUsername(currentUserService.getCurrentUser().getUsername());
        String key = "user" + currentUser.getId() + ":recommendations";
        List<ProfileWatchHistoryDto> profileWatchHistoryDtos = null;
        String cache = redisTemplate.opsForValue().get(key);
        if (cache != null) {
            profileWatchHistoryDtos = objectMapper.readValue(cache, new TypeReference<List<ProfileWatchHistoryDto>>(){});
        }else{
            //TODO Решил воспользоваться тем же ДТО что и Истории что бы не создавать лишенего так как поля идентичные
            ResponseEntity<List<WatchHistoryDTO>> response = restTemplate.exchange(
                    "http://localhost:8082//api/recommendations/" + currentUser.getId(),
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<WatchHistoryDTO>>() {}
            );
            List<WatchHistoryDTO> recommendedHistoryList = response.getBody();
            profileWatchHistoryDtos = recommendedHistory(recommendedHistoryList);

            if (profileWatchHistoryDtos.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            String jsonToCache = objectMapper.writeValueAsString(profileWatchHistoryDtos);
            redisTemplate.opsForValue().set(key, jsonToCache, Duration.ofMinutes(10));
        }

        return ResponseEntity.ok(profileWatchHistoryDtos);

    }
    public List<ProfileWatchHistoryDto> recommendedHistory(List<WatchHistoryDTO> historyList) {
        if(historyList == null || historyList.size() == 0) {
            log.info("No history found");
            return Collections.emptyList();
        }
        List<ProfileWatchHistoryDto> profileWatchHistoryDtos = historyList.stream().map(history ->{
            ProfileWatchHistoryDto profileWatchHistoryDto = new ProfileWatchHistoryDto();
            profileWatchHistoryDto.setDate(history.getDate());
            profileWatchHistoryDto.setFilmId(history.getFilmId().toString());
            log.info("ProfileWatchHistoryDto: " + profileWatchHistoryDto);
            Movie movie = movieService.getMovieById(history.getFilmId());
            log.info("Movie: " + movie);
            if (movie != null) {
                profileWatchHistoryDto.setType("movie");
                profileWatchHistoryDto.setPosterUrl(movie.getPosterPath());
                profileWatchHistoryDto.setTitle(movie.getTitle());
            }else {
                Serial serial = serialService.getSerialDetails(history.getFilmId())
                        .orElseThrow(()->new RuntimeException("Movie and Serial not found"));
                profileWatchHistoryDto.setType("serial");
                profileWatchHistoryDto.setPosterUrl(serial.getPoster());
                profileWatchHistoryDto.setTitle(serial.getTitle());
            }
            log.info("FINAL ProfileWatchHistoryDto: " + profileWatchHistoryDto);
            return profileWatchHistoryDto;
        }).toList();
        return profileWatchHistoryDtos;
    }
}
