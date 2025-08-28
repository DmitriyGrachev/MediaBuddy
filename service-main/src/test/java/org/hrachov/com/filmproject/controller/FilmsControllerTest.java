package org.hrachov.com.filmproject.controller;

import org.hrachov.com.filmproject.model.dto.FilmDTO;
import org.hrachov.com.filmproject.repository.jpa.FilmRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.security.JwtUtils;
import org.hrachov.com.filmproject.service.impl.FilmServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


//TODO Убераю фильтры, стоит написать конфиг для тестирования аутентификации
@WebMvcTest(FilmsController.class)
@AutoConfigureMockMvc(addFilters = false) // отключает Security фильтры
class FilmsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private FilmServiceImpl filmService;



    @Test
    void testShowFilms() throws Exception {
        // Your existing 'when' block
        when(filmService.getAllNewFilms()).thenReturn(List.of(
                FilmDTO.builder().id(1L).build(),
                FilmDTO.builder().id(2L).build(),
                FilmDTO.builder().id(3L).build(),
                FilmDTO.builder().id(4L).build()
        ));


        mockMvc.perform(get("/api/films/newFilms"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[2].id").value(3))
                .andExpect(jsonPath("$[3].id").value(4));
    }
    @Test
    void testCarouselFilms() throws Exception {
        when(filmService.getAllNewFilms()).thenReturn(List.of(
                FilmDTO.builder().id(1L).build(),
                FilmDTO.builder().id(2L).build(),
                FilmDTO.builder().id(3L).build(),
                FilmDTO.builder().id(4L).build()
        ));

        mockMvc.perform(get("/api/films/newFilms"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[2].id").value(3))
                .andExpect(jsonPath("$[3].id").value(4));
    }

}