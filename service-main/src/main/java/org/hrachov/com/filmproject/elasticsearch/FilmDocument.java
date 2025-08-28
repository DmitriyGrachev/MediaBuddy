package org.hrachov.com.filmproject.elasticsearch;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "mediabuddy") // Название индекса в Elasticsearch
public class FilmDocument {

    private Long id;
    private String filmType;
    private String title;
    private Integer releaseYear;
    private String description;
    private String director;
    private Double rating;
    private Double popularity;
    private List<String> genres;
    private Integer duration;
    private Integer numberOfSeasons;
    private Integer totalEpisodes;
    private String poster;
}

