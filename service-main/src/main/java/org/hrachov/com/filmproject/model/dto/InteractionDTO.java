package org.hrachov.com.filmproject.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class InteractionDTO {
    private String userId;
    private String filmId;
    private String interactionType;
    private Double value;
    private LocalDateTime timestamp;
}
