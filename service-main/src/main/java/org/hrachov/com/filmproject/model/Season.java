package org.hrachov.com.filmproject.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "seasons")
public class Season {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serial_film_id", nullable = false)
    @JsonBackReference("serial-season") // Уникальное имя для обратной ссылки, если есть несколько
    private Serial serial; // Эта ссылка не будет сериализоваться, чтобы избежать цикла

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("episodeNumber ASC")
    @JsonManagedReference("season-episode") // Season "управляет" списком эпизодов
    private List<Episode> episodeList = new ArrayList<>();

    // Helper methods (если есть)
    public void addEpisode(Episode episode) {
        episodeList.add(episode);
        episode.setSeason(this);
    }

    public void removeEpisode(Episode episode) {
        episodeList.remove(episode);
        episode.setSeason(null);
    }
}