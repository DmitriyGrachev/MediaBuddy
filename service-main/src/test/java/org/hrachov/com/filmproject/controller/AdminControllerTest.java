package org.hrachov.com.filmproject.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.hrachov.com.filmproject.exception.UserNotFoundException;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.dto.*;
import org.hrachov.com.filmproject.security.JwtUtils;
import org.hrachov.com.filmproject.service.impl.admin.EpisodeManagementService;
import org.hrachov.com.filmproject.service.impl.admin.FilmManagementService;
import org.hrachov.com.filmproject.service.impl.admin.SeasonManagementService;
import org.hrachov.com.filmproject.service.impl.admin.UserManagementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserManagementService userManagementService;

    @MockBean
    private FilmManagementService filmManagementService;

    @MockBean
    private SeasonManagementService seasonManagementService;

    @MockBean
    private EpisodeManagementService episodeManagementService;

    @Test
    @DisplayName("Test get all users with block info")
    public void getAllUsersWithBlockInfo() throws Exception {
        UserResponseDTO responseDTO = new UserResponseDTO();
        responseDTO.setUsername("admin");
        responseDTO.setBlockReason("block");
        responseDTO.setBlocked(true);

        List<UserResponseDTO> responseDTOList = List.of(responseDTO);

        when(userManagementService.getAllUsersWithBlockInfo()).thenReturn(responseDTOList);

        mockMvc.perform(get("/api/admin/all-users"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].blockReason").value("block"))
                .andExpect(jsonPath("$[0].blocked").value(true));
    }

    @Test
    @DisplayName("Delete User should return deleted user")
    public void deleteUser() throws Exception {
        User user = new User();
        user.setUsername("admin");
        user.setPassword("admin");
        user.setId(1L);

        when(userManagementService.deleteUserById(1L)).thenReturn(user);

        mockMvc.perform(delete("/api/admin/delete/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Delete user should throw UserNotFoundException")
    public void deleteUserNotFound() throws Exception {
        when(userManagementService.deleteUserById(1L)).thenThrow(new UserNotFoundException(1L));

        mockMvc.perform(delete("/api/admin/delete/{id}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("User with id 1 not found"));
    }

    @Test
    @DisplayName("Should return updated user")
    public void updateUser() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("UPDATED USER");
        user.setEmail("user@gmail.com");

        UserDTO userDTO = new UserDTO();
        userDTO.setId(1L);
        userDTO.setUsername("UPDATED USER");
        userDTO.setEmail("user@gmail.com");

        when(userManagementService.updateUser(any(UserDTO.class))).thenReturn(user);

        mockMvc.perform(put("/api/admin/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("UPDATED USER"))
                .andExpect(jsonPath("$.email").value("user@gmail.com"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Find user by username or email should return user")
    public void findByUsernameOrEmail() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setEmail("admin@example.com");

        when(userManagementService.findUserByUsernameOrEmail("admin")).thenReturn(user);

        mockMvc.perform(get("/api/admin/find").param("q", "admin"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Update user roles should succeed")
    public void updateUserRoles() throws Exception {
        Long userId = 1L;
        Set<String> newRoleStrings = Set.of("ROLE_ADMIN", "ROLE_REGULAR");

        doNothing().when(userManagementService).updateUserRoles(eq(userId), any(Set.class));

        mockMvc.perform(put("/api/admin/users/{userId}/roles", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRoleStrings)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Роли пользователя с ID 1 успешно обновлены."));
    }

    @Test
    @DisplayName("Block user should succeed")
    public void blockUser() throws Exception {
        BlockUserRequest blockUserRequest = new BlockUserRequest();
        blockUserRequest.setUserId(1L);
        blockUserRequest.setReason("Test block");
        blockUserRequest.setBlockedUntil(LocalDateTime.now().plusDays(1));

        doNothing().when(userManagementService).blockUser(any(BlockUserRequest.class));

        mockMvc.perform(put("/api/admin/block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockUserRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Пользователь успешно заблокирован."));
    }

    @Test
    @DisplayName("Block user should return internal server error on exception")
    public void blockUser_shouldReturnInternalServerError() throws Exception {
        BlockUserRequest blockUserRequest = new BlockUserRequest();
        blockUserRequest.setUserId(1L);
        blockUserRequest.setReason("Test block");

        doThrow(new RuntimeException("Block error")).when(userManagementService).blockUser(any(BlockUserRequest.class));

        mockMvc.perform(put("/api/admin/block")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blockUserRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Block error"));
    }

    @Test
    @DisplayName("Unblock user should succeed")
    public void unblockUser() throws Exception {
        Long userId = 1L;

        doNothing().when(userManagementService).unblockUser(userId);

        mockMvc.perform(put("/api/admin/unblock").param("id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Пользователь успешно разблокирован."));
    }

    @Test
    @DisplayName("Unblock user should return internal server error on exception")
    public void unblockUser_shouldReturnInternalServerError() throws Exception {
        Long userId = 1L;

        doThrow(new RuntimeException("Unblock error")).when(userManagementService).unblockUser(userId);

        mockMvc.perform(put("/api/admin/unblock").param("id", userId.toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unblock error"));
    }

    @Test
    @DisplayName("Check block info should return block info")
    public void checkBlockInfo() throws Exception {
        Long userId = 1L;
        Map<String, String> blockInfo = Map.of("isBlocked", "true", "info", "Blocked for spam");

        when(userManagementService.getBlockInfo(userId)).thenReturn(blockInfo);

        mockMvc.perform(get("/api/admin/blockInfo").param("id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.isBlocked").value("true"))
                .andExpect(jsonPath("$.info").value("Blocked for spam"));
    }

    @Test
    @DisplayName("Check block info should return internal server error on exception")
    public void checkBlockInfo_shouldReturnInternalServerError() throws Exception {
        Long userId = 1L;

        when(userManagementService.getBlockInfo(userId)).thenThrow(new RuntimeException("Block info error"));

        mockMvc.perform(get("/api/admin/blockInfo").param("id", userId.toString()))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Get film data from OMDB should return FilmDTO")
    public void getFilmDataFromOmdb() throws Exception {
        String title = "Inception";
        FilmDTO filmDTO = new FilmDTO();
        filmDTO.setTitle("Inception");
        filmDTO.setType("movie");

        when(filmManagementService.getFilmDataFromExternalApi(title)).thenReturn(filmDTO);

        mockMvc.perform(get("/api/admin/getFilmData").param("title", title))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Inception"))
                .andExpect(jsonPath("$.type").value("movie"));
    }

    @Test
    @DisplayName("Get film data from OMDB should return not found when film not found")
    public void getFilmDataFromOmdb_shouldReturnNotFound() throws Exception {
        String title = "NonExistentMovie";

        when(filmManagementService.getFilmDataFromExternalApi(title))
                .thenThrow(new RuntimeException("Фильм не найден"));

        mockMvc.perform(get("/api/admin/getFilmData").param("title", title))
                .andExpect(status().isNotFound());
    }


    @Test
    @DisplayName("Create film should return created FilmDTO")
    public void createFilm() throws Exception {
        FilmDTO filmDTO = new FilmDTO();
        filmDTO.setTitle("Inception");
        filmDTO.setType("movie");

        when(filmManagementService.createFilm(any(FilmDTO.class))).thenReturn(filmDTO);

        mockMvc.perform(post("/api/admin/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filmDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Inception"))
                .andExpect(jsonPath("$.type").value("movie"));
    }

    @Test
    @DisplayName("Get all films should return list of FilmDTO")
    public void getAllFilms() throws Exception {
        FilmDTO filmDTO = new FilmDTO();
        filmDTO.setTitle("Inception");
        filmDTO.setType("movie");
        List<FilmDTO> films = List.of(filmDTO);

        when(filmManagementService.getAllFilms()).thenReturn(films);

        mockMvc.perform(get("/api/admin/films"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].title").value("Inception"))
                .andExpect(jsonPath("$[0].type").value("movie"));
    }

    @Test
    @DisplayName("Get film by ID should return FilmDTO")
    public void getFilmById() throws Exception {
        Long filmId = 1L;
        FilmDTO filmDTO = new FilmDTO();
        filmDTO.setId(filmId);
        filmDTO.setTitle("Inception");

        when(filmManagementService.getFilmById(filmId)).thenReturn(filmDTO);

        mockMvc.perform(get("/api/admin/films/{id}", filmId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(filmId))
                .andExpect(jsonPath("$.title").value("Inception"));
    }

    @Test
    @DisplayName("Update film should return updated FilmDTO")
    public void updateFilm() throws Exception {
        Long filmId = 1L;
        FilmDTO filmDTO = new FilmDTO();
        filmDTO.setId(filmId);
        filmDTO.setTitle("Updated Inception");

        when(filmManagementService.updateFilm(eq(1L), any(FilmDTO.class))).thenReturn(filmDTO);

        mockMvc.perform(put("/api/admin/films/{id}", filmId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filmDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(filmId))
                .andExpect(jsonPath("$.title").value("Updated Inception"));
    }

    @Test
    @DisplayName("Delete film should return no content")
    public void deleteFilm() throws Exception {
        Long filmId = 1L;

        doNothing().when(filmManagementService).deleteFilm(filmId);

        mockMvc.perform(delete("/api/admin/films/{id}", filmId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Create season should return created SeasonDTO")
    public void createSeason() throws Exception {
        Long serialId = 1L;
        SeasonDTO seasonDTO = new SeasonDTO();
        seasonDTO.setId(1L);
        seasonDTO.setSeasonNumber(1);

        when(seasonManagementService.createSeason(eq(serialId), any(SeasonDTO.class))).thenReturn(seasonDTO);

        mockMvc.perform(post("/api/admin/films/{serialId}/seasons", serialId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(seasonDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.seasonNumber").value(1));
    }

    @Test
    @DisplayName("Get season by ID should return SeasonDTO")
    public void getSeasonById() throws Exception {
        Long seasonId = 1L;
        SeasonDTO seasonDTO = new SeasonDTO();
        seasonDTO.setId(seasonId);
        seasonDTO.setSeasonNumber(1);

        when(seasonManagementService.getSeasonById(seasonId)).thenReturn(seasonDTO);

        mockMvc.perform(get("/api/admin/seasons/{id}", seasonId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(seasonId))
                .andExpect(jsonPath("$.seasonNumber").value(1));
    }

    @Test
    @DisplayName("Update season should return updated SeasonDTO")
    public void updateSeason() throws Exception {
        Long seasonId = 1L;
        SeasonDTO seasonDTO = new SeasonDTO();
        seasonDTO.setId(seasonId);
        seasonDTO.setSeasonNumber(2);

        when(seasonManagementService.updateSeason(eq(seasonId), any(SeasonDTO.class))).thenReturn(seasonDTO);

        mockMvc.perform(put("/api/admin/seasons/{id}", seasonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(seasonDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(seasonId))
                .andExpect(jsonPath("$.seasonNumber").value(2));
    }

    @Test
    @DisplayName("Delete season should return no content")
    public void deleteSeason() throws Exception {
        Long seasonId = 1L;

        doNothing().when(seasonManagementService).deleteSeason(seasonId);

        mockMvc.perform(delete("/api/admin/seasons/{id}", seasonId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Create episode should return created EpisodeDTO")
    public void createEpisode() throws Exception {
        Long seasonId = 1L;
        EpisodeDTO episodeDTO = new EpisodeDTO();
        episodeDTO.setId(1L);
        episodeDTO.setEpisodeNumber(1);

        when(episodeManagementService.createEpisode(eq(seasonId), any(EpisodeDTO.class))).thenReturn(episodeDTO);

        mockMvc.perform(post("/api/admin/seasons/{seasonId}/episodes", seasonId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(episodeDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.episodeNumber").value(1));
    }

    @Test
    @DisplayName("Get episode by ID should return EpisodeDTO")
    public void getEpisodeById() throws Exception {
        Long episodeId = 1L;
        EpisodeDTO episodeDTO = new EpisodeDTO();
        episodeDTO.setId(episodeId);
        episodeDTO.setEpisodeNumber(1);

        when(episodeManagementService.getEpisodeById(episodeId)).thenReturn(episodeDTO);

        mockMvc.perform(get("/api/admin/episodes/{id}", episodeId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(episodeId))
                .andExpect(jsonPath("$.episodeNumber").value(1));
    }

    @Test
    @DisplayName("Update episode should return updated EpisodeDTO")
    public void updateEpisode() throws Exception {
        Long episodeId = 1L;
        EpisodeDTO episodeDTO = new EpisodeDTO();
        episodeDTO.setId(episodeId);
        episodeDTO.setEpisodeNumber(2);

        when(episodeManagementService.updateEpisode(eq(episodeId), any(EpisodeDTO.class))).thenReturn(episodeDTO);

        mockMvc.perform(put("/api/admin/episodes/{id}", episodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(episodeDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(episodeId))
                .andExpect(jsonPath("$.episodeNumber").value(2));
    }

    @Test
    @DisplayName("Delete episode should return no content")
    public void deleteEpisode() throws Exception {
        Long episodeId = 1L;

        doNothing().when(episodeManagementService).deleteEpisode(episodeId);

        mockMvc.perform(delete("/api/admin/episodes/{id}", episodeId))
                .andExpect(status().isNoContent());
    }
}