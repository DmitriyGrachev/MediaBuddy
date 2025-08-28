package org.hrachov.com.filmproject.controller;

import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.exception.FavoriteNotFoundException;
import org.hrachov.com.filmproject.model.Favorites;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.service.UserService;
import org.hrachov.com.filmproject.service.FavoritesService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@AllArgsConstructor
public class FavoritesController {

    private final FavoritesService favoritesService;
    private final CurrentUserService currentUserService;
    private final UserService userService;


    @PostMapping("/add")
    public ResponseEntity<Map<String,String>> addToFavorites(@RequestParam long filmId,@RequestParam String type) {
        try {
            Map<String,String> map = new HashMap<>();
            Favorites favorite = new Favorites();
            User user = userService.findByUsername(currentUserService.getCurrentUser().getUsername());

            favorite.setFilmId(filmId);
            favorite.setUserId(user.getId());
            favorite.setType(type);
            favorite.setDate(LocalDateTime.now());
            favoritesService.addFavorite(favorite);

            map.put("status", "success");

        return ResponseEntity.ok().body(map);

        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @GetMapping("/check")
    public ResponseEntity<Map<String,String>> checkFavorites(@RequestParam long filmId) {
        Map<String, String> map = new HashMap<>();
        User user = userService.findByUsername(currentUserService.getCurrentUser().getUsername());
        Long userId = user.getId();
        Favorites favorite = favoritesService.checkFavorite(filmId, userId);
        System.out.println(favorite);
        map.put("status", "success");
        map.put("isFavorite", favorite != null ? "true" : "false");

        return ResponseEntity.ok(map);
    }
    @DeleteMapping("/remove")
    public ResponseEntity<Map<String, String>> removeFavorites(@RequestParam long filmId) {
        Map<String, String> map = new HashMap<>();
            User user = userService.findByUsername(currentUserService.getCurrentUser().getUsername());

            if(favoritesService.checkFavorite(filmId,user.getId()) == null){
                throw new FavoriteNotFoundException(filmId);
            }

            favoritesService.removeFavorite(filmId, user.getId());
            map.put("status", "success");
            map.put("message", "Favorite removed successfully.");
            return ResponseEntity.ok(map);
    }
}
