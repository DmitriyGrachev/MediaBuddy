package org.hrachov.com.filmproject.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "serial")
@PrimaryKeyJoinColumn(name = "film_id")
public class Serial extends Film {

    @Column(name = "seasons")
    private Integer numberOfSeasons;

    @Column(name = "episodes")
    private Integer totalEpisodes;

    @Column(name = "poster")
    private String poster;

    @OneToMany(mappedBy = "serial", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonManagedReference // Serial "управляет" списком сезонов при сериализации
    private List<Season> seasonList = new ArrayList<>();

    // Helper methods (если есть)
    public void addSeason(Season season) {
        seasonList.add(season);
        season.setSerial(this);
    }

    public void removeSeason(Season season) {
        seasonList.remove(season);
        season.setSerial(null);
    }
}