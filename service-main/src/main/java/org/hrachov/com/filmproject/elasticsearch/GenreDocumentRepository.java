package org.hrachov.com.filmproject.elasticsearch;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GenreDocumentRepository extends ElasticsearchRepository<GenreDocument, String> {
    //Ищем по префиксу интерсно попробывать WildcardQueryBuilder
    @Query("{ \"wildcard\": { \"name\": { \"value\": \"?0*\" } } }")
    List<GenreDocument> findByName(String name);

}
