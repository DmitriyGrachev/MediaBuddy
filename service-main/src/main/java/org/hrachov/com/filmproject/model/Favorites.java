package org.hrachov.com.filmproject.model;

import com.google.errorprone.annotations.NoAllocation;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SecondaryRow;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@Document("Favorites")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorites {
    @Id
    private String id;

    private long userId;
    private long filmId;
    private String type;
    private LocalDateTime date;
}
