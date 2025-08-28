package org.hrachov.com.filmproject.repository.jpa;

import org.hrachov.com.filmproject.model.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeasonRepository extends JpaRepository<Season, Integer> {
    Optional<Season> findById(Long seasonId);
}
