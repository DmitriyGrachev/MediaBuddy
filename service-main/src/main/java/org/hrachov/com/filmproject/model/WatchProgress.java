package org.hrachov.com.filmproject.model;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document("Watch progress")
public class WatchProgress {
    @Id
    private String id;

    private Long userId;
    private Long filmId;
    private Double currentTime;

    private LocalDateTime updateAt;
    //NEW TODO
    private String type;//movie / episode
    private String episodeId;
    private String seasonId;
}
