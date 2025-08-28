package org.hrachov.com.filmproject.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hrachov.com.filmproject.model.*;
import org.hrachov.com.filmproject.model.dto.EpisodeDTO;
import org.hrachov.com.filmproject.model.dto.FilmDTO;
import org.hrachov.com.filmproject.model.dto.GenreDTO;
import org.hrachov.com.filmproject.model.dto.SeasonDTO;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public interface FilmService {
     List<Film> getAllFilms();

     Film getFilmById(long id);

     List<FilmDTO> getAllNewFilms();

     List<FilmDTO> getFilmsByRevelence() throws JsonProcessingException;
     Film findById(Long id);
     Film saveFilmFromDTO(FilmDTO dto);
     Film updateFilmFromDTO(Long id, FilmDTO dto);
      FilmDTO convertToFilmDTO(Film film);
      SeasonDTO convertToSeasonDTO(Season season);
      EpisodeDTO convertToEpisodeDTO(Episode episode);
      Season saveSeasonFromDTO(SeasonDTO seasonDTO);
      Season updateSeasonFromDTO(Long seasonId, SeasonDTO seasonDTO);
      void deleteSeasonById(Long seasonId);
      Episode saveEpisodeFromDTO(EpisodeDTO episodeDTO);
      Episode updateEpisodeFromDTO(Long episodeId, EpisodeDTO episodeDTO);
      void deleteEpisodeById(Long episodeId);
}