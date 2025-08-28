package org.hrachov.com.filmproject.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.jfr.ContentType;
import org.hrachov.com.filmproject.model.Comment;
import org.hrachov.com.filmproject.model.Genre;
import org.hrachov.com.filmproject.model.Movie;
import org.hrachov.com.filmproject.model.dto.MovieDTO;
import org.hrachov.com.filmproject.security.JwtUtils;
import org.hrachov.com.filmproject.service.CommentService;
import org.hrachov.com.filmproject.service.MovieService;
import org.hrachov.com.filmproject.service.impl.CommentServiceImpl;
import org.hrachov.com.filmproject.service.impl.MovieServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MovieController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private MovieServiceImpl movieService;

    @MockBean
    private CommentServiceImpl commentService;

    private final ObjectMapper mapper = new ObjectMapper();
    @MockBean
    private RestTemplate restTemplate;

    @Test
    void testGetMovieByID_ShouldReturnMovie() throws Exception {
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

        MovieDTO movieDTO = new MovieDTO();
        movieDTO.setId(1L);
        movieDTO.setTitle("Interstellar");
        movieDTO.setDescription("Exploring space and time");
        movieDTO.setDirector("Christopher Nolan");
        movieDTO.setRating(8.6);
        movieDTO.setPopularity(92.0);
        movieDTO.setReleaseYear(2014);
        movieDTO.setDuration(169);
        movieDTO.setSource("http://streaming.example.com/interstellar");

        Comment comment = new Comment();
        comment.setId(1L);

        List<Comment> comments = List.of(
                comment
        );
        when(movieService.getMovieDtoById(1L)).thenReturn(movieDTO);
        when(commentService.getCommentsByMovie(movieDTO.getId())).thenReturn(comments);

        mockMvc.perform(get("/api/movie/{id}", movieDTO.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Interstellar"))
                .andExpect(jsonPath("$.description").value("Exploring space and time"))
                .andExpect(jsonPath("$.comments.[0].id").value(1L));
    }

    @Test
    @DisplayName("Test getMovie if null should throw movie not found in db")
    void testGetMovie_if_null_shouldThrowException() throws Exception {
        when(movieService.getMovieDtoById(1L)).thenReturn(null);
        mockMvc.perform(get("/api/movie/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Movie not found"))
                .andExpect(jsonPath("$.error").value("Movie not found in DB"));
    }
    @Test
    @DisplayName("stream корректного файла → 200 OK и вызов RestTemplate.execute")
    void stream_ValidPath_ShouldProxyToRestTemplate() throws Exception {
        // подготовим заглушку: любое выполнение execute просто ничего не делает
        doAnswer(invocation -> null)
                .when(restTemplate)
                .execute(
                        any(URI.class),
                        eq(HttpMethod.GET),
                        any(RequestCallback.class),
                        any(ResponseExtractor.class)
                );

        // выполним запрос к нашему контроллеру
        mockMvc.perform(get("/api/movie/stream/trailer.mp4"))
                .andExpect(status().isOk());

        // проверяем, что URI построился правильно
        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate, times(1)).execute(
                uriCaptor.capture(),
                eq(HttpMethod.GET),
                any(RequestCallback.class),
                any(ResponseExtractor.class)
        );

        URI calledUri = uriCaptor.getValue();
        assertEquals("http://localhost:8083/api/stream/movies/trailer.mp4",
                calledUri.toString());
    }
}
