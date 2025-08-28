package org.hrachov.com.filmproject.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.exception.MoviesNotFound;
import org.hrachov.com.filmproject.model.dto.GenreDTO;
import org.hrachov.com.filmproject.model.dto.MovieDTO;
import org.hrachov.com.filmproject.model.dto.PagedResponseDTO;
import org.hrachov.com.filmproject.model.Genre;
import org.hrachov.com.filmproject.model.Movie;
import org.hrachov.com.filmproject.repository.jpa.MovieRepository;
import org.hrachov.com.filmproject.service.MovieService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.search.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.hrachov.com.filmproject.model.specification.MovieSpecification.byGenre;


@Service
@AllArgsConstructor
public class MovieServiceImpl implements MovieService {
    private final MovieRepository movieRepository;
    private final JedisPooled jedis;
    private final ObjectMapper objectMapper;


    @Override
    public List<Movie> getAllMovies() {
        List<Movie> movies = movieRepository.findAll();
        if(movies.isEmpty()){
            throw new MoviesNotFound("No movies found");
        }
        System.out.println("getAllMovies возвращает: " + movies.size() + " фильмов");
        return movies;
    }

    @Override
    public Movie getMovieById(long id) {
        String key = "movie:id:" + id;
        String cachedValue = jedis.get(key);

        if (cachedValue != null) {
            System.out.println("Found cached value: " + cachedValue);
            try {
                MovieDTO movieDTO = objectMapper.readValue(cachedValue, MovieDTO.class);
                Movie movie = new Movie();
                movie.setId(movieDTO.getId());
                movie.setTitle(movieDTO.getTitle());
                movie.setPosterPath(movieDTO.getPoster());
                return movie;
            } catch (JsonProcessingException e) {
                System.err.println("Ошибка десериализации кэша: " + e.getMessage());
                jedis.del(key);
            }
        }

        System.out.println("No cached value");
        try {
            Movie movie = movieRepository.findById(id).orElse(null);
            if (movie != null) {
                MovieDTO movieDTO = convertToDTO(movie);
                String saveValue = objectMapper.writeValueAsString(movieDTO);
                jedis.setex(key, 60, saveValue);
                System.out.println("Сохранено в кэш: " + saveValue);
            }
            return movie;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override
    public MovieDTO getMovieDtoById(long id) {
        String key = "movie:id:" + id;
        String cachedValue = jedis.get(key);

        if (cachedValue != null) {
            System.out.println("Found cached DTO in cache: " + cachedValue);
            try {
                return objectMapper.readValue(cachedValue, MovieDTO.class);
            } catch (Exception e) {
                System.err.println("Ошибка десериализации MovieDTO из кэша: " + e.getMessage());
                jedis.del(key);
            }
        }

        try {
            Movie movie = movieRepository.findById(id).orElse(null);
            if (movie != null) {
                MovieDTO movieDTO = convertToDTO(movie);
                String saveValue = objectMapper.writeValueAsString(movieDTO);
                jedis.setex(key, 60, saveValue);
                System.out.println("Сохранено DTO в кэш: " + saveValue);
                return movieDTO;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    @Override
    public List<Movie> getMovieByTitle(String title) {
        List<Movie> movies = movieRepository.findByTitle(title);
        if (movies == null || movies.isEmpty()) {
            throw new MoviesNotFound("No movies found");
        }
        System.out.println("getMovieByTitle для '" + title + "' возвращает: " + movies.size() + " фильмов");
        return movies;
    }

    @Override
    public List<Movie> getAllMovies(String sortBy, String sortDir) {
        Sort.Direction direction = Sort.Direction.fromOptionalString(sortDir).orElse(Sort.Direction.ASC);
        List<Movie> movies = movieRepository.findAll(Sort.by(direction, sortBy));
        System.out.println("getAllMovies с сортировкой sortBy=" + sortBy + ", sortDir=" + sortDir + " возвращает: " + movies.size() + " фильмов");
        return movies;
    }

    @Override
    public List<MovieDTO> getAllMoviesByTitle(String title) {
        List<MovieDTO> movies = movieRepository.findByTitleIgnoreCase(title).stream()
                .map(novie->convertToDTO(novie))
                .collect(Collectors.toList());
        System.out.println("getAllMoviesByTitle для '" + title + "' возвращает: " + movies.size() + " фильмов");
        return movies;
    }

    @Override
    public PagedResponseDTO<MovieDTO> getAllMoviesWithPageable(String sortBy, String sortDir, int page, int size, String genre) {
        List<String> validSortFields = Arrays.asList("title", "releaseYear", "rating", "duration");
        if (!validSortFields.contains(sortBy.toLowerCase())) {
            sortBy = "title";
        }
        if (!sortDir.equalsIgnoreCase("asc") && !sortDir.equalsIgnoreCase("desc")) {
            sortDir = "asc";
        }

        Sort.Direction direction = Sort.Direction.fromString(sortDir.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Specification<Movie> spec = byGenre(genre);

        Page<Movie> moviePage = movieRepository.findAll(spec, pageable);
        System.out.println("movieRepository.findAll возвращает: " + moviePage.getContent().size() + " фильмов на странице " + page);

        List<MovieDTO> movieDTOs = moviePage.getContent().stream()
                .map(novie->convertToDTO(novie))
                .collect(Collectors.toList());

        return new PagedResponseDTO<>(
                movieDTOs,
                moviePage.getNumber(),
                size,
                moviePage.getTotalElements(),
                moviePage.getTotalPages()
        );
    }

    @Override
    public List<MovieDTO> getAllMoviesForCarousel() {
        Specification<Movie> spec = byPopularity();
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "popularity"));
        return movieRepository.findAll(spec, pageable)
                .stream()
                .map(novie->convertToDTO(novie))
                .collect(Collectors.toList());
    }


    @Override
    public void saveMovie(Movie movie) {
        movieRepository.save(movie);
        addMovieToRedis(movie);
    }

    @Override
    public List<MovieDTO> searchMovies(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        saveSearchQuery(query);

        String sanitizedQuery = query.replaceAll("[\\[\\]\\(\\)\\{\\}\\^\"\\~\\*\\?\\:\\\\]", " ");
        String searchQuery = "@title:*" + sanitizedQuery + "*";

        try {
            Query redisQuery = new Query(searchQuery)
                    .returnFields("title")
                    .limit(0, 10);

            SearchResult result = jedis.ftSearch("moviesIdx", redisQuery);
            System.out.println("Redis search: " + searchQuery + ", found: " + result.getTotalResults());

            if (result.getTotalResults() == 0) {
                return Collections.emptyList();
            }

            return result.getDocuments().stream()
                    .map(doc -> {
                        String key = doc.getId();
                        String title = doc.getString("title");
                        long id = Long.parseLong(key.replace("movie:", ""));
                        MovieDTO dto = new MovieDTO();
                        dto.setId(id);
                        dto.setTitle(title);
                        return dto;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Redis search error: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public void saveSearchQuery(String query) {
        jedis.lpush("recent:searches", query);
        jedis.ltrim("recent:searches", 0, 4);
    }

    @Override
    public List<String> getRecentSearches() {
        return jedis.lrange("recent:searches", 0, 4);
    }

    @Override
    @Transactional
    public void initRedis() {
        try {
            jedis.ftInfo("moviesIdx");
            //System.out.println("Индекс уже существует.");
        } catch (Exception e) {
            //System.out.println("Индекс не существует, создаем.");
            Schema schema = new Schema()
                    .addTextField("title", 1.0)
                    .addTagField("genres");

            IndexDefinition indexDefinition = new IndexDefinition()
                    .setPrefixes("movie:");

            jedis.ftCreate("moviesIdx", IndexOptions.defaultOptions().setDefinition(indexDefinition), schema);
        }

        List<Movie> movies = movieRepository.findAll();
        //System.out.println("Загружено фильмов из базы данных: " + movies.size());
        for (Movie movie : movies) {
            addMovieToRedis(movie);
        }
    }

    private void addMovieToRedis(Movie movie) {
        Set<String> genreNames = movie.getGenres()
                .stream()
                .map(Genre::getName)
                .collect(Collectors.toSet());

        Map<String, String> redisDoc = new HashMap<>();
        redisDoc.put("title", movie.getTitle());
        redisDoc.put("genres", String.join(",", genreNames));
        redisDoc.put("poster", movie.getPosterPath() != null ? movie.getPosterPath() : "");

        jedis.hset("movie:" + movie.getId(), redisDoc);
        System.out.println("Фильм добавлен в Redis: " + movie.getTitle());
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
        List<GenreDTO> genreDTOs = movie.getGenres().stream()
                .map(genre -> new GenreDTO(genre.getId(), genre.getName()))
                .collect(Collectors.toList());
        dto.setGenres(genreDTOs);
        return dto;
    }

    private Specification<Movie> byPopularity() {
        return (root, query, cb) -> {
            query.orderBy(cb.desc(root.get("popularity")), cb.desc(root.get("rating")));
            return cb.conjunction();
        };
    }
}