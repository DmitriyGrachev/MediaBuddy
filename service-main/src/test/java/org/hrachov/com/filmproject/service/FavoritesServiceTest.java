package org.hrachov.com.filmproject.service;

import org.hrachov.com.filmproject.model.Favorites;
import org.hrachov.com.filmproject.repository.mongo.FavoriteRepository;
import org.hrachov.com.filmproject.service.impl.FavoritesServiceImpl;
import org.hrachov.com.filmproject.utils.HistoryEventSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class FavoritesServiceTest {
    @Mock
    private  FavoriteRepository favoriteRepository;

    @Mock
    private  HistoryEventSender historyEventSender;

    @InjectMocks
    private FavoritesServiceImpl favoritesService;

    @BeforeEach
    void setUp() {
        favoritesService = new FavoritesServiceImpl(favoriteRepository, historyEventSender);
    }

    @Test
    @DisplayName("Add to favorites should work And sends to microservice")
    void addToFavoritesShouldWork_AndSendToMicroservice() {
        Favorites favorites = new Favorites();
        favorites.setDate(LocalDateTime.now());
        favorites.setType("favorite");
        favorites.setFilmId(1);
        favorites.setUserId(1);

        // Stub the save method to return the input Favorites object
        when(favoriteRepository.save(favorites)).thenReturn(favorites);

        Favorites result = favoritesService.addFavorite(favorites);

        verify(favoriteRepository, times(1)).save(favorites);
        verify(historyEventSender, times(1))
                .sendWatchEvent(eq(favorites.getUserId()), eq(favorites.getFilmId()),
                        eq(favorites.getType()), anyDouble());

        assertNotNull(result);
        assertEquals(favorites.getDate(), result.getDate());
        assertEquals(favorites.getType(), result.getType());
        assertEquals(favorites.getFilmId(), result.getFilmId());
        assertEquals(favorites.getUserId(), result.getUserId());

    }
    @Test
    @DisplayName("Check if favorite")
    void checkIfFavorite() {
        Favorites  favorites = new Favorites();
        favorites.setDate(LocalDateTime.now());
        favorites.setType("favorite");
        favorites.setFilmId(1);
        favorites.setUserId(1);

        when(favoriteRepository.findByUserIdAndFilmId(
                favorites.getFilmId(),favorites.getUserId())).thenReturn(favorites);

        Favorites result = favoritesService.checkFavorite(1,1);

        verify(favoriteRepository, times(1)).findByUserIdAndFilmId(anyLong(),anyLong());
        assertNotNull(result);
        assertEquals(favorites.getDate(), result.getDate());
        assertEquals(favorites.getType(), result.getType());
        assertEquals(favorites.getFilmId(), result.getFilmId());
        assertEquals(favorites.getUserId(), result.getUserId());
    }
    @Test
    @DisplayName("Remove favorite should succeed")
    void removeFavorite() {
        Favorites favorites = new Favorites();
        favorites.setDate(LocalDateTime.now());
        favorites.setType("favorite");
        favorites.setFilmId(1);
        favorites.setUserId(1);

        when(favoriteRepository.findByUserIdAndFilmId(anyLong(),anyLong())).thenReturn(favorites);
        favoritesService.removeFavorite(1,1);

        verify(favoriteRepository, times(1)).findByUserIdAndFilmId(anyLong(),anyLong());
        verify(favoriteRepository, times(1)).delete(any(Favorites.class));
    }
    @Test
    @DisplayName("Remove favoretie should not succeed")
    void removeFavorite_shouldNotSucceed() {

        when(favoriteRepository.findByUserIdAndFilmId(anyLong(),anyLong())).thenReturn(null);

        favoritesService.removeFavorite(1,1);

        verify(favoriteRepository, times(1)).findByUserIdAndFilmId(anyLong(),anyLong());
        verify(favoriteRepository,never()).delete(any(Favorites.class));
    }
    @Test
    @DisplayName("Find favorites by User")
    void findFavoritesByUser() {

        Pageable pageable = PageRequest.of(0, 5,Sort.by(Sort.Direction.DESC,"date"));
        Page<Favorites> favoritesPage = new PageImpl<>(new ArrayList<>(),pageable,5);

        when(favoriteRepository.getFavoritesByUserId(anyLong(),any(Pageable.class))).thenReturn(favoritesPage);

        Page<Favorites> result = favoritesService.findFavoritesByUser(1L,pageable);

        verify(favoriteRepository, times(1)).getFavoritesByUserId(anyLong(),any(Pageable.class));

        assertNotNull(result);
        assertEquals(favoritesPage.getTotalElements(), result.getTotalElements());
        assertEquals(favoritesPage.getSize(), result.getSize());
        assertEquals(favoritesPage.getNumber(), result.getNumber());

    }
}

