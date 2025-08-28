package org.hrachov.com.filmproject.model;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Document("UserVideos")
public class UsersVideos {
    @Id
    private String id;

    private String title;
    private String description;
    private Long userId;
    private LocalDateTime creationDate;
    private String url;
}
