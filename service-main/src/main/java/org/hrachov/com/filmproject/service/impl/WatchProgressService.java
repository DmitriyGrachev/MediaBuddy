package org.hrachov.com.filmproject.service.impl;

import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.WatchProgress;
import org.hrachov.com.filmproject.model.dto.SerialProgressDto;
import org.hrachov.com.filmproject.repository.mongo.WatchProgressRepository;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.service.UserService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class WatchProgressService {
    private final WatchProgressRepository watchProgressRepository;
    private final CurrentUserService currentUserService;
    private final UserService userService;


    public void saveProgress(Long videoId, Double time){
        //User currentUser = currentUserService.getCurrentUser().getUser();
        User user = userService.findByUsername(currentUserService.getCurrentUser().getUsername());

        WatchProgress watchProgress = watchProgressRepository.findByUserIdAndFilmId(user.getId(), videoId).orElse(new WatchProgress());
        watchProgress.setUserId(user.getId());
        watchProgress.setFilmId(videoId);
        watchProgress.setCurrentTime(time);
        watchProgress.setUpdateAt(LocalDateTime.now());
        watchProgress.setType("movie");


        watchProgressRepository.save(watchProgress);
    }
    public void saveProgress(Long videoId, Double time,String episodeId,String seasonId){
        //User currentUser = currentUserService.getCurrentUser().getUser();
        User user = userService.findByUsername(currentUserService.getCurrentUser().getUsername());

        WatchProgress watchProgress = watchProgressRepository.findByUserIdAndFilmId(user.getId(), videoId).orElse(new WatchProgress());
        watchProgress.setUserId(user.getId());
        watchProgress.setFilmId(videoId);
        watchProgress.setCurrentTime(time);
        watchProgress.setUpdateAt(LocalDateTime.now());
        watchProgress.setType("serial");
        watchProgress.setEpisodeId(episodeId);
        watchProgress.setSeasonId(seasonId);

        watchProgressRepository.save(watchProgress);
    }
    public Double getProgress(Long filmId){
        User user = userService.findByUsername(currentUserService.getCurrentUser().getUsername());
        return watchProgressRepository.findByUserIdAndFilmId(user.getId(), filmId)
                .map(WatchProgress::getCurrentTime).orElse(0.0);
    }
    public SerialProgressDto getSerialProgress(Long filmId){
        SerialProgressDto serialProgressDto = null;

        User user = userService.findByUsername(currentUserService.getCurrentUser().getUsername());
        WatchProgress watchProgress = watchProgressRepository.findByUserIdAndFilmId(user.getId(),filmId).orElse(null);
        if(watchProgress == null){
            return serialProgressDto;
        }
        serialProgressDto = new SerialProgressDto();
        serialProgressDto.setProgress(watchProgress.getCurrentTime());
        serialProgressDto.setEpisodeId(watchProgress.getEpisodeId());
        serialProgressDto.setSeasonId(watchProgress.getSeasonId());
        return serialProgressDto;
    }
}
