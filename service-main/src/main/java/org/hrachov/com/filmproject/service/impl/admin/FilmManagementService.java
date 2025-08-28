package org.hrachov.com.filmproject.service.impl.admin;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.units.qual.A;
import org.hrachov.com.filmproject.exception.FilmNotFoundException;
import org.hrachov.com.filmproject.exception.OmdbException;
import org.hrachov.com.filmproject.exception.SerialNotFoundException;
import org.hrachov.com.filmproject.model.Film;
import org.hrachov.com.filmproject.model.Genre;
import org.hrachov.com.filmproject.model.Season;
import org.hrachov.com.filmproject.model.Serial;
import org.hrachov.com.filmproject.model.dto.FilmDTO;
import org.hrachov.com.filmproject.model.dto.GenreDTO;
import org.hrachov.com.filmproject.model.dto.OmdbDTO;
import org.hrachov.com.filmproject.repository.jpa.*;
import org.hrachov.com.filmproject.service.BlockService;
import org.hrachov.com.filmproject.service.FilmService;
import org.hrachov.com.filmproject.service.impl.ExternalApiService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class FilmManagementService {
    private final GenreRepository genreRepository;
    private final FilmService filmService;
    private final FilmRepository filmRepository;
    private final SeasonRepository seasonRepository;
    private final ExternalApiService externalApiService;
    private final SerialRepository serialRepository;

    public FilmDTO getFilmDataFromExternalApi(String title) {
        OmdbDTO omdbDTO = externalApiService.getFilmFromOmdb(title);

        if (omdbDTO == null || "False".equals(omdbDTO.getResponse())) {
            throw new OmdbException(title);
        }
        if(omdbDTO.getType().equals("series")){
            omdbDTO.setType("serial");
        }

        return convertOmdbToFilmDTO(omdbDTO);
    }


    private FilmDTO convertOmdbToFilmDTO(OmdbDTO omdbDTO) {
        FilmDTO filmDTO = new FilmDTO();
        filmDTO.setTitle(omdbDTO.getTitle());
        filmDTO.setDescription(omdbDTO.getDescription());
        filmDTO.setPoster(omdbDTO.getPoster());
        filmDTO.setDirector(omdbDTO.getDirector());
        filmDTO.setType(omdbDTO.getType() != null ? omdbDTO.getType().toLowerCase() : null);

        setReleaseYear(filmDTO, omdbDTO);
        setRating(filmDTO, omdbDTO);
        setTypeSpecificData(filmDTO, omdbDTO);
        setGenres(filmDTO, omdbDTO);

        return filmDTO;
    }

    private void setReleaseYear(FilmDTO filmDTO, OmdbDTO omdbDTO) {
        if (omdbDTO.getReleaseYear() != null && omdbDTO.getReleaseYear().matches("\\d{4}")) {
            filmDTO.setReleaseYear(Integer.parseInt(omdbDTO.getReleaseYear()));
        }
    }

    private void setRating(FilmDTO filmDTO, OmdbDTO omdbDTO) {
        if (omdbDTO.getImdbRating() != null && !omdbDTO.getImdbRating().equalsIgnoreCase("N/A")) {
            try {
                filmDTO.setRating(Double.parseDouble(omdbDTO.getImdbRating()));
            } catch (NumberFormatException e) {
                // Rating остается null
            }
        }
    }

    private void setTypeSpecificData(FilmDTO filmDTO, OmdbDTO omdbDTO) {
        if ("serial".equalsIgnoreCase(filmDTO.getType())) {
            setSeriesData(filmDTO, omdbDTO);
        } else {
            setMovieData(filmDTO, omdbDTO);
        }
    }

    private void setSeriesData(FilmDTO filmDTO, OmdbDTO omdbDTO) {
        if (omdbDTO.getTotalSeasons() != null && !omdbDTO.getTotalSeasons().equalsIgnoreCase("N/A")) {
            try {
                filmDTO.setNumberOfSeasons(Integer.parseInt(omdbDTO.getTotalSeasons()));
            } catch (NumberFormatException e) {
                // NumberOfSeasons остается null
            }
        }
    }

    private void setMovieData(FilmDTO filmDTO, OmdbDTO omdbDTO) {
        if (omdbDTO.getDuration() != null && !omdbDTO.getDuration().equalsIgnoreCase("N/A")) {
            String durationStr = omdbDTO.getDuration().replaceAll("[^0-9]", "");
            if (!durationStr.isEmpty()) {
                try {
                    filmDTO.setDuration(Integer.parseInt(durationStr));
                } catch (NumberFormatException e) {
                    // Duration остается null
                }
            }
        }
        filmDTO.setSource("movie.mp4");
    }

    private void setGenres(FilmDTO filmDTO, OmdbDTO omdbDTO) {
        List<GenreDTO> genreDTOs = new ArrayList<>();
        if (omdbDTO.getGenres() != null && !omdbDTO.getGenres().equalsIgnoreCase("N/A")) {
            String[] genres = omdbDTO.getGenres().split(",");
            genreDTOs = Arrays.stream(genres)
                    .map(String::trim)
                    .map(this::processGenre)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        filmDTO.setGenres(genreDTOs);
    }

    private GenreDTO processGenre(String genreName) {
        Genre existingGenre = genreRepository.findByName(genreName).orElse(null);
        if (existingGenre != null) {
            return new GenreDTO(existingGenre.getId(), existingGenre.getName());
        }

        try {
            Genre newGenre = new Genre();
            newGenre.setName(genreName);
            Genre savedGenre = genreRepository.save(newGenre);
            return new GenreDTO(savedGenre.getId(), savedGenre.getName());
        } catch (DataIntegrityViolationException e) {
            // Жанр уже создан в параллельном запросе
            existingGenre = genreRepository.findByName(genreName)
                    .orElseThrow(() -> new RuntimeException("Ошибка при создании жанра: " + genreName));
            return new GenreDTO(existingGenre.getId(), existingGenre.getName());
        }
    }

    @Transactional
    public FilmDTO createFilm(FilmDTO filmDTO) {
        filmDTO.setPopularity(10000.0);
        System.out.println("Слздаем фильм " + filmDTO.toString());

        Film savedFilm = filmService.saveFilmFromDTO(filmDTO);
        if(filmDTO.getSeasons() != null && filmDTO.getSeasons() > 0){
            Serial serial = serialRepository.findById(savedFilm.getId())
                    .orElseThrow(()->new SerialNotFoundException(savedFilm.getId()," exception thrown in create film (Possible transaction issue)"));

            for (int i = 1; i <= filmDTO.getSeasons(); i++) {
                Season season = new Season();
                season.setSerial(serial);
                season.setSeasonNumber(i);
                log.info("Saving season " + i + " of " + filmDTO.getSeasons() + " to serial " + serial);
                seasonRepository.save(season);
            }
        }
        System.out.println("Вот что сохраниили" + savedFilm.toString());
        return filmService.convertToFilmDTO(savedFilm);
    }

    public List<FilmDTO> getAllFilms() {
        List<Film> films = filmRepository.findAll();
        return films.stream()
                .map(filmService::convertToFilmDTO)
                .collect(Collectors.toList());
    }

    public FilmDTO getFilmById(Long id) {
        Film film = filmRepository.findById(id)
                .orElseThrow(() -> new FilmNotFoundException(id));
        return filmService.convertToFilmDTO(film);
    }

    @Transactional
    public FilmDTO updateFilm(Long id, FilmDTO filmDTO) {
        Film updatedFilm = filmService.updateFilmFromDTO(id, filmDTO);
        return filmService.convertToFilmDTO(updatedFilm);
    }

    @Transactional
    public void deleteFilm(Long id) {
        if (!filmRepository.existsById(id)) {
            throw new FilmNotFoundException(id);
            //throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Film not found");
        }
        filmRepository.deleteById(id);
    }
}
