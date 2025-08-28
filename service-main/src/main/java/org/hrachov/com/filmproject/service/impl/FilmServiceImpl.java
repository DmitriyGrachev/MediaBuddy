package org.hrachov.com.filmproject.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.exception.FilmNotFoundException;
import org.hrachov.com.filmproject.model.*;
import org.hrachov.com.filmproject.model.dto.EpisodeDTO;
import org.hrachov.com.filmproject.model.dto.FilmDTO;
import org.hrachov.com.filmproject.model.dto.GenreDTO;
import org.hrachov.com.filmproject.model.dto.SeasonDTO;
import org.hrachov.com.filmproject.repository.jpa.*;
import org.hrachov.com.filmproject.service.FilmService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import redis.clients.jedis.JedisPooled;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.hrachov.com.filmproject.model.specification.FilmSpecification.getAllFilmsNew;

@Service
@AllArgsConstructor
public class FilmServiceImpl implements FilmService {
    private final FilmRepository filmRepository;
    private final ObjectMapper objectMapper;
    private final JedisPooled jedisPooled;

    public List<Film> getAllFilms() {
        List<Film> films = filmRepository.findAll();
        return films;
    }

    @Override
    public List<FilmDTO> getFilmsByRevelence() throws JsonProcessingException {
        //TODO сделать оптимизацию JPQL, птомучто брать все фильмы при помощи findall() как-то через чур + убрать многопоточку потому что на маленьких данных смысла нет
        long startTime = System.nanoTime();

        String key = "films:revelence:general";
        String value = jedisPooled.get(key);
        if (value != null) {
            long endTime = System.nanoTime();
            System.out.println("Из Redis: " + (endTime - startTime) / 1_000_000 + " мс");
            return objectMapper.readValue(value, new TypeReference<List<FilmDTO>>() {});
        }

        List<Film> unSortedList = new CopyOnWriteArrayList<>(filmRepository.findAll());

        double minPopularity = 0;
        double maxPopularity = 0;
        long minDate = 0;
        long maxDate = 0;

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        try {
            Callable<Double> minPopularityTask = () -> unSortedList.stream().mapToDouble(Film::getPopularity).min().orElse(0);
            Callable<Double> maxPopularityTask = () -> unSortedList.stream().mapToDouble(Film::getPopularity).max().orElse(1);
            Callable<Long> minDateTask = () -> unSortedList.stream().mapToLong(Film::getReleaseYear).min().orElse(0L);
            Callable<Long> maxDateTask = () -> unSortedList.stream().mapToLong(Film::getReleaseYear).max().orElse(1L);

            Future<Double> minPopularityFuture = executorService.submit(minPopularityTask);
            Future<Double> maxPopularityFuture = executorService.submit(maxPopularityTask);
            Future<Long> minDateFuture = executorService.submit(minDateTask);
            Future<Long> maxDateFuture = executorService.submit(maxDateTask);

            minPopularity = minPopularityFuture.get();
            maxPopularity = maxPopularityFuture.get();
            minDate = minDateFuture.get();
            maxDate = maxDateFuture.get();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }

        double alpha = 0.5;

        double finalMinPopularity = minPopularity;
        double finalMaxPopularity = maxPopularity;
        long finalMinDate = minDate;
        long finalMaxDate = maxDate;

        List<Film> sortedFilms = unSortedList.stream()
                .sorted((m1, m2) -> {
                    double popNorm1 = (m1.getPopularity() - finalMinPopularity) / (finalMaxPopularity - finalMinPopularity + 1e-6);
                    double popNorm2 = (m2.getPopularity() - finalMinPopularity) / (finalMaxPopularity - finalMinPopularity + 1e-6);

                    double dateNorm1 = 1.0 - (m1.getReleaseYear() - finalMinDate) / (double)(finalMaxDate - finalMinDate + 1);
                    double dateNorm2 = 1.0 - (m2.getReleaseYear() - finalMinDate) / (double)(finalMaxDate - finalMinDate + 1);

                    double relevance1 = alpha * popNorm1 + (1 - alpha) * dateNorm1;
                    double relevance2 = alpha * popNorm2 + (1 - alpha) * dateNorm2;

                    return Double.compare(relevance2, relevance1); // По убыванию
                })
                .limit(10)
                .collect(Collectors.toList());

        List<FilmDTO> dtos = sortedFilms.stream()
                .map(film -> {
                    FilmDTO dto = objectMapper.convertValue(film, FilmDTO.class);
                    if (film instanceof Movie movie) {
                        dto.setType("movie");
                        dto.setPoster(movie.getPosterPath());
                    } else if (film instanceof Serial serial) {
                        dto.setType("serial");
                        dto.setPoster(serial.getPoster() != null ? serial.getPoster() : "/images/inception.jpg");
                    } else {
                        dto.setType("film");
                        dto.setPoster("/images/default.jpg");
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        jedisPooled.setex(key, 60, objectMapper.writeValueAsString(dtos));

        long endTime = System.nanoTime();
        System.out.println("Время выполнения: " + (endTime - startTime) / 1_000_000 + " мс");

        return dtos;
    }

    public Film getFilmById(long id) {

        return filmRepository.findById(id).orElse(null);
    }public List<FilmDTO> getAllNewFilms() {
        //TODO Критериа по releaseYear а не date added стоит задуматься над полем когда добавили фильм/cериал
        Specification<Film> spec = getAllFilmsNew();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "releaseYear"));
        Page<Film> page = filmRepository.findAll(spec, pageable);
        List<Film> films = page.getContent();
        System.out.println("Found " + films.size() + " new films");
        return films.stream()
                .map(film -> {
                    FilmDTO dto = objectMapper.convertValue(film, FilmDTO.class);
                    if (film instanceof Movie movie) {
                        dto.setType("movie");
                        dto.setPoster(movie.getPosterPath());
                    } else if (film instanceof Serial serial) {
                        dto.setType("serial");
                        dto.setPoster(serial.getPoster() != null ? serial.getPoster() : "/images/inception.jpg");
                    } else {
                        dto.setType("film");
                        dto.setPoster("/images/default.jpg");
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }
    @Override
    public Film findById(Long id) {
        return filmRepository.findById(id).orElse(null);
    }

    private void updateFilmEntity(Film film, FilmDTO dto) {
        film.setTitle(dto.getTitle());
        film.setReleaseYear(dto.getReleaseYear());
        film.setDescription(dto.getDescription());
        film.setRating(dto.getRating());
        film.setDirector(dto.getDirector());
        // Логика для жанров и популярности должна быть добавлена здесь
    }
    private final MovieRepository movieRepository;
    private final SerialRepository serialRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final GenreRepository genreRepository;


    // Helper to convert Genre names to Genre entities
    private Set<Genre> getOrCreateGenres(List<GenreDTO> genreDTOs) {
        Set<Genre> genres = new HashSet<>();
        if (genreDTOs != null) {
            for (GenreDTO dto : genreDTOs) {
                genreRepository.findByName(dto.getName())
                        .ifPresentOrElse(
                                genres::add,
                                () -> genres.add(genreRepository.save(new Genre(dto.getName())))
                        );
            }
        }
        return genres;
    }

    // Convert Film entity to FilmDTO (for GET requests, especially detail)
    public FilmDTO convertToFilmDTO(Film film) {
        FilmDTO dto = new FilmDTO();
        dto.setId(film.getId());
        dto.setTitle(film.getTitle());
        dto.setReleaseYear(film.getReleaseYear());
        dto.setDescription(film.getDescription());
        dto.setDirector(film.getDirector()); // Director is on Film
        dto.setRating(film.getRating());
        //dto.setPoster(film.getPoster()); // Poster is now on Movie/Serial for consistency
        dto.setPopularity(film.getPopularity()); // Popularity is on Film

        dto.setGenres(film.getGenres().stream()
                .map(g -> new GenreDTO(g.getId(), g.getName()))
                .collect(Collectors.toList()));

        if (film instanceof Movie) {
            Movie movie = (Movie) film;
            dto.setType("movie");
            dto.setDuration(movie.getDuration());
            dto.setSource(movie.getSource());
            dto.setPoster(movie.getPosterPath()); // Set poster from movie entity
        } else if (film instanceof Serial) {
            Serial serial = (Serial) film;
            dto.setType("serial");
            dto.setNumberOfSeasons(serial.getNumberOfSeasons());
            dto.setTotalEpisodes(serial.getTotalEpisodes());
            dto.setPoster(serial.getPoster()); // Set poster from serial entity
            dto.setSeasonList(serial.getSeasonList().stream()
                    .map(this::convertToSeasonDTO)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    // Convert Season entity to SeasonDTO
    public SeasonDTO convertToSeasonDTO(Season season) {
        return SeasonDTO.builder()
                .id(season.getId())
                .seasonNumber(season.getSeasonNumber())
                .title(season.getTitle())
                .serialFilmId(season.getSerial().getId())
                .episodeList(season.getEpisodeList().stream()
                        .map(this::convertToEpisodeDTO)
                        .collect(Collectors.toList()))
                .build();
    }

    // Convert Episode entity to EpisodeDTO
    public EpisodeDTO convertToEpisodeDTO(Episode episode) {
        return EpisodeDTO.builder()
                .id(episode.getId())
                .episodeNumber(episode.getEpisodeNumber())
                .title(episode.getTitle())
                .source(episode.getSource())
                .duration(episode.getDuration())
                .seasonId(episode.getSeason().getId())
                .build();
    }


    @Transactional
    public Film saveFilmFromDTO(FilmDTO filmDTO) {
        Film film;
        Set<Genre> genres = getOrCreateGenres(filmDTO.getGenres());

        if ("movie".equalsIgnoreCase(filmDTO.getType())) {
            Movie movie = new Movie();
            // Map common fields from FilmDTO
            movie.setTitle(filmDTO.getTitle());
            movie.setReleaseYear(filmDTO.getReleaseYear());
            movie.setDescription(filmDTO.getDescription());
            movie.setDirector(filmDTO.getDirector());
            movie.setRating(filmDTO.getRating());
            movie.setPopularity(filmDTO.getPopularity());
            movie.setGenres(genres);

            // Map movie-specific fields
            movie.setDuration(filmDTO.getDuration());
            movie.setSource(filmDTO.getSource()); // Set source for movie
            String poster = downLoadPoster(filmDTO.getPoster(), filmDTO.getTitle());
            movie.setPosterPath(poster); // Movie has its own poster

            film = movieRepository.save(movie);
            //TODO IMPORTANT SERIAL = SERIES
        } else if ("serial".equalsIgnoreCase(filmDTO.getType())) {
            Serial serial = new Serial();
            // Map common fields from FilmDTO
            serial.setTitle(filmDTO.getTitle());
            serial.setReleaseYear(filmDTO.getReleaseYear());
            serial.setDescription(filmDTO.getDescription());
            serial.setDirector(filmDTO.getDirector());
            serial.setRating(filmDTO.getRating());
            serial.setPopularity(filmDTO.getPopularity());
            serial.setGenres(genres);

            // Map serial-specific fields
            serial.setNumberOfSeasons(filmDTO.getNumberOfSeasons()); // Can be initial, will be updated when seasons are added
            serial.setTotalEpisodes(filmDTO.getTotalEpisodes());
            String poster = downLoadPoster(filmDTO.getPoster(), filmDTO.getTitle());
            serial.setPoster(poster); // Serial has its own poster

            film = serialRepository.save(serial);
        } else {
            throw new IllegalArgumentException("Неизвестный тип фильма: " + filmDTO.getType());
        }
        return film;
    }
    public String downLoadPoster(String posterUrl,String title) {
        try {
            // Clean file name (e.g., remove slashes, colons, etc.)
            String safeTitle = title.replaceAll("[^a-zA-Z0-9\\-_]", "_");

            // Define the static images path relative to project structure
            String imagesDirPath = "src/main/resources/static/images/";
            File imagesDir = new File(imagesDirPath);

            if (!imagesDir.exists()) {
                imagesDir.mkdirs(); // Create the directory if it doesn't exist
            }

            // Build the output file path
            File outputFile = new File(imagesDir, safeTitle + ".jpg");

            // Download and save the image
            URL url = new URL(posterUrl);
            BufferedImage bufferedImage = ImageIO.read(url);
            ImageIO.write(bufferedImage, "jpg", outputFile);

            // Return the relative path used in frontend (accessible via /images/...)
            if (outputFile.exists() && outputFile.length() > 0) {
                return "/images/" + safeTitle + ".jpg";
            } else {
                throw new IOException("Файл не был сохранён: " + outputFile.getAbsolutePath());
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Transactional
    public Film updateFilmFromDTO(Long id, FilmDTO filmDTO) {
        Film existingFilm = filmRepository.findById(id)
                .orElseThrow(() -> new FilmNotFoundException(id));

        Set<Genre> genres = getOrCreateGenres(filmDTO.getGenres());

        // Update common fields
        existingFilm.setTitle(filmDTO.getTitle());
        existingFilm.setReleaseYear(filmDTO.getReleaseYear());
        existingFilm.setDescription(filmDTO.getDescription());
        existingFilm.setDirector(filmDTO.getDirector());
        existingFilm.setRating(filmDTO.getRating());
        existingFilm.setPopularity(filmDTO.getPopularity());
        existingFilm.setGenres(genres); // Update genres

        if ("movie".equalsIgnoreCase(filmDTO.getType()) && existingFilm instanceof Movie) {
            Movie movie = (Movie) existingFilm;
            movie.setDuration(filmDTO.getDuration());
            movie.setSource(filmDTO.getSource()); // Update source for movie
            movie.setPosterPath(filmDTO.getPoster());
            return movieRepository.save(movie);
        } else if ("serial".equalsIgnoreCase(filmDTO.getType()) && existingFilm instanceof Serial) {
            Serial serial = (Serial) existingFilm;
            serial.setNumberOfSeasons(filmDTO.getNumberOfSeasons()); // Update if manually set, or let entity manage
            serial.setTotalEpisodes(filmDTO.getTotalEpisodes());
            serial.setPoster(filmDTO.getPoster());
            return serialRepository.save(serial);
        } else {
            // Handle type mismatch or invalid type
            throw new IllegalArgumentException("Тип обновляемого контента не совпадает или недействителен.");
        }
    }

    // New methods for Season and Episode management

    @Transactional
    public Season saveSeasonFromDTO(SeasonDTO seasonDTO) {
        Serial serial = serialRepository.findById(seasonDTO.getSerialFilmId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сериал не найден с ID: " + seasonDTO.getSerialFilmId()));

        Season season = new Season();
        season.setSeasonNumber(seasonDTO.getSeasonNumber());
        season.setTitle(seasonDTO.getTitle());
        season.setSerial(serial);

        Season savedSeason = seasonRepository.save(season);
        serial.addSeason(savedSeason); // Update serial's season list and count
        serialRepository.save(serial); // Save updated serial
        return savedSeason;
    }

    @Transactional
    public Season updateSeasonFromDTO(Long seasonId, SeasonDTO seasonDTO) {
        Season existingSeason = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон не найден с ID: " + seasonId));

        existingSeason.setSeasonNumber(seasonDTO.getSeasonNumber());
        existingSeason.setTitle(seasonDTO.getTitle());

        return seasonRepository.save(existingSeason);
    }

    @Transactional
    public void deleteSeasonById(Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон не найден с ID: " + seasonId));
        Serial serial = season.getSerial();
        serial.removeSeason(season); // Remove season from serial's list
        serialRepository.save(serial); // Save updated serial (updates numberOfSeasons)
        seasonRepository.delete(season);
    }

    @Transactional
    public Episode saveEpisodeFromDTO(EpisodeDTO episodeDTO) {
        Season season = seasonRepository.findById(episodeDTO.getSeasonId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Сезон не найден с ID: " + episodeDTO.getSeasonId()));

        Episode episode = new Episode();
        episode.setEpisodeNumber(episodeDTO.getEpisodeNumber());
        episode.setTitle(episodeDTO.getTitle());
        episode.setSource(episodeDTO.getSource());
        episode.setDuration(episodeDTO.getDuration());
        episode.setSeason(season);

        Episode savedEpisode = episodeRepository.save(episode);
        season.addEpisode(savedEpisode); // Update season's episode list
        seasonRepository.save(season); // Save updated season
        // Consider updating totalEpisodes on Serial if needed
        return savedEpisode;
    }

    @Transactional
    public Episode updateEpisodeFromDTO(Long episodeId, EpisodeDTO episodeDTO) {
        Episode existingEpisode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Эпизод не найден с ID: " + episodeId));

        existingEpisode.setEpisodeNumber(episodeDTO.getEpisodeNumber());
        existingEpisode.setTitle(episodeDTO.getTitle());
        existingEpisode.setSource(episodeDTO.getSource());
        existingEpisode.setDuration(episodeDTO.getDuration());

        return episodeRepository.save(existingEpisode);
    }

    @Transactional
    public void deleteEpisodeById(Long episodeId) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Эпизод не найден с ID: " + episodeId));
        Season season = episode.getSeason();
        season.removeEpisode(episode); // Remove episode from season's list
        seasonRepository.save(season); // Save updated season
        episodeRepository.delete(episode);
    }
}
