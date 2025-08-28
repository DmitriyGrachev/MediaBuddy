package com.hrachovhistoryservice.microserviceforhistory.controller;

import com.hrachovhistoryservice.microserviceforhistory.model.dto.WatchHistoryDTO;
import com.hrachovhistoryservice.microserviceforhistory.service.WatchHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final WatchHistoryService historyService;

    // Получить всю историю просмотра пользователя
    @GetMapping("/{userId}")
    public ResponseEntity<List<WatchHistoryDTO>> getWatchHistory(@PathVariable Long userId) {
        List<WatchHistoryDTO> list = historyService.getWatchHistoryByUserId(userId);
        log.info("USER " + userId + " HISTORY :" + list.toString());
        return ResponseEntity.ok(list);
    }
    // Обновленный эндпоинт для сохранения/обновления прогресса
    //@PostMapping("/progress")
    //public ResponseEntity<Void> saveWatchProgress(@RequestBody WatchProgressRequest request) {
    //   User currentUser = currentUserService.getCurrentUser().getUser();
    //   if (currentUser == null) {
    //      return ResponseEntity.status(403).build(); // Или 401 Unauthorized
    //  }
    //  watchHistoryService.saveOrUpdateWatchProgress(currentUser, request.getFilmId(), request.getCurrentTime());
    //  return ResponseEntity.ok().build();
    //}
}
