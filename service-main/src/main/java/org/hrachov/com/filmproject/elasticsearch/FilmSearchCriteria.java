package org.hrachov.com.filmproject.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FilmSearchCriteria {
    private Long id;
    private String filmType;
    private String title;

    private Date dateFrom;
    private Date dateTo;

    private Double ratingFrom;
    private Double ratingTo;

    private String description;
    private String director;
    private String sort; //Rating,Popularity
    private List<String> genres;

    private Integer durationFrom;
    private Integer durationTo;

    private Integer numberOfSeasons;
    private Integer totalEpisodes;
}
