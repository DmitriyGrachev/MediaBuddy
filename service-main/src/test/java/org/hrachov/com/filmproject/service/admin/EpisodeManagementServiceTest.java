package org.hrachov.com.filmproject.service.admin;

import org.hrachov.com.filmproject.exception.EpisodeFoundException;
import org.hrachov.com.filmproject.model.Episode;
import org.hrachov.com.filmproject.model.dto.EpisodeDTO;
import org.hrachov.com.filmproject.repository.jpa.EpisodeRepository;
import org.hrachov.com.filmproject.service.FilmService;
import org.hrachov.com.filmproject.service.impl.admin.EpisodeManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EpisodeManagementServiceTest {

    @Mock
    private FilmService filmService;

    @Mock
    private EpisodeRepository episodeRepository;

    @InjectMocks
    private EpisodeManagementService episodeManagementService;

    @BeforeEach
    void setUp() {
        episodeManagementService = new EpisodeManagementService(filmService, episodeRepository);
    }

    @Test
    @DisplayName("createEpisode должен успешно создать эпизод")
    void createEpisode_shouldSucceed() {
        Long seasonId = 1L;
        EpisodeDTO episodeDTO = new EpisodeDTO();
        episodeDTO.setEpisodeNumber(1);
        Episode savedEpisode = new Episode();
        savedEpisode.setId(1L);
        savedEpisode.setEpisodeNumber(1);
        EpisodeDTO expectedEpisodeDTO = new EpisodeDTO();
        expectedEpisodeDTO.setId(1L);
        expectedEpisodeDTO.setEpisodeNumber(1);

        when(filmService.saveEpisodeFromDTO(any(EpisodeDTO.class))).thenReturn(savedEpisode);
        when(filmService.convertToEpisodeDTO(savedEpisode)).thenReturn(expectedEpisodeDTO);

        EpisodeDTO result = episodeManagementService.createEpisode(seasonId, episodeDTO);

        assertNotNull(result);
        assertEquals(expectedEpisodeDTO.getId(), result.getId());
        assertEquals(expectedEpisodeDTO.getEpisodeNumber(), result.getEpisodeNumber());
        assertEquals(seasonId, episodeDTO.getSeasonId());
        verify(filmService, times(1)).saveEpisodeFromDTO(episodeDTO);
        verify(filmService, times(1)).convertToEpisodeDTO(savedEpisode);
    }

    @Test
    @DisplayName("getEpisodeById должен вернуть EpisodeDTO, если эпизод существует")
    void getEpisodeById_shouldReturnEpisodeDTO_whenEpisodeExists() {
        Long episodeId = 1L;
        Episode episode = new Episode();
        episode.setId(episodeId);
        episode.setEpisodeNumber(1);
        EpisodeDTO expectedEpisodeDTO = new EpisodeDTO();
        expectedEpisodeDTO.setId(episodeId);
        expectedEpisodeDTO.setEpisodeNumber(1);

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(episode));
        when(filmService.convertToEpisodeDTO(episode)).thenReturn(expectedEpisodeDTO);

        EpisodeDTO result = episodeManagementService.getEpisodeById(episodeId);

        assertNotNull(result);
        assertEquals(expectedEpisodeDTO.getId(), result.getId());
        assertEquals(expectedEpisodeDTO.getEpisodeNumber(), result.getEpisodeNumber());
        verify(episodeRepository, times(1)).findById(episodeId);
        verify(filmService, times(1)).convertToEpisodeDTO(episode);
    }

    @Test
    @DisplayName("getEpisodeById должен выбросить исключение, если эпизод не найден")
    void getEpisodeById_shouldThrowException_whenEpisodeNotFound() {
        Long episodeId = 99L;
        when(episodeRepository.findById(episodeId)).thenReturn(Optional.empty());

        EpisodeFoundException exception = assertThrows(
                EpisodeFoundException.class,
                () -> episodeManagementService.getEpisodeById(episodeId)
        );


        assertEquals("Episode " + episodeId + " was not found", exception.getMessage());
        verify(episodeRepository, times(1)).findById(episodeId);
        verify(filmService, never()).convertToEpisodeDTO(any());
    }

    @Test
    @DisplayName("updateEpisode должен успешно обновить эпизод")
    void updateEpisode_shouldSucceed() {
        Long episodeId = 1L;
        EpisodeDTO episodeDTO = new EpisodeDTO();
        episodeDTO.setEpisodeNumber(2);
        Episode updatedEpisode = new Episode();
        updatedEpisode.setId(episodeId);
        updatedEpisode.setEpisodeNumber(2);
        EpisodeDTO expectedEpisodeDTO = new EpisodeDTO();
        expectedEpisodeDTO.setId(episodeId);
        expectedEpisodeDTO.setEpisodeNumber(2);

        when(filmService.updateEpisodeFromDTO(episodeId, episodeDTO)).thenReturn(updatedEpisode);
        when(filmService.convertToEpisodeDTO(updatedEpisode)).thenReturn(expectedEpisodeDTO);

        EpisodeDTO result = episodeManagementService.updateEpisode(episodeId, episodeDTO);

        assertNotNull(result);
        assertEquals(expectedEpisodeDTO.getId(), result.getId());
        assertEquals(expectedEpisodeDTO.getEpisodeNumber(), result.getEpisodeNumber());
        verify(filmService, times(1)).updateEpisodeFromDTO(episodeId, episodeDTO);
        verify(filmService, times(1)).convertToEpisodeDTO(updatedEpisode);
    }

    @Test
    @DisplayName("deleteEpisode должен успешно удалить эпизод")
    void deleteEpisode_shouldSucceed() {
        Long episodeId = 1L;

        doNothing().when(filmService).deleteEpisodeById(episodeId);

        episodeManagementService.deleteEpisode(episodeId);

        verify(filmService, times(1)).deleteEpisodeById(episodeId);
    }
}