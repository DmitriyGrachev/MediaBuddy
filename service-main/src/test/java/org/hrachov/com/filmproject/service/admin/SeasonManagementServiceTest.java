package org.hrachov.com.filmproject.service.admin;
import org.hrachov.com.filmproject.exception.SeasonNotFoundException;
import org.hrachov.com.filmproject.model.Season;
import org.hrachov.com.filmproject.model.dto.SeasonDTO;
import org.hrachov.com.filmproject.repository.jpa.SeasonRepository;
import org.hrachov.com.filmproject.service.FilmService;
import org.hrachov.com.filmproject.service.impl.admin.SeasonManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class SeasonManagementServiceTest {

    @Mock
    private FilmService filmService;

    @Mock
    private SeasonRepository seasonRepository;

    @InjectMocks
    private SeasonManagementService seasonManagementService;

    @BeforeEach
    void setUp() {
        seasonManagementService = new SeasonManagementService(filmService, seasonRepository);
    }

    @Test
    @DisplayName("createSeason должен успешно создать сезон")
    void createSeason_shouldSucceed() {
        Long serialId = 1L;
        SeasonDTO seasonDTO = new SeasonDTO();
        seasonDTO.setSeasonNumber(1);
        Season savedSeason = new Season();
        savedSeason.setId(1L);
        savedSeason.setSeasonNumber(1);
        SeasonDTO expectedSeasonDTO = new SeasonDTO();
        expectedSeasonDTO.setId(1L);
        expectedSeasonDTO.setSeasonNumber(1);

        when(filmService.saveSeasonFromDTO(any(SeasonDTO.class))).thenReturn(savedSeason);
        when(filmService.convertToSeasonDTO(savedSeason)).thenReturn(expectedSeasonDTO);

        SeasonDTO result = seasonManagementService.createSeason(serialId, seasonDTO);

        assertNotNull(result);
        assertEquals(expectedSeasonDTO.getId(), result.getId());
        assertEquals(expectedSeasonDTO.getSeasonNumber(), result.getSeasonNumber());
        assertEquals(serialId, seasonDTO.getSerialFilmId());
        verify(filmService, times(1)).saveSeasonFromDTO(seasonDTO);
        verify(filmService, times(1)).convertToSeasonDTO(savedSeason);
    }

    @Test
    @DisplayName("getSeasonById должен вернуть SeasonDTO, если сезон существует")
    void getSeasonById_shouldReturnSeasonDTO_whenSeasonExists() {
        Long seasonId = 1L;
        Season season = new Season();
        season.setId(seasonId);
        season.setSeasonNumber(1);
        SeasonDTO expectedSeasonDTO = new SeasonDTO();
        expectedSeasonDTO.setId(seasonId);
        expectedSeasonDTO.setSeasonNumber(1);

        when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));
        when(filmService.convertToSeasonDTO(season)).thenReturn(expectedSeasonDTO);

        SeasonDTO result = seasonManagementService.getSeasonById(seasonId);

        assertNotNull(result);
        assertEquals(expectedSeasonDTO.getId(), result.getId());
        assertEquals(expectedSeasonDTO.getSeasonNumber(), result.getSeasonNumber());
        verify(seasonRepository, times(1)).findById(seasonId);
        verify(filmService, times(1)).convertToSeasonDTO(season);
    }

    @Test
    @DisplayName("getSeasonById должен выбросить исключение, если сезон не найден")
    void getSeasonById_shouldThrowException_whenSeasonNotFound() {
        Long seasonId = 99L;
        when(seasonRepository.findById(seasonId)).thenReturn(Optional.empty());

        SeasonNotFoundException exception = assertThrows(
                SeasonNotFoundException.class,
                () -> seasonManagementService.getSeasonById(seasonId)
        );


        assertEquals("The season with id " + seasonId + " was not found.", exception.getMessage());
        verify(seasonRepository, times(1)).findById(seasonId);
        verify(filmService, never()).convertToSeasonDTO(any());
    }

    @Test
    @DisplayName("updateSeason должен успешно обновить сезон")
    void updateSeason_shouldSucceed() {
        Long seasonId = 1L;
        SeasonDTO seasonDTO = new SeasonDTO();
        seasonDTO.setSeasonNumber(2);
        Season updatedSeason = new Season();
        updatedSeason.setId(seasonId);
        updatedSeason.setSeasonNumber(2);
        SeasonDTO expectedSeasonDTO = new SeasonDTO();
        expectedSeasonDTO.setId(seasonId);
        expectedSeasonDTO.setSeasonNumber(2);

        when(filmService.updateSeasonFromDTO(seasonId, seasonDTO)).thenReturn(updatedSeason);
        when(filmService.convertToSeasonDTO(updatedSeason)).thenReturn(expectedSeasonDTO);

        SeasonDTO result = seasonManagementService.updateSeason(seasonId, seasonDTO);

        assertNotNull(result);
        assertEquals(expectedSeasonDTO.getId(), result.getId());
        assertEquals(expectedSeasonDTO.getSeasonNumber(), result.getSeasonNumber());
        verify(filmService, times(1)).updateSeasonFromDTO(seasonId, seasonDTO);
        verify(filmService, times(1)).convertToSeasonDTO(updatedSeason);
    }

    @Test
    @DisplayName("deleteSeason должен успешно удалить сезон")
    void deleteSeason_shouldSucceed() {
        Long seasonId = 1L;

        doNothing().when(filmService).deleteSeasonById(seasonId);

        seasonManagementService.deleteSeason(seasonId);

        verify(filmService, times(1)).deleteSeasonById(seasonId);
    }
}