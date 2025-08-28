package org.hrachov.com.filmproject.service;

import org.hrachov.com.filmproject.model.Movie;
import org.hrachov.com.filmproject.model.dto.MovieDTO;
import org.hrachov.com.filmproject.model.dto.PagedResponseDTO;
import org.springframework.data.jpa.domain.Specification;


import java.util.*;
public interface MovieService {
    MovieDTO getMovieDtoById(long id);
    List<Movie> getAllMovies();
    Movie getMovieById(long id);
    List<Movie> getMovieByTitle(String title);
    List<Movie> getAllMovies(String sortBy, String sortDir);
    List<MovieDTO> getAllMoviesByTitle(String title);
    PagedResponseDTO<MovieDTO> getAllMoviesWithPageable(String sortBy, String sortDir, int page, int size, String genre);
    List<MovieDTO> getAllMoviesForCarousel();
    void saveMovie(Movie movie);
    List<MovieDTO> searchMovies(String query);
    void saveSearchQuery(String query);
    List<String> getRecentSearches();
    void initRedis();
}