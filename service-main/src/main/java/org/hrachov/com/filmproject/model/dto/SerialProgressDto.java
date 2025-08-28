package org.hrachov.com.filmproject.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SerialProgressDto {
    private Double progress;
    private String episodeId;
    private String seasonId;
}
