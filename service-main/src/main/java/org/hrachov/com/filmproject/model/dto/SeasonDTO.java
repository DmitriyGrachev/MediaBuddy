package org.hrachov.com.filmproject.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// DTO для Season
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeasonDTO {
    private Long id;
    private Integer seasonNumber;
    private String title;
    private Long serialFilmId; // ID родительского сериала
    private List<EpisodeDTO> episodeList; // Для вложенной структуры
}
