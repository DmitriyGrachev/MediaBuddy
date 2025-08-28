package org.hrachov.com.filmproject.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hrachov.com.filmproject.model.Comment;
import org.hrachov.com.filmproject.model.Genre;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Data
@NoArgsConstructor
public class MovieDTO {
    private Long id;
    private String title;
    private Integer releaseYear;
    private String description;
    private String director;
    private Double rating;
    private Integer duration;
    private Double popularity;
    private String poster;
    private String trailerUrl;
    private List<CommentDTO> comments;
    private String source;
    private List<GenreDTO> genres; // Changed from Set<Genre>
}