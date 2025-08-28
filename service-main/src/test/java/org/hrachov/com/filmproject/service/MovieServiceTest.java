package org.hrachov.com.filmproject.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hrachov.com.filmproject.exception.MoviesNotFound;
import org.hrachov.com.filmproject.model.Genre;
import org.hrachov.com.filmproject.model.Movie;
import org.hrachov.com.filmproject.model.dto.MovieDTO;
import org.hrachov.com.filmproject.model.dto.PagedResponseDTO;
import org.hrachov.com.filmproject.repository.jpa.MovieRepository;
import org.hrachov.com.filmproject.service.impl.MovieServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.SearchResult;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MovieServiceTest {
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private JedisPooled jedis;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MovieServiceImpl movieService;

    @BeforeEach
    void setUp() {
        movieService = new MovieServiceImpl(movieRepository, jedis, objectMapper);
    }

    @Test
    void getAllMovies() {
        Genre action = new Genre();
        action.setId(1L);
        action.setName("Action");

        Genre drama = new Genre();
        drama.setId(2L);
        drama.setName("Drama");

        Movie movie1 = new Movie();
        movie1.setId(1L);
        movie1.setTitle("Inception");
        movie1.setDescription("A mind-bending thriller");
        movie1.setDirector("Christopher Nolan");
        movie1.setRating(8.8);
        movie1.setPopularity(95.0);
        movie1.setReleaseYear(2010);
        movie1.setDuration(148);
        movie1.setSource("http://streaming.example.com/inception");
        movie1.setPosterPath("/images/inception.jpg");
        movie1.setUserRatingSum(450L);
        movie1.setUserRatingCount(100L);
        movie1.setGenres(Set.of(action, drama));
        action.getFilms().add(movie1);
        drama.getFilms().add(movie1);

        Movie movie2 = new Movie();
        movie2.setId(2L);
        movie2.setTitle("Interstellar");
        movie2.setDescription("Exploring space and time");
        movie2.setDirector("Christopher Nolan");
        movie2.setRating(8.6);
        movie2.setPopularity(92.0);
        movie2.setReleaseYear(2014);
        movie2.setDuration(169);
        movie2.setSource("http://streaming.example.com/interstellar");
        movie2.setPosterPath("/images/interstellar.jpg");
        movie2.setUserRatingSum(500L);
        movie2.setUserRatingCount(110L);
        movie2.setGenres(Set.of(drama)); // Только Drama
        drama.getFilms().add(movie2);

        when(movieRepository.findAll()).thenReturn(List.of(movie1, movie2));

        List<Movie> movies = movieService.getAllMovies();
        assertNotNull(movies);
        assertEquals(2, movies.size());
        assertEquals(movie1, movies.get(0));
        assertEquals(movie2, movies.get(1));
        assertEquals(movie1.getTitle(), movies.get(0).getTitle());
        assertEquals(movie2.getTitle(), movies.get(1).getTitle());
        assertEquals(movie1.getDescription(), movies.get(0).getDescription());
        assertEquals(movie2.getDescription(), movies.get(1).getDescription());
        assertEquals(movie1.getReleaseYear(), movies.get(0).getReleaseYear());
        assertEquals(movie2.getReleaseYear(), movies.get(1).getReleaseYear());
        assertEquals(movie1.getGenres(), movies.get(0).getGenres());
        assertEquals(movie2.getGenres(), movies.get(1).getGenres());
    }
    @Test
    @DisplayName("Should throw MoviesNotFound exception with status SERVICE_UNAVAILABLE")
    void getAllMovies_ShouldThrowException() {
        when(movieRepository.findAll()).thenReturn(List.of());

        MoviesNotFound moviesNotFound = assertThrows(
                MoviesNotFound.class,
                () -> movieService.getAllMovies()
        );
        assertEquals("No movies found", moviesNotFound.getMessage());
    }
    @Test
    @DisplayName("Testing getMovieByID with EMPTY cache Jedis")
    void getMovieByID() throws JsonProcessingException {
        Genre action = new Genre();
        action.setId(1L);
        action.setName("Action");

        Genre drama = new Genre();
        drama.setId(2L);
        drama.setName("Drama");

        Movie movie1 = new Movie();
        movie1.setId(1L);
        movie1.setTitle("Inception");
        movie1.setDescription("A mind-bending thriller");
        movie1.setDirector("Christopher Nolan");
        movie1.setRating(8.8);
        movie1.setPopularity(95.0);
        movie1.setReleaseYear(2010);
        movie1.setDuration(148);
        movie1.setSource("http://streaming.example.com/inception");
        movie1.setPosterPath("/images/inception.jpg");
        movie1.setUserRatingSum(450L);
        movie1.setUserRatingCount(100L);
        movie1.setGenres(Set.of(action, drama));
        action.getFilms().add(movie1);
        drama.getFilms().add(movie1);

        String key = "movie:id:" + movie1.getId();
        when(jedis.get(key)).thenReturn(null);
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie1));

        Movie movie = movieService.getMovieById(1L);
        assertNotNull(movie);
        assertEquals(movie1, movie);
        assertEquals(movie1.getTitle(), movie.getTitle());

        verify(movieRepository, times(1)).findById(1L);
        verify(jedis, times(1)).get(key);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jedis, times(1)).setex(eq(key), eq(60L), captor.capture());

        MovieDTO movieCached = objectMapper.readValue(captor.getValue(), MovieDTO.class);
        assertEquals(movie1.getTitle(), movieCached.getTitle());
    }
    @Test
    @DisplayName("Testing GetMovieById with CACHE")
    void getMovieById() throws JsonProcessingException {
        Genre action = new Genre();
        action.setId(1L);
        action.setName("Action");

        Genre drama = new Genre();
        drama.setId(2L);
        drama.setName("Drama");

        Movie movie1 = new Movie();
        movie1.setId(1L);
        movie1.setTitle("Inception");
        movie1.setDescription("A mind-bending thriller");
        movie1.setDirector("Christopher Nolan");
        movie1.setRating(8.8);
        movie1.setPopularity(95.0);
        movie1.setReleaseYear(2010);
        movie1.setDuration(148);
        movie1.setSource("http://streaming.example.com/inception");
        movie1.setPosterPath("/images/inception.jpg");
        movie1.setUserRatingSum(450L);
        movie1.setUserRatingCount(100L);
        movie1.setGenres(Set.of(action, drama));
        action.getFilms().add(movie1);
        drama.getFilms().add(movie1);

        String key = "movie:id:" + movie1.getId();
        MovieDTO movieCached = new MovieDTO();
        movieCached.setId(1L);
        movieCached.setTitle("Inception");
        movieCached.setDescription("A mind-bending thriller");
        String value = objectMapper.writeValueAsString(movieCached);

        when(jedis.get(key)).thenReturn(value);

        Movie movie = movieService.getMovieById(1L);
        assertNotNull(movie);
        assertEquals(movieCached.getTitle(), movie.getTitle());
        verify(movieRepository, never()).findById(anyLong()); // Убедились, что в базу НЕ ходили
        verify(jedis, never()).setex(anyString(), anyLong(), anyString()); // И ничего нового в кэш не писали
    }
    @Test
    @DisplayName("Should delete cache key if deserialization fails")
    void getMovieById_shouldDeleteCacheKey_onDeserializationError() {
        long movieId = 1L;
        String key = "movie:id:" + movieId;
        String badJson = "{невалидный-json";

        when(jedis.get(key)).thenReturn(badJson);

        when(movieRepository.findById(movieId)).thenReturn(Optional.empty());

        movieService.getMovieById(movieId);

        verify(jedis, times(1)).get(key);
        verify(jedis, times(1)).del(key);
        verify(movieRepository, times(1)).findById(movieId);
    }
    @Test
    @DisplayName("Get empty movieDtoByID with no cache")
    void getMovieDtoByID() throws JsonProcessingException {
        String key = "movie:id:" + 1L;

        when(jedis.get(key)).thenReturn(null);
        when(movieRepository.findById(1L)).thenReturn(Optional.empty());

        MovieDTO movieDto = movieService.getMovieDtoById(1L);
        assertNull(movieDto);
        verify(jedis, times(1)).get(key);
        verify(movieRepository, times(1)).findById(1L);

    }
    @Test
    @DisplayName("Get movieDto with empty cache")
    void getMovieDtoWithEmptyCache() throws JsonProcessingException {
        Genre action = new Genre();
        action.setId(1L);
        action.setName("Action");

        Genre drama = new Genre();
        drama.setId(2L);
        drama.setName("Drama");

        Movie movie1 = new Movie();
        movie1.setId(1L);
        movie1.setTitle("Inception");
        movie1.setDescription("A mind-bending thriller");
        movie1.setDirector("Christopher Nolan");
        movie1.setRating(8.8);
        movie1.setPopularity(95.0);
        movie1.setReleaseYear(2010);
        movie1.setDuration(148);
        movie1.setSource("http://streaming.example.com/inception");
        movie1.setPosterPath("/images/inception.jpg");
        movie1.setUserRatingSum(450L);
        movie1.setUserRatingCount(100L);
        movie1.setGenres(Set.of(action, drama));
        action.getFilms().add(movie1);
        drama.getFilms().add(movie1);

        String key = "movie:id:" + 1L;

        MovieDTO movieDto = new MovieDTO();
        movieDto.setId(1L);
        movieDto.setTitle("Inception");
        movieDto.setDescription("A mind-bending thriller");

        when(jedis.get(key)).thenReturn(null);
        when(movieRepository.findById(1L)).thenReturn(Optional.of(movie1));

        MovieDTO movieDto1 = movieService.getMovieDtoById(1L);

        String value = objectMapper.writeValueAsString(movieDto);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jedis, times(1)).setex(eq(key), eq(60L), captor.capture());
        MovieDTO movieCached = objectMapper.readValue(captor.getValue(), MovieDTO.class);
        System.out.println(movieCached);
        assertEquals(movieDto.getTitle(), movieDto1.getTitle());
        assertEquals(movieDto.getDescription(), movieDto1.getDescription());
        assertNotNull(movieCached);
    }
    @Test
    @DisplayName("Should return null getMovieDtoById")
    void getMovieDtoById() throws JsonProcessingException {
        String key = "movie:id:" + 1L;

        when(jedis.get(key)).thenReturn(null);
        when(movieRepository.findById(1L)).thenReturn(Optional.empty());

        MovieDTO movieDto = movieService.getMovieDtoById(1L);
        assertNull(movieDto);
        verify(jedis, times(1)).get(key);
        verify(movieRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Get movie by title")
    void getMovieByTitle() throws JsonProcessingException {
        Movie movie = new Movie();
        movie.setId(1L);
        movie.setTitle("Inception");
        movie.setDescription("A mind-bending thriller");

        Movie movie1 = new Movie();
        movie1.setId(2L);
        movie1.setTitle("Inception");
        movie1.setDescription("Ai Matrix revo");

        List<Movie> movies = List.of(
               movie,movie1
        );

        when(movieRepository.findByTitle("Inception")).thenReturn(movies);

        List<Movie> movies1 = movieService.getMovieByTitle("Inception");
        assertNotNull(movies1);
        assertEquals(movies.size(), movies1.size());
    }
    @Test
    @DisplayName("Shoud throw MoviesNotFoundException when not found by title")
    void getMovieByTitleNotFound() throws JsonProcessingException {
        when(movieRepository.findByTitle("Inception")).thenReturn(Collections.emptyList());
        MoviesNotFound moviesNotFound = assertThrows(
                MoviesNotFound.class,
                () -> movieService.getMovieByTitle("Inception")
        );
        assertEquals("No movies found", moviesNotFound.getMessage());
    }

    @Test
    @DisplayName("GetAllMoviesWithPageable")
    void getAllMoviesWithPageable() throws JsonProcessingException {
        String sortBy = "title";
        String sortOrder = "desc";
        int page = 1;
        int pageSize = 5;
        String genre = "Action";

        Genre action = new Genre();
        action.setId(1L);
        action.setName("Action");

        Genre drama = new Genre();
        drama.setId(2L);
        drama.setName("Drama");

        Movie movie1 = new Movie();
        movie1.setId(1L);
        movie1.setTitle("Inception");
        movie1.setDescription("A mind-bending thriller");
        movie1.setDirector("Christopher Nolan");
        movie1.setRating(8.8);
        movie1.setPopularity(95.0);
        movie1.setReleaseYear(2010);
        movie1.setDuration(148);
        movie1.setSource("http://streaming.example.com/inception");
        movie1.setPosterPath("/images/inception.jpg");
        movie1.setUserRatingSum(450L);
        movie1.setUserRatingCount(100L);
        movie1.setGenres(Set.of(action, drama));
        action.getFilms().add(movie1);
        drama.getFilms().add(movie1);

        Movie movie2 = new Movie();
        movie2.setId(2L);
        movie2.setTitle("Interstellar");
        movie2.setDescription("Exploring space and time");
        movie2.setDirector("Christopher Nolan");
        movie2.setRating(8.6);
        movie2.setPopularity(92.0);
        movie2.setReleaseYear(2014);
        movie2.setDuration(169);
        movie2.setSource("http://streaming.example.com/interstellar");
        movie2.setPosterPath("/images/interstellar.jpg");
        movie2.setUserRatingSum(500L);
        movie2.setUserRatingCount(110L);
        movie2.setGenres(Set.of(drama));
        drama.getFilms().add(movie2);

        Page<Movie> page1 = new PageImpl<>(List.of(movie1, movie2));
        Sort.Direction direction = Sort.Direction.fromString(sortOrder.toUpperCase());
        Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortBy));

        // Используем any() для Specification
        when(movieRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page1);

        PagedResponseDTO<MovieDTO> pagedResponseDTO = movieService
                .getAllMoviesWithPageable("title", "desc", 1, 5, "Action");

        assertNotNull(pagedResponseDTO);
        verify(movieRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }
    @Test
    @DisplayName("Should return empty list when query is empty or blank")
    void searchMoviesWithEmptyQuery() {
        List<MovieDTO> result = movieService.searchMovies("   ");
        assertEquals(Collections.emptyList(), result);
        verifyNoInteractions(jedis);
    }

    @Test
    @DisplayName("Should return movies when search finds results")
    void searchMoviesWithValidQuery() {
        // Подготовка данных
        String query = "Inception";
        SearchResult searchResult = mock(SearchResult.class);
        Document doc1 = mock(Document.class);
        when(doc1.getId()).thenReturn("movie:1");
        when(doc1.getString("title")).thenReturn("Inception");
        when(searchResult.getTotalResults()).thenReturn(1L);
        when(searchResult.getDocuments()).thenReturn(List.of(doc1));
        when(jedis.ftSearch(eq("moviesIdx"), any(Query.class))).thenReturn(searchResult);

        // Выполнение
        List<MovieDTO> result = movieService.searchMovies(query);

        // Проверки
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("Inception", result.get(0).getTitle());
        verify(jedis, times(1)).ftSearch(eq("moviesIdx"), any(Query.class));
    }


    @Test
    @DisplayName("Should return empty list when Redis throws exception")
    void searchMoviesWithRedisException() {
        // Подготовка данных
        String query = "Inception";
        when(jedis.ftSearch(eq("moviesIdx"), any(Query.class))).thenThrow(new RuntimeException("Redis error"));

        // Выполнение
        List<MovieDTO> result = movieService.searchMovies(query);

        // Проверки
        assertEquals(Collections.emptyList(), result);
        verify(jedis, times(1)).ftSearch(eq("moviesIdx"), any(Query.class));
    }
}
