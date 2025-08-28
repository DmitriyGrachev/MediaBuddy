package org.hrachov.com.filmproject.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FilmDTO {
    private Long id;
    private String title;
    private Integer releaseYear;
    private String description;
    private Double rating;
    private String poster;
    private String type; // "movie", "serial", или "film"

    // Дополнительные поля для Movie
    private Integer duration;
    private String director;
    private Double popularity;
    private List<GenreDTO> genres;

    // Дополнительные поля для Serial
    private Integer seasons;
    private Integer episodes;


    // Fields specific to Movie
    private String source; // New field for Movie source

    // Fields specific to Serial
    private Integer numberOfSeasons; // Changed from 'seasons' to match entity
    private Integer totalEpisodes; // Assuming this is calculated or set manually
    private List<SeasonDTO> seasonList; // New field for nested seasons

}