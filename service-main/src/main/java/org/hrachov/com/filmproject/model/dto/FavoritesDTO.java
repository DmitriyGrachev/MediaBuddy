package org.hrachov.com.filmproject.model.dto;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoritesDTO {
    public String id;
    public long userId;
    public long filmId;
    public String type;
    public LocalDateTime date;
}
