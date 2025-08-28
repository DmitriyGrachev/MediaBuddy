package org.hrachov.com.filmproject.controller;
import org.hrachov.com.filmproject.model.dto.MovieDTO;
import org.hrachov.com.filmproject.model.dto.PagedResponseDTO;
import org.hrachov.com.filmproject.repository.jpa.MovieRepository;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.security.JwtUtils;
import org.hrachov.com.filmproject.service.MovieService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = MoviesController.class)
@AutoConfigureMockMvc(addFilters = false)
public class MoviesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovieService movieService;

    // MovieRepository можно не мокать, если он не используется напрямую в контроллере,
    // но лучше добавить для полноты картины
    @MockBean
    private MovieRepository movieRepository;

    // Обязательные моки
    @MockBean
    private UserDetailsService userDetailsService;
    @MockBean
    private JwtUtils jwtUtils;
    @MockBean
    private CurrentUserService currentUserService;

    @Test
    void getMovies_shouldReturnPagedResponse() throws Exception {
        // Arrange
        MovieDTO movie = new MovieDTO();
        movie.setId(1L);
        movie.setTitle("Inception");

        PagedResponseDTO<MovieDTO> pagedResponse = new PagedResponseDTO<>();
        pagedResponse.setContent(List.of(movie));
        pagedResponse.setPage(0);
        pagedResponse.setSize(10);
        pagedResponse.setTotalElements(1L);
        pagedResponse.setTotalPages(1);

        when(movieService.getAllMoviesWithPageable("title", "asc", 0, 10, null))
                .thenReturn(pagedResponse);

        // Act & Assert
        mockMvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].title").value("Inception"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void getTenMovies_shouldReturnListOfMoviesForCarousel() throws Exception {
        // Arrange
        MovieDTO movie1 = new MovieDTO();
        movie1.setId(1L);
        movie1.setTitle("Movie 1");

        MovieDTO movie2 = new MovieDTO();
        movie2.setId(2L);
        movie2.setTitle("Movie 2");

        when(movieService.getAllMoviesForCarousel()).thenReturn(List.of(movie1, movie2));

        // Act & Assert
        mockMvc.perform(get("/api/movies/carousel"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Movie 1"));
    }

    @Test
    void searchMovies_shouldReturnListOfFoundMovies() throws Exception {
        // Arrange
        String query = "Matrix";
        MovieDTO movie = new MovieDTO();
        movie.setId(1L);
        movie.setTitle("The Matrix");

        when(movieService.searchMovies(query)).thenReturn(List.of(movie));

        // Act & Assert
        mockMvc.perform(get("/api/movies/search").param("query", query))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("The Matrix"));
    }

    @Test
    void fastSearchMovies_shouldReturnListOfFoundMovies() throws Exception {
        // Arrange
        String query = "Fast";
        MovieDTO movie = new MovieDTO();
        movie.setId(1L);
        movie.setTitle("Fast & Furious");

        when(movieService.searchMovies(query)).thenReturn(List.of(movie));

        // Act & Assert
        mockMvc.perform(get("/api/movies/fastsearch").param("query", query))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Fast & Furious"));
    }
}