package org.hrachov.com.filmproject.service.admin;

import org.hrachov.com.filmproject.exception.*;
import org.hrachov.com.filmproject.model.Film;
import org.hrachov.com.filmproject.model.Season;
import org.hrachov.com.filmproject.model.Serial;
import org.hrachov.com.filmproject.model.dto.*;
import org.hrachov.com.filmproject.repository.jpa.*;
import org.hrachov.com.filmproject.service.FilmService;
import org.hrachov.com.filmproject.service.impl.ExternalApiService;
import org.hrachov.com.filmproject.service.impl.admin.FilmManagementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FilmManagementServiceTest {
    @Mock
    private GenreRepository genreRepository;
    @Mock
    private FilmService filmService;
    @Mock
    private FilmRepository filmRepository;
    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private ExternalApiService externalApiService;
    @Mock
    private SerialRepository serialRepository;
    @InjectMocks
    private FilmManagementService filmManagementService;

    @BeforeEach
    void setUp() {
        filmManagementService = new FilmManagementService(genreRepository, filmService, filmRepository, seasonRepository, externalApiService,serialRepository);
    }

    @Test
    @DisplayName("getFilmDataFromExternalApi должен вернуть FilmDTO, когда API отвечает успешно")
    void getFilmDataFromExternalApi_shouldReturnDto_whenApiSucceeds() {
        String title = "Inception";
        OmdbDTO omdbDTO = new OmdbDTO();
        omdbDTO.setTitle("Inception");
        omdbDTO.setType("movie");

        when(externalApiService.getFilmFromOmdb(title)).thenReturn(omdbDTO);

        FilmDTO result = filmManagementService.getFilmDataFromExternalApi(title);

        assertNotNull(result);
        assertEquals("Inception", result.getTitle());
        verify(externalApiService, times(1)).getFilmFromOmdb(title);
        verifyNoMoreInteractions(externalApiService);
    }

    @Test
    @DisplayName("getFilmDataFromExternalApi должен выбросить исключение, когда фильм не найден")
    void getFilmDataFromExternalApi_shouldThrowException_whenFilmNotFound() {
        String title = "NonExistentMovie";
        OmdbDTO omdbDTO = new OmdbDTO();
        omdbDTO.setResponse("False");

        when(externalApiService.getFilmFromOmdb(title)).thenReturn(omdbDTO);

        OmdbException exception = assertThrows(
                OmdbException.class,
                () -> filmManagementService.getFilmDataFromExternalApi(title)
        );

        assertEquals("Omdb found nothing for " + title, exception.getMessage());
    }

    @Test
    @DisplayName("deleteFilm должен успешно удалить фильм, если он существует")
    void deleteFilm_shouldSucceed_whenFilmExists() {
        Long filmId = 1L;
        when(filmRepository.existsById(filmId)).thenReturn(true);
        doNothing().when(filmRepository).deleteById(filmId);

        filmManagementService.deleteFilm(filmId);

        verify(filmRepository).existsById(filmId);
        verify(filmRepository).deleteById(filmId);
    }

    @Test
    @DisplayName("deleteFilm должен выбросить исключение, если фильм не существует")
    void deleteFilm_shouldThrowException_whenFilmDoesNotExist() {
        Long filmId = 99L;
        when(filmRepository.existsById(filmId)).thenReturn(false);

        FilmNotFoundException filmNotFoundException = assertThrows(
                FilmNotFoundException.class,
                () -> filmManagementService.deleteFilm(filmId)
        );

        verify(filmRepository).existsById(filmId);
        verify(filmRepository, never()).deleteById(anyLong());
        assertEquals("Film with id 99 not found", filmNotFoundException.getMessage());
    }
    @Test
    @DisplayName("Create film should work without errors")
    void createFilm_shouldCreateFilmFromDto() {
        FilmDTO filmDTO = new FilmDTO();
        filmDTO.setTitle("Inception");
        filmDTO.setType("serial");
        filmDTO.setSeasons(1);

        Film film = new Film();
        film.setTitle("Inception");
        film.setId(99L);

        Season season = new Season();
        season.setSeasonNumber(1);


        Serial serial = new Serial();
        serial.setTitle("Inception");
        serial.setId(film.getId());

        when(filmService.saveFilmFromDTO(any(FilmDTO.class))).thenReturn(film);
        when(serialRepository.findById(serial.getId())).thenReturn(Optional.of(serial));
        when(seasonRepository.save(any(Season.class))).thenReturn(season);
        when(filmService.convertToFilmDTO(any(Film.class))).thenReturn(new FilmDTO());


        FilmDTO result = filmManagementService.createFilm(filmDTO);

        assertNotNull(result);
        verify(filmService,times(1)).saveFilmFromDTO(any(FilmDTO.class));
        verify(seasonRepository, times(filmDTO.getSeasons())).save(any(Season.class));
        verify(serialRepository,times(1)).findById(serial.getId());
        verify(filmService,times(1)).convertToFilmDTO(any(Film.class));
    }
    @Test
    @DisplayName("Create film from dto Sgould throw exception")
    void createFilmFromDto_shouldThrowException_whenFilmNotFound() {
        FilmDTO filmDTO = new FilmDTO();
        filmDTO.setTitle("Inception");
        filmDTO.setType("serial");
        filmDTO.setSeasons(1);
        filmDTO.setSeasons(2);
        filmDTO.setId(99L);

        Film film = new Film();
        film.setTitle("Inception");
        film.setId(99L);

        when(filmService.saveFilmFromDTO(any(FilmDTO.class))).thenReturn(film);
        when(serialRepository.findById(filmDTO.getId()))
                .thenThrow(SerialNotFoundException.class);

        SerialNotFoundException serialNotFoundException = assertThrows(
                 SerialNotFoundException.class,
                ()->filmManagementService.createFilm(filmDTO)
        );

        verify(filmService,times(1)).saveFilmFromDTO(any(FilmDTO.class));
        verify(serialRepository,times(1)).findById(anyLong());
        verify(seasonRepository,never()).save(any(Season.class));
    }
    @Test
    @DisplayName("getAllFilms should return all films from database")
    void getAllFilms_shouldReturnAllFilmsFromDatabase() {
        // Arrange
        Film film1 = new Film();
        film1.setId(1L);
        film1.setTitle("Inception");
        Film film2 = new Serial();
        film2.setId(2L);
        film2.setTitle("Breaking Bad");
        List<Film> films = Arrays.asList(film1, film2);

        FilmDTO filmDTO1 = new FilmDTO();
        filmDTO1.setId(1L);
        filmDTO1.setTitle("Inception");
        FilmDTO filmDTO2 = new FilmDTO();
        filmDTO2.setId(2L);
        filmDTO2.setTitle("Breaking Bad");

        when(filmRepository.findAll()).thenReturn(films);
        when(filmService.convertToFilmDTO(film1)).thenReturn(filmDTO1);
        when(filmService.convertToFilmDTO(film2)).thenReturn(filmDTO2);

        // Act
        List<FilmDTO> result = filmManagementService.getAllFilms();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Inception", result.get(0).getTitle());
        assertEquals("Breaking Bad", result.get(1).getTitle());
        verify(filmRepository, times(1)).findAll();
        verify(filmService, times(1)).convertToFilmDTO(film1);
        verify(filmService, times(1)).convertToFilmDTO(film2);
    }

    @Test
    @DisplayName("getAllFilms should return empty list when no films exist")
    void getAllFilms_shouldReturnEmptyListWhenNoFilms() {
        // Arrange
        when(filmRepository.findAll()).thenReturn(List.of());

        // Act
        List<FilmDTO> result = filmManagementService.getAllFilms();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(filmRepository, times(1)).findAll();
        verify(filmService, never()).convertToFilmDTO(any(Film.class));
    }

    @Test
    @DisplayName("getFilmById should return FilmDTO when film exists")
    void getFilmById_shouldReturnFilmDTOWhenFilmExists() {
        // Arrange
        Long filmId = 1L;
        Film film = new Film();
        film.setId(filmId);
        film.setTitle("Inception");
        FilmDTO filmDTO = new FilmDTO();
        filmDTO.setId(filmId);
        filmDTO.setTitle("Inception");

        when(filmRepository.findById(filmId)).thenReturn(Optional.of(film));
        when(filmService.convertToFilmDTO(film)).thenReturn(filmDTO);

        // Act
        FilmDTO result = filmManagementService.getFilmById(filmId);

        // Assert
        assertNotNull(result);
        assertEquals(filmId, result.getId());
        assertEquals("Inception", result.getTitle());
        verify(filmRepository, times(1)).findById(filmId);
        verify(filmService, times(1)).convertToFilmDTO(film);
    }

    @Test
    @DisplayName("getFilmById should throw ResponseStatusException when film not found")
    void getFilmById_shouldThrowExceptionWhenFilmNotFound() {
        // Arrange
        Long filmId = 99L;
        when(filmRepository.findById(filmId)).thenReturn(Optional.empty());

        // Act & Assert
        FilmNotFoundException exception = assertThrows(
                FilmNotFoundException.class,
                () -> filmManagementService.getFilmById(filmId)
        );

        assertEquals("Film with id " + filmId + " not found", exception.getMessage());
        verify(filmRepository, times(1)).findById(filmId);
        verify(filmService, never()).convertToFilmDTO(any(Film.class));
    }

    @Test
    @DisplayName("updateFilm should update and return FilmDTO when film exists")
    void updateFilm_shouldUpdateAndReturnFilmDTO() {
        // Arrange
        Long filmId = 1L;
        FilmDTO filmDTO = new FilmDTO();
        filmDTO.setId(filmId);
        filmDTO.setTitle("Inception Updated");
        Film updatedFilm = new Film();
        updatedFilm.setId(filmId);
        updatedFilm.setTitle("Inception Updated");
        FilmDTO updatedFilmDTO = new FilmDTO();
        updatedFilmDTO.setId(filmId);
        updatedFilmDTO.setTitle("Inception Updated");

        when(filmService.updateFilmFromDTO(filmId, filmDTO)).thenReturn(updatedFilm);
        when(filmService.convertToFilmDTO(updatedFilm)).thenReturn(updatedFilmDTO);

        // Act
        FilmDTO result = filmManagementService.updateFilm(filmId, filmDTO);

        // Assert
        assertNotNull(result);
        assertEquals(filmId, result.getId());
        assertEquals("Inception Updated", result.getTitle());
        verify(filmService, times(1)).updateFilmFromDTO(filmId, filmDTO);
        verify(filmService, times(1)).convertToFilmDTO(updatedFilm);
    }

    @Test
    @DisplayName("updateFilm should throw exception when update fails")
    void updateFilm_shouldThrowExceptionWhenUpdateFails() {
        // Arrange
        Long filmId = 99L;
        FilmDTO filmDTO = new FilmDTO();
        filmDTO.setId(filmId);
        filmDTO.setTitle("Inception Updated");

        when(filmService.updateFilmFromDTO(filmId, filmDTO))
                .thenThrow(new FilmNotFoundException(filmId));

        // Act & Assert
        FilmNotFoundException exception = assertThrows(
                FilmNotFoundException.class,
                () -> filmManagementService.updateFilm(filmId, filmDTO)
        );

        assertEquals("Film with id " + filmId + " not found", exception.getMessage());
        verify(filmService, times(1)).updateFilmFromDTO(filmId, filmDTO);
        verify(filmService, never()).convertToFilmDTO(any(Film.class));
    }

    @Test
    @DisplayName("deleteFilm should succeed when film exists")
    void deleteFilm_shouldSucceedWhenFilmExists() {
        // Arrange
        Long filmId = 1L;
        when(filmRepository.existsById(filmId)).thenReturn(true);
        doNothing().when(filmRepository).deleteById(filmId);

        // Act
        filmManagementService.deleteFilm(filmId);

        // Assert
        verify(filmRepository, times(1)).existsById(filmId);
        verify(filmRepository, times(1)).deleteById(filmId);
    }

    @Test
    @DisplayName("deleteFilm should throw FilmNotFoundException when film does not exist")
    void deleteFilm_shouldThrowExceptionWhenFilmDoesNotExist() {
        // Arrange
        Long filmId = 99L;
        when(filmRepository.existsById(filmId)).thenReturn(false);

        // Act & Assert
        FilmNotFoundException exception = assertThrows(
                FilmNotFoundException.class,
                () -> filmManagementService.deleteFilm(filmId)
        );
        assertEquals("Film with id 99 not found", exception.getMessage());
        verify(filmRepository, times(1)).existsById(filmId);
        verify(filmRepository, never()).deleteById(anyLong());
    }
}