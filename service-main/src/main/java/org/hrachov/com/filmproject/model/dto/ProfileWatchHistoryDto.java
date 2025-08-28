package org.hrachov.com.filmproject.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileWatchHistoryDto {
    private String filmId;
    private String title;
    private String type;
    private LocalDateTime date;
    private String posterUrl;
}
