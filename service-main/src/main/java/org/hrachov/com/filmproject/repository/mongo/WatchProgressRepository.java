package org.hrachov.com.filmproject.repository.mongo;

import org.hrachov.com.filmproject.model.WatchProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WatchProgressRepository extends MongoRepository<WatchProgress, String> {
    Optional<WatchProgress> findByUserIdAndFilmId(Long userId, Long filmId);
}
