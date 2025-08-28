package org.hrachov.com.filmproject.repository.mongo;

import org.hrachov.com.filmproject.model.Favorites;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface FavoriteRepository extends MongoRepository<Favorites,String> {
    Favorites findByUserIdAndFilmId(long userId, long filmId);

    @Transactional
    void deleteByUserIdAndFilmId(long userId, long filmId);

    Page<Favorites> getFavoritesByUserId(long userId, Pageable pageable);
}
