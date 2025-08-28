package org.hrachov.com.filmproject.service.impl;

import lombok.RequiredArgsConstructor;
import org.hrachov.com.filmproject.model.Favorites;
import org.hrachov.com.filmproject.repository.mongo.FavoriteRepository;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.service.FavoritesService;
import org.hrachov.com.filmproject.utils.HistoryEventSender;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FavoritesServiceImpl implements FavoritesService {
    private final FavoriteRepository favoriteRepository;
    private final HistoryEventSender historyEventSender;

    public Favorites addFavorite(Favorites favorites) {
        //Отправка для рекоменжаций
        historyEventSender.sendWatchEvent(favorites.getUserId(), favorites.getFilmId(),"favorite",5.0);

        return favoriteRepository.save(favorites);
    }

    public Favorites checkFavorite(long filmId, long userId) {
        return favoriteRepository.findByUserIdAndFilmId(userId, filmId);
    }


    public void removeFavorite(long filmId, long userId) {
        Favorites favoriteToDelete = favoriteRepository.findByUserIdAndFilmId(userId, filmId);
        if (favoriteToDelete != null) {
            favoriteRepository.delete(favoriteToDelete);
        } else {
            System.out.println("Favorite not found for filmId: " + filmId + " userId: " + userId);
        }
    }
    public Page<Favorites> findFavoritesByUser(long userId, Pageable pageable) {
        return favoriteRepository.getFavoritesByUserId(userId, pageable);
    }
}
