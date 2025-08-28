package com.hrachovhistoryservice.microserviceforhistory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrachovhistoryservice.microserviceforhistory.model.WatchHistory;

import com.hrachovhistoryservice.microserviceforhistory.model.dto.WatchHistoryDTO;
import com.hrachovhistoryservice.microserviceforhistory.repo.WatchHistoryRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WatchHistoryService {

    private final WatchHistoryRepository watchHistoryRepository;
    private final ObjectMapper objectMapper;

    public WatchHistoryService(WatchHistoryRepository watchHistoryRepository, ObjectMapper objectMapper) {
        this.watchHistoryRepository = watchHistoryRepository;
        this.objectMapper = objectMapper;
    }

    /*public void saveWatchHistory(User user, Film film) {
        WatchHistory watchHistory = new WatchHistory();
        watchHistory.setUser(user);
        watchHistory.setFilm(film);
        watchHistory.setDate(LocalDateTime.now());
        watchHistoryRepository.save(watchHistory);
    }

     */
    public WatchHistory saveWatchHistoryByIds(Long userId, Long filmId, String type, Double value) {
        WatchHistory wh = new WatchHistory();
        wh.setUserId(userId);
        wh.setFilmId(filmId);
        wh.setDate(LocalDateTime.now());
        WatchHistory watchHistory = watchHistoryRepository.save(wh);
        return watchHistory;
    }

    public WatchHistory getWatchHistoryById(Long userId, Long filmId) {
        return watchHistoryRepository.findByUserIdAndFilmId(userId, filmId);
    }
    public List<WatchHistoryDTO> getWatchHistoryByUserId(Long userId) {
        return watchHistoryRepository.findByUserIdOrderByDateDesc(userId).stream()
                .map(w -> {
                    WatchHistoryDTO watchHistoryDTO = new WatchHistoryDTO();
                    watchHistoryDTO.setId(w.getId());
                    watchHistoryDTO.setDate(w.getDate());
                    watchHistoryDTO.setFilmId(w.getFilmId());
                    watchHistoryDTO.setUserId(w.getUserId());
                    return watchHistoryDTO;
                })
                .collect(Collectors.toList());
    }

}
