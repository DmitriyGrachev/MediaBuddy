package org.hrachov.com.filmproject.elasticsearch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hrachov.com.filmproject.model.Film;
import org.hrachov.com.filmproject.model.Genre;
import org.hrachov.com.filmproject.model.Movie;
import org.hrachov.com.filmproject.model.Serial;
import org.hrachov.com.filmproject.model.dto.FilmDTO;
import org.hrachov.com.filmproject.model.dto.GenreDTO;
import org.hrachov.com.filmproject.repository.jpa.FilmRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.ScriptType;
import org.springframework.data.elasticsearch.core.script.Script;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilmSearchService {

    private final FilmRepository filmRepository; // JPA репозиторий
    private final FilmSearchRepository filmSearchRepository; // Elasticsearch репозиторий
    private final ElasticsearchOperations elasticsearchOperations;


    @Transactional(readOnly = true)
    public void reindexAll() {
        log.info("Starting reindexing all films to Elasticsearch...");
        List<Film> filmsFromDb = filmRepository.findAll();

        List<FilmDocument> filmDocuments = filmsFromDb.stream()
                .map(this::mapToDocument)
                .collect(Collectors.toList());

        filmSearchRepository.saveAll(filmDocuments);
        log.info("Reindexing completed. Indexed {} documents.", filmDocuments.size());
    }
    public List<FilmDTO> searchFilms(FilmSearchCriteria searchCriteria) {
        Criteria criteriaQuery = new Criteria();

        if(searchCriteria.getTitle() != null && !searchCriteria.getTitle().isEmpty()) {
            //fuzzy || wildcard (contains)
            Criteria fuzzyCriteria = Criteria.where("title").fuzzy(searchCriteria.getTitle());
            Criteria wildcardCriteria = Criteria.where("title.ngram").contains("*" + searchCriteria.getTitle() + "*");
            criteriaQuery = fuzzyCriteria.or(wildcardCriteria);
            //criteriaQuery = criteriaQuery.and("title").matches(searchCriteria.getTitle());
        }

        if(searchCriteria.getFilmType() != null && !searchCriteria.getFilmType().isEmpty()) {
            criteriaQuery = criteriaQuery.and("filmType").is(searchCriteria.getFilmType());
        }

        if (searchCriteria.getDirector() != null && !searchCriteria.getDirector().isEmpty()) {
            criteriaQuery = criteriaQuery.and("director").is(searchCriteria.getDirector());
        }

        if (searchCriteria.getDateFrom() != null || searchCriteria.getDateTo() != null) {
            Criteria yearCrit = new Criteria("releaseYear");
            if (searchCriteria.getDateFrom() != null) {
                yearCrit = yearCrit.greaterThanEqual(searchCriteria.getDateFrom());
            }
            if (searchCriteria.getDateTo() != null) {
                yearCrit = yearCrit.lessThanEqual(searchCriteria.getDateTo());
            }
            criteriaQuery = criteriaQuery.and(yearCrit);
        }

        if(searchCriteria.getRatingFrom() != null || searchCriteria.getRatingTo() != null) {
            Criteria ratingCrit = new Criteria("rating");
            if(searchCriteria.getRatingFrom() != null) ratingCrit = ratingCrit.greaterThanEqual(searchCriteria.getRatingFrom());
            if(searchCriteria.getRatingTo() != null) ratingCrit = ratingCrit.lessThanEqual(searchCriteria.getRatingTo());
            criteriaQuery = criteriaQuery.and("rating").greaterThan(ratingCrit);
        }
        Sort sort = Sort.unsorted();
        if(searchCriteria.getSort() != null && !searchCriteria.getSort().isEmpty()) {
            sort = Sort.by(Sort.Direction.DESC, searchCriteria.getSort());
        }
        if(searchCriteria.getGenres() != null && !searchCriteria.getGenres().isEmpty()) {
            //TODO IN - Ищет все жанры а не выборку по жанрам
            criteriaQuery.and("genres").in(searchCriteria.getGenres());
        }

        if(searchCriteria.getDurationFrom()!=null || searchCriteria.getDurationTo()!=null) {
            Criteria durationCrit = new Criteria("duration");
            if(searchCriteria.getDurationFrom() != null) durationCrit = durationCrit.greaterThanEqual(searchCriteria.getDurationFrom());
            if(searchCriteria.getDurationTo() != null) durationCrit = durationCrit.greaterThanEqual(searchCriteria.getDurationTo());
            criteriaQuery = criteriaQuery.and("duration").greaterThan(durationCrit);
        }
        if(searchCriteria.getNumberOfSeasons() != null){
            criteriaQuery = criteriaQuery.and("seasons").greaterThan(searchCriteria.getNumberOfSeasons());
        }
        if(searchCriteria.getTotalEpisodes() != null){
            criteriaQuery = criteriaQuery.and("episodes").greaterThan(searchCriteria.getTotalEpisodes());
        }

        CriteriaQuery query = new CriteriaQuery(criteriaQuery)
                .setPageable(PageRequest.of(0, 50, sort));

        // 4) Выполняем поиск
        return elasticsearchOperations
                .search(query, FilmDocument.class)
                .map(SearchHit::getContent)
                .map(this::mapToDTO)
                .toList();
    }

    public List<FilmDTO> autocompleteTitles(String query) {
        query = query.trim();
        Criteria criteria = new Criteria("title").matches(query);
        CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);
        criteriaQuery.setPageable(PageRequest.of(0, 10)); // Ограничь количество

        return elasticsearchOperations
                .search(criteriaQuery, FilmDocument.class)
                .stream()
                .map(hit -> mapToDTO(hit.getContent())).limit(5)
                .toList();
    }

    private FilmDTO mapToDTO(FilmDocument doc) {
        return FilmDTO.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .releaseYear(doc.getReleaseYear())
                .description(doc.getDescription())
                .rating(doc.getRating())
                .poster(doc.getPoster())
                .type(doc.getFilmType())
                .duration(doc.getDuration())
                .director(doc.getDirector())
                .popularity(doc.getPopularity())
                .genres(doc.getGenres().stream()
                        .map(name -> new GenreDTO(name))  // если GenreDTO просто обёртка над именем
                        .collect(Collectors.toList()))
                .seasons(doc.getNumberOfSeasons())
                .episodes(doc.getTotalEpisodes())
                .build();
    }
    private FilmDocument mapToDocument(Film film) {
        FilmDocument.FilmDocumentBuilder builder = FilmDocument.builder()
                .id(film.getId())
                .title(film.getTitle())
                .releaseYear(film.getReleaseYear())
                .description(film.getDescription())
                .director(film.getDirector())
                .rating(film.getRating())
                .popularity(film.getPopularity())
                .genres(film.getGenres().stream()
                        .map(Genre::getName)
                        .collect(Collectors.toList()));

        if (film instanceof Movie) {
            Movie movie = (Movie) film;
            builder.filmType("movie")
                    .poster(movie.getPosterPath())
                    .duration(movie.getDuration());
        } else if (film instanceof Serial) {
            Serial serial = (Serial) film;
            builder.filmType("serial")
                    .poster(serial.getPoster())
                    .numberOfSeasons(serial.getNumberOfSeasons())
                    .totalEpisodes(serial.getTotalEpisodes());
        }
        return builder.build();
    }
}
