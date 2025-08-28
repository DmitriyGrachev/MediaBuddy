package org.hrachov.com.filmproject.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OmdbDTO {
    @JsonProperty("Title")
    private String title;

    @JsonProperty("Year")
    private String releaseYear;

    @JsonProperty("Rated")
    private String rating;//convert to double during mapping to miviedto

    @JsonProperty("Runtime")
    private String duration;

    @JsonProperty("Genre")
    private String genres;

    @JsonProperty("Director")
    private String director;

    @JsonProperty("Plot")
    private String description;

    @JsonProperty("Poster")
    private String poster;
    /*
    "Ratings": [
    {
      "Source": "Internet Movie Database",
      "Value": "7.4/10"
    },
    {
      "Source": "Rotten Tomatoes",
      "Value": "94%"
    },
    {
      "Source": "Metacritic",
      "Value": "73/100"
    }
  ],
  "Metascore": "73",
  "imdbRating": "7.4",
  "imdbVotes": "548,584",
  "imdbID": "tt1229238",
     */
    @JsonProperty("imdbRating")
    private String imdbRating;

    @JsonProperty("Type") // "movie", "series"
    private String type;

    @JsonProperty("totalSeasons")
    private String totalSeasons;

    @JsonProperty("Response") // "True" or "False"
    private String response;
}
//TODO REMAKE FOR FILMS WITH TYPE MOVIE/SERIAL