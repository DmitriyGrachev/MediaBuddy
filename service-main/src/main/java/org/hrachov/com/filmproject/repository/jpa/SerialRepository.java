package org.hrachov.com.filmproject.repository.jpa;

import org.hrachov.com.filmproject.model.Serial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SerialRepository extends JpaRepository<Serial, Long> {
    // Новый метод: загружает Serial вместе с его сезонами (seasonList)
    // Коллекция episodeList у каждого сезона будет загружена позже (лениво)
    @Query("SELECT DISTINCT s FROM Serial s LEFT JOIN FETCH s.seasonList sl WHERE s.id = :id ORDER BY sl.seasonNumber ASC")
    Optional<Serial> findByIdWithSeasons(@Param("id") Long id);

    // Старый метод, вызывающий ошибку, следует удалить или закомментировать:
    // @Query("SELECT s FROM Serial s LEFT JOIN FETCH s.seasonList sl LEFT JOIN FETCH sl.episodeList el WHERE s.id = :id ORDER BY sl.seasonNumber ASC, el.episodeNumber ASC")
    // Optional<Serial> findByIdFetchingSeasonsAndEpisodes(@Param("id") Long id);
}
