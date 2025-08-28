package org.hrachov.com.filmproject.controller;

import lombok.extern.slf4j.Slf4j;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.WatchProgress;
import org.hrachov.com.filmproject.model.dto.SerialProgressDto;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.service.impl.WatchProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
@Slf4j
@RestController
@RequestMapping("/api/progress")
public class WatchProgressController {
    private final WatchProgressService watchProgressService;
    private final CurrentUserService currentUserService;

    public WatchProgressController(WatchProgressService watchProgressService, CurrentUserService currentUserService) {
        this.watchProgressService = watchProgressService;
        this.currentUserService = currentUserService;
    }

    @PostMapping
    public ResponseEntity<?> getProgress(@RequestParam Long filmId
            , @RequestParam Double time) {

        watchProgressService.saveProgress(filmId, time);
        return ResponseEntity.ok().build();
    }
    @GetMapping
    public ResponseEntity<?> getAllProgress(@RequestParam Long filmId) {

        return ResponseEntity.ok(watchProgressService.getProgress(filmId));
    }
    @PostMapping("/episode")
    public ResponseEntity<?> saveEpisodeProgress(@RequestParam Long filmId, @RequestParam Double time, @RequestParam String episodeId,@RequestParam String seasonId) {
        watchProgressService.saveProgress(filmId, time,episodeId,seasonId);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/episode")
    public ResponseEntity<SerialProgressDto> getSerialProgress(@RequestParam Long filmId){

        return ResponseEntity.ok(watchProgressService.getSerialProgress(filmId));
    }
}
