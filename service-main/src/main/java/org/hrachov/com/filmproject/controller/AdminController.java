package org.hrachov.com.filmproject.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.model.*;
import org.hrachov.com.filmproject.model.dto.*;
import org.hrachov.com.filmproject.service.impl.admin.EpisodeManagementService;
import org.hrachov.com.filmproject.service.impl.admin.FilmManagementService;
import org.hrachov.com.filmproject.service.impl.admin.SeasonManagementService;
import org.hrachov.com.filmproject.service.impl.admin.UserManagementService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/")
@Tag(name = "Админ контроллер", description = "Операции с фильмами,пользователями,OMDB api")
@AllArgsConstructor
public class AdminController {

    private final UserManagementService userManagementService;
    private final FilmManagementService filmManagementService;
    private final SeasonManagementService seasonManagementService;
    private final EpisodeManagementService episodeManagementService;
    @Operation(summary = "Получить всех пользователей",description = "Возвращает ВСЕХ пользователей с БД")
    @GetMapping("/all-users")
    public ResponseEntity<List<UserResponseDTO>> getUsers() {
        List<UserResponseDTO> users = userManagementService.getAllUsersWithBlockInfo();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<User> deleteUser(@PathVariable Long id) {
        User deletedUser = userManagementService.deleteUserById(id);
        return ResponseEntity.ok(deletedUser);
    }

    @PutMapping("/update")
    public ResponseEntity<User> updateUser(@RequestBody UserDTO userDTO) {
        User updatedUser = userManagementService.updateUser(userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/find")
    public ResponseEntity<User> findByUsernameOrEmail(@RequestParam String q) {
        User user = userManagementService.findUserByUsernameOrEmail(q);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/users/{userId}/roles")
    public ResponseEntity<Map<String, String>> updateUserRoles(
            @PathVariable Long userId,
            @Valid @RequestBody Set<String> newRoleStrings) {
        userManagementService.updateUserRoles(userId, newRoleStrings);
        return ResponseEntity.ok(Map.of(
                "message", "Роли пользователя с ID " + userId + " успешно обновлены."
        ));
    }

    @PutMapping("/block")
    public ResponseEntity<Map<String, String>> blockUser(@RequestBody BlockUserRequest blockUserRequest) {
        try {
            userManagementService.blockUser(blockUserRequest);
            return ResponseEntity.ok(Map.of("message", "Пользователь успешно заблокирован."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/unblock")
    public ResponseEntity<Map<String, String>> unblockUser(@RequestParam Long id) {
        try {
            userManagementService.unblockUser(id);
            return ResponseEntity.ok(Map.of("message", "Пользователь успешно разблокирован."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/blockInfo")
    public ResponseEntity<Map<String, String>> checkBlockInfo(@RequestParam(required = true) Long id) {
        try {
            Map<String, String> blockInfo = userManagementService.getBlockInfo(id);
            return ResponseEntity.ok(blockInfo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/getFilmData")
    public ResponseEntity<FilmDTO> getFilmDataFromOmdb(@RequestParam String title) {
        try {
            FilmDTO filmDTO = filmManagementService.getFilmDataFromExternalApi(title);
            return ResponseEntity.ok(filmDTO);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("Фильм не найден")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    @PostMapping("/films")
    public ResponseEntity<FilmDTO> createFilm(@RequestBody FilmDTO filmDTO) {
        FilmDTO createdFilm = filmManagementService.createFilm(filmDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdFilm);
    }

    @GetMapping("/films")
    public ResponseEntity<List<FilmDTO>> getAllFilms() {
        List<FilmDTO> films = filmManagementService.getAllFilms();
        return ResponseEntity.ok(films);
    }

    @GetMapping("/films/{id}")
    public ResponseEntity<FilmDTO> getFilmById(@PathVariable Long id) {
        FilmDTO film = filmManagementService.getFilmById(id);
        return ResponseEntity.ok(film);
    }

    @PutMapping("/films/{id}")
    public ResponseEntity<FilmDTO> updateFilm(@PathVariable Long id, @RequestBody FilmDTO filmDTO) {
        FilmDTO updatedFilm = filmManagementService.updateFilm(id, filmDTO);
        return ResponseEntity.ok(updatedFilm);
    }

    @DeleteMapping("/films/{id}")
    public ResponseEntity<Void> deleteFilm(@PathVariable Long id) {
        filmManagementService.deleteFilm(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/films/{serialId}/seasons")
    public ResponseEntity<SeasonDTO> createSeason(
            @PathVariable Long serialId,
            @RequestBody SeasonDTO seasonDTO) {
        SeasonDTO createdSeason = seasonManagementService.createSeason(serialId, seasonDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdSeason);
    }

    @GetMapping("/seasons/{id}")
    public ResponseEntity<SeasonDTO> getSeasonById(@PathVariable Long id) {
        SeasonDTO season = seasonManagementService.getSeasonById(id);
        return ResponseEntity.ok(season);
    }

    @PutMapping("/seasons/{id}")
    public ResponseEntity<SeasonDTO> updateSeason(@PathVariable Long id, @RequestBody SeasonDTO seasonDTO) {
        SeasonDTO updatedSeason = seasonManagementService.updateSeason(id, seasonDTO);
        return ResponseEntity.ok(updatedSeason);
    }

    @DeleteMapping("/seasons/{id}")
    public ResponseEntity<Void> deleteSeason(@PathVariable Long id) {
        seasonManagementService.deleteSeason(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/seasons/{seasonId}/episodes")
    public ResponseEntity<EpisodeDTO> createEpisode(
            @PathVariable Long seasonId,
            @RequestBody EpisodeDTO episodeDTO) {
        EpisodeDTO createdEpisode = episodeManagementService.createEpisode(seasonId, episodeDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEpisode);
    }

    @GetMapping("/episodes/{id}")
    public ResponseEntity<EpisodeDTO> getEpisodeById(@PathVariable Long id) {
        EpisodeDTO episode = episodeManagementService.getEpisodeById(id);
        return ResponseEntity.ok(episode);
    }

    @PutMapping("/episodes/{id}")
    public ResponseEntity<EpisodeDTO> updateEpisode(@PathVariable Long id, @RequestBody EpisodeDTO episodeDTO) {
        EpisodeDTO updatedEpisode = episodeManagementService.updateEpisode(id, episodeDTO);
        return ResponseEntity.ok(updatedEpisode);
    }

    @DeleteMapping("/episodes/{id}")
    public ResponseEntity<Void> deleteEpisode(@PathVariable Long id) {
        episodeManagementService.deleteEpisode(id);
        return ResponseEntity.noContent().build();
    }
}