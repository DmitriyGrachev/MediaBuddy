package org.hrachov.com.filmproject.elasticsearch;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilmSearchRepository extends ElasticsearchRepository<FilmDocument, Long> {

    // Spring Data сам поймет, как реализовать этот метод
    // Поиск по названию без учета регистра
    List<FilmDocument> findByTitleContainingIgnoreCase(String title);

    // Поиск фильмов, у которых есть хотя бы один жанр из переданного списка
    List<FilmDocument> findByGenresIn(List<String> genres);

    // Можно добавлять и более сложные кастомные запросы с помощью @Query
}
