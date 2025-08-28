package org.hrachov.com.filmproject.repository.jpa;

import org.hrachov.com.filmproject.model.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, Integer> {
    Optional<Episode> findById(Long episodeId);
}
