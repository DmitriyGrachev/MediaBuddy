package org.hrachov.com.filmproject.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hrachov.com.filmproject.exception.FilmNotFoundException;
import org.hrachov.com.filmproject.model.*;
import org.hrachov.com.filmproject.model.dto.FilmDTO;
import org.hrachov.com.filmproject.model.dto.SeasonDTO;
import org.hrachov.com.filmproject.repository.jpa.*;
import org.hrachov.com.filmproject.service.impl.FilmServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import redis.clients.jedis.JedisPooled;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilmServiceImplTest {

    // --- Mocks для зависимостей ---
    @Mock
    private FilmRepository filmRepository;
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private SerialRepository serialRepository;
    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private EpisodeRepository episodeRepository;
    @Mock
    private GenreRepository genreRepository;
    @Mock
    private JedisPooled jedisPooled;

    // --- Тестируемый сервис ---
    // @InjectMocks автоматически попытается внедрить @Mock поля в конструктор filmService
    @InjectMocks
    private FilmServiceImpl filmService;

    // ObjectMapper не мокаем, так как хотим протестировать реальную логику сериализации
    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- Настройка перед каждым тестом ---
    @BeforeEach
    void setUp() {
        // Явно создаем экземпляр сервиса с моками
        // Это необходимо, так как у нас есть поле ObjectMapper, которое не является моком
        filmService = new FilmServiceImpl(filmRepository, objectMapper, jedisPooled,
                movieRepository, serialRepository, seasonRepository, episodeRepository, genreRepository);
    }


    //-------------------------------------------------------------------------
    // Тесты для CRUD операций
    //-------------------------------------------------------------------------

    @Test
    @DisplayName("findById должен возвращать фильм, если он найден")
    void findById_shouldReturnFilm_whenFound() {
        // --- Arrange ---
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Inception");
        when(filmRepository.findById(1L)).thenReturn(Optional.of(movie));

        // --- Act ---
        Film result = filmService.findById(1L);

        // --- Assert ---
        assertNotNull(result);
        assertEquals("Inception", result.getTitle());
        verify(filmRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("findById должен возвращать null, если фильм не найден")
    void findById_shouldReturnNull_whenNotFound() {
        // --- Arrange ---
        when(filmRepository.findById(99L)).thenReturn(Optional.empty());

        // --- Act ---
        Film result = filmService.findById(99L);

        // --- Assert ---
        assertNull(result);
        verify(filmRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("saveFilmFromDTO должен успешно сохранять новый фильм (Movie)")
    void saveFilmFromDTO_shouldSaveMovieCorrectly() {
        // --- Arrange ---
        FilmDTO dto = new FilmDTO();
        dto.setType("movie");
        dto.setTitle("New Movie");
        dto.setReleaseYear(2025);

        // Мокаем возвращаемое значение при сохранении
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- Act ---
        Film result = filmService.saveFilmFromDTO(dto);

        // --- Assert ---
        assertNotNull(result);
        assertTrue(result instanceof Movie);
        assertEquals("New Movie", result.getTitle());
        // Убедимся, что был вызван save именно для MovieRepository
        verify(movieRepository, times(1)).save(any(Movie.class));
        verify(serialRepository, never()).save(any(Serial.class));
    }



    @Test
    @DisplayName("updateFilmFromDTO должен выбрасывать исключение, если фильм не найден")
    void updateFilmFromDTO_shouldThrowException_whenFilmNotFound() {
        // --- Arrange ---
        FilmDTO dto = new FilmDTO();
        when(filmRepository.findById(99L)).thenReturn(Optional.empty());

        // --- Act & Assert ---
        // Проверяем, что вызов метода с несуществующим ID приводит к исключению
        FilmNotFoundException exception = assertThrows(
                FilmNotFoundException.class, () -> {
            filmService.updateFilmFromDTO(99L, dto);
        });

        assertEquals("Film with id 99 not found", exception.getMessage());
    }

    //-------------------------------------------------------------------------
    // Тесты для сезонов и эпизодов
    //-------------------------------------------------------------------------

    @Test
    @DisplayName("saveSeasonFromDTO должен сохранять сезон и обновлять сериал")
    void saveSeasonFromDTO_shouldSaveSeasonAndUpdateSerial() {
        // --- Arrange ---
        Serial serial = new Serial();
        serial.setId(1L);
        serial.setSeasonList(new ArrayList<>()); // Важно инициализировать список

        SeasonDTO seasonDTO = SeasonDTO.builder()
                .seasonNumber(1)
                .title("First Season")
                .serialFilmId(1L)
                .build();

        when(serialRepository.findById(1L)).thenReturn(Optional.of(serial));
        when(seasonRepository.save(any(Season.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- Act ---
        Season result = filmService.saveSeasonFromDTO(seasonDTO);

        // --- Assert ---
        assertNotNull(result);
        assertEquals("First Season", result.getTitle());
        assertEquals(1, serial.getSeasonList().size()); // Проверяем, что сезон добавлен в список сериала

        verify(serialRepository, times(1)).findById(1L);
        verify(seasonRepository, times(1)).save(any(Season.class));
        verify(serialRepository, times(1)).save(serial); // Проверяем, что сериал был пересохранен
    }

    @Test
    @DisplayName("deleteEpisodeById должен удалять эпизод и обновлять сезон")
    void deleteEpisodeById_shouldDeleteEpisodeAndUpdateSeason() {
        // --- Arrange ---
        Season season = new Season();
        season.setId(10L);
        season.setEpisodeList(new ArrayList<>());

        Episode episode = new Episode();
        episode.setId(101L);
        episode.setSeason(season);

        season.addEpisode(episode); // Добавляем эпизод в сезон

        when(episodeRepository.findById(101L)).thenReturn(Optional.of(episode));

        // --- Act ---
        filmService.deleteEpisodeById(101L);

        // --- Assert ---
        // Проверяем, что методы удаления и сохранения были вызваны
        verify(episodeRepository, times(1)).findById(101L);
        verify(seasonRepository, times(1)).save(season); // Сезон пересохраняется для обновления состояния
        verify(episodeRepository, times(1)).delete(episode);

        // Убедимся, что эпизод удален из списка в объекте сезона
        assertTrue(season.getEpisodeList().isEmpty());
    }
}