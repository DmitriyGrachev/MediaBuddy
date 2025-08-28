package org.hrachov.com.filmproject.model.dto;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRatingDto {
    private Integer userRating;
    private Double filmRating;
    private Long votes;
}
