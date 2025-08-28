package org.hrachov.com.filmproject.controller;

import org.hrachov.com.filmproject.model.Movie;
import org.hrachov.com.filmproject.model.dto.*;
import org.hrachov.com.filmproject.repository.jpa.MovieRepository;
import org.hrachov.com.filmproject.service.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/movies")
public class MoviesController {

    private final MovieService movieService;
    private final MovieRepository movieRepository;

    public MoviesController(MovieService movieService, MovieRepository movieRepository) {
        this.movieService = movieService;
        this.movieRepository = movieRepository;
    }

    @GetMapping
    public ResponseEntity<PagedResponseDTO<MovieDTO>> getMovies(
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String genre) {



        // Regular paginated results
        PagedResponseDTO<MovieDTO> movies = movieService.getAllMoviesWithPageable(sortBy, sortDir, page, size, genre);
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/carousel")
    public ResponseEntity<List<MovieDTO>> getTenMovies(){
        List<MovieDTO> list = movieService.getAllMoviesForCarousel();

        return ResponseEntity.ok(list);
    }

    @GetMapping("/search")
    public ResponseEntity<List<MovieDTO>> searchMovies(@RequestParam String query) {
        List<MovieDTO> results = movieService.searchMovies(query);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/fastsearch")
    public ResponseEntity<List<MovieDTO>> fastSearchMovies(@RequestParam String query) {
        List<MovieDTO> results = movieService.searchMovies(query);
        System.out.println("Fast search for: " + query + ", found: " + results.size() + " results");
        return ResponseEntity.ok(results);
    }

    public static MovieDTO convertToDTO(Movie movie) {
        MovieDTO dto = new MovieDTO();
        dto.setId(movie.getId());
        dto.setTitle(movie.getTitle());
        dto.setReleaseYear(movie.getReleaseYear());
        dto.setDescription(movie.getDescription());
        dto.setDirector(movie.getDirector());
        dto.setRating(movie.getRating());
        dto.setDuration(movie.getDuration());
        dto.setSource(movie.getSource());
        dto.setPoster(movie.getPosterPath());
        dto.setPopularity(movie.getPopularity());
        // Convert genres to DTOs

        List<GenreDTO> genreDTOs = movie.getGenres().stream()
                .map(genre -> new GenreDTO(genre.getId(), genre.getName()))
                .collect(Collectors.toList());
        dto.setGenres(genreDTOs);

        return dto;
    }


}