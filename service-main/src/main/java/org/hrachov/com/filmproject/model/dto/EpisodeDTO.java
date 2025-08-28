package org.hrachov.com.filmproject.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO для Episode
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EpisodeDTO {
    private Long id;
    private Integer episodeNumber;
    private String title;
    private String source;
    private Integer duration;
    private Long seasonId; // ID родительского сезона
}