package org.hrachov.com.filmproject.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.security.PermitAll;
import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.model.Film;
import org.hrachov.com.filmproject.model.Movie;
import org.hrachov.com.filmproject.model.Serial;
import org.hrachov.com.filmproject.service.FilmService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.hrachov.com.filmproject.model.dto.FilmDTO;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/films")
@AllArgsConstructor
public class FilmsController {

    private final FilmService filmService;

    @GetMapping("/newFilms")
    public ResponseEntity<List<FilmDTO>> newFilms() {
        return ResponseEntity.ok(filmService.getAllNewFilms());
    }

    @GetMapping("/carousel")
    public ResponseEntity<List<FilmDTO>> carouselFilms() throws JsonProcessingException {
        List<FilmDTO> films = filmService.getFilmsByRevelence();

        return ResponseEntity.ok(films);
    }
}