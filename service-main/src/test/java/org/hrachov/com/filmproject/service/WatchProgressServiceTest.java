package org.hrachov.com.filmproject.service;
import org.hrachov.com.filmproject.model.Role;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.WatchProgress;
import org.hrachov.com.filmproject.model.dto.SerialProgressDto;
import org.hrachov.com.filmproject.repository.mongo.WatchProgressRepository;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.security.UserDetailsImpl;
import org.hrachov.com.filmproject.service.impl.WatchProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WatchProgressServiceTest {

    @Mock
    private WatchProgressRepository watchProgressRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserService userService;

    @InjectMocks
    private WatchProgressService watchProgressService;

    private User user;
    private UserDetails currentUserDetails;
    private Long filmId = 100L;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("password");
        user.setRoles(new HashSet<>(List.of(Role.ROLE_REGULAR)));

        UserDetailsImpl userDetails = new UserDetailsImpl(user);
        currentUserDetails = new UserDetailsImpl(user);

        when(currentUserService.getCurrentUser()).thenReturn(userDetails);
        when(userService.findByUsername("testuser")).thenReturn(user);
    }

    @Test
    void saveProgress_forMovie_whenNewProgress() {
        Double time = 120.5;
        when(watchProgressRepository.findByUserIdAndFilmId(user.getId(), filmId)).thenReturn(Optional.empty());

        watchProgressService.saveProgress(filmId, time);

        ArgumentCaptor<WatchProgress> captor = ArgumentCaptor.forClass(WatchProgress.class);
        verify(watchProgressRepository).save(captor.capture());

        WatchProgress savedProgress = captor.getValue();
        assertEquals(user.getId(), savedProgress.getUserId());
        assertEquals(filmId, savedProgress.getFilmId());
        assertEquals(time, savedProgress.getCurrentTime());
        assertEquals("movie", savedProgress.getType());
        assertNotNull(savedProgress.getUpdateAt());
    }

    @Test
    void saveProgress_forMovie_whenExistingProgress() {
        Double time = 240.0;
        WatchProgress existingProgress = new WatchProgress();
        existingProgress.setCurrentTime(120.5);
        when(watchProgressRepository.findByUserIdAndFilmId(user.getId(), filmId)).thenReturn(Optional.of(existingProgress));

        watchProgressService.saveProgress(filmId, time);

        ArgumentCaptor<WatchProgress> captor = ArgumentCaptor.forClass(WatchProgress.class);
        verify(watchProgressRepository).save(captor.capture());

        WatchProgress savedProgress = captor.getValue();
        assertEquals(time, savedProgress.getCurrentTime());
    }

    @Test
    void saveProgress_forSerial() {
        Double time = 300.0;
        String episodeId = "e01";
        String seasonId = "s01";
        when(watchProgressRepository.findByUserIdAndFilmId(user.getId(), filmId)).thenReturn(Optional.empty());

        watchProgressService.saveProgress(filmId, time, episodeId, seasonId);

        ArgumentCaptor<WatchProgress> captor = ArgumentCaptor.forClass(WatchProgress.class);
        verify(watchProgressRepository).save(captor.capture());

        WatchProgress savedProgress = captor.getValue();
        assertEquals(user.getId(), savedProgress.getUserId());
        assertEquals(filmId, savedProgress.getFilmId());
        assertEquals(time, savedProgress.getCurrentTime());
        assertEquals("serial", savedProgress.getType());
        assertEquals(episodeId, savedProgress.getEpisodeId());
        assertEquals(seasonId, savedProgress.getSeasonId());
    }

    @Test
    void getProgress_whenProgressExists() {
        Double expectedTime = 150.0;
        WatchProgress progress = new WatchProgress();
        progress.setCurrentTime(expectedTime);
        when(watchProgressRepository.findByUserIdAndFilmId(user.getId(), filmId)).thenReturn(Optional.of(progress));

        Double result = watchProgressService.getProgress(filmId);

        assertEquals(expectedTime, result);
    }

    @Test
    void getProgress_whenNoProgress() {
        when(watchProgressRepository.findByUserIdAndFilmId(user.getId(), filmId)).thenReturn(Optional.empty());

        Double result = watchProgressService.getProgress(filmId);

        assertEquals(0.0, result);
    }

    @Test
    void getSerialProgress_whenProgressExists() {
        Double time = 450.0;
        String episodeId = "e02";
        String seasonId = "s02";
        WatchProgress progress = new WatchProgress();
        progress.setCurrentTime(time);
        progress.setEpisodeId(episodeId);
        progress.setSeasonId(seasonId);

        when(watchProgressRepository.findByUserIdAndFilmId(user.getId(), filmId)).thenReturn(Optional.of(progress));

        SerialProgressDto result = watchProgressService.getSerialProgress(filmId);

        assertNotNull(result);
        assertEquals(time, result.getProgress());
        assertEquals(episodeId, result.getEpisodeId());
        assertEquals(seasonId, result.getSeasonId());
    }

    @Test
    void getSerialProgress_whenNoProgress() {
        when(watchProgressRepository.findByUserIdAndFilmId(user.getId(), filmId)).thenReturn(Optional.empty());

        SerialProgressDto result = watchProgressService.getSerialProgress(filmId);

        assertNull(result);
    }
}