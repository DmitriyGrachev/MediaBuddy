package org.hrachov.com.filmproject.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hrachov.com.filmproject.model.Genre;
import org.hrachov.com.filmproject.model.dto.FilmDTO;
import org.hrachov.com.filmproject.model.dto.GenreDTO;
import org.hrachov.com.filmproject.repository.jpa.GenreRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/search/films")
@RequiredArgsConstructor
public class FilmSearchController {

    private final FilmSearchService filmSearchService;
    private final GenreRepository genreRepository;
    private final RedisTemplate redisTemplate;

    @PostMapping
    public List<FilmDTO> searchByDto(@RequestBody FilmSearchCriteria filmSearchCriteria) {
        //Добавил поиск по жанрам
        List<String> genres = filmSearchCriteria.getGenres() != null ? filmSearchCriteria.getGenres() : new ArrayList<>();

        return filmSearchService.searchFilms(filmSearchCriteria).stream().filter(film ->{
            List<GenreDTO> filmGenre = film.getGenres();
            List<String> filmGenreList = filmGenre.stream().map(GenreDTO::getName).collect(Collectors.toList());
            return filmGenreList.containsAll(genres);
        }).toList();
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/reindex")
    public ResponseEntity<String> reindex() {
        filmSearchService.reindexAll();
        return ResponseEntity.ok("Reindexing process started.");
    }

    @GetMapping("/genres")
    public ResponseEntity<List<String>> getGenres() throws JsonProcessingException {
        String key = "mediabuddy:genres:all";
        ObjectMapper objectMapper = new ObjectMapper();
        List<String> genres = null;

        try {
            String cachedGenres = redisTemplate.opsForValue().get(key).toString();
            if (cachedGenres != null) {
                genres = objectMapper.readValue(cachedGenres, new TypeReference<List<String>>(){});
            }
        } catch (Exception e) {
            // Обработка ошибки десериализации из Redis
            log.error("Ошибка при чтении жанров из Redis", e);
        }

        if (genres == null) {
            genres = genreRepository.findAll().stream()
                    .map(Genre::getName)
                    .collect(Collectors.toList());

            if (genres == null || genres.isEmpty()) {
                return ResponseEntity.noContent().build();
            }

            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(genres), Duration.ofHours(24));
        }

        return ResponseEntity.ok(genres);
    }
    /*
    @GetMapping("autocomplete")
    public ResponseEntity<List<FilmDTO>> searchFast(@RequestParam String title) {
        List<FilmDTO> list = filmSearchService.autocompleteTitles(title);

        if(list.isEmpty()){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(list);
    }
     */
}
