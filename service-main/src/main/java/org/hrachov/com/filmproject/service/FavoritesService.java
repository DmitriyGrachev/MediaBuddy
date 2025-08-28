package org.hrachov.com.filmproject.service;

import org.hrachov.com.filmproject.model.Favorites;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface FavoritesService {
     Favorites addFavorite(Favorites favorites);
     Favorites checkFavorite(long filmId, long userId);
     void removeFavorite(long filmId, long userId);
     Page<Favorites> findFavoritesByUser(long userId, Pageable pageable);

}
