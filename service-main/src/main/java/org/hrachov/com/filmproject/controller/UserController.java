package org.hrachov.com.filmproject.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.exception.UserNotFoundException;
import org.hrachov.com.filmproject.exception.UserNotLoggedException;
import org.hrachov.com.filmproject.model.Comment;
import org.hrachov.com.filmproject.model.Favorites;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.dto.CommentDTO;
import org.hrachov.com.filmproject.model.dto.FavoritesDTO;
import org.hrachov.com.filmproject.model.dto.UserDTO;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.service.UserService;
import org.hrachov.com.filmproject.service.FavoritesService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final FavoritesService favoritesService;

    @Transactional
    @GetMapping("/profile")
    public ResponseEntity<UserDTO> getProfile() {

        User user;
        try{
             user = userService.findByUsername(currentUserService.getCurrentUser().getUsername());
        }catch (Exception e) {
            throw new UserNotLoggedException("User not logged in");
        }

        //UserDTO userDTO = objectMapper.convertValue(user, UserDTO.class);
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setEmail(user.getEmail());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setRegistrationDate(user.getRegistrationDate());

        return new ResponseEntity<>(userDTO, HttpStatus.OK);
    }
    @GetMapping("/comments")
    public ResponseEntity<Page<CommentDTO>> getComments(@RequestParam int id,
                                                        @RequestParam(required = false,defaultValue = "0") int page,
                                                        @RequestParam(required = false,defaultValue = "5") int size,
                                                        @RequestParam(required = false, defaultValue = "time,desc") String sort) {
        User user = userService.findUserById(id);
        if(user == null) {
            throw new UserNotFoundException((long) id);
        }

        String[] parts = sort.split(",");
        String sortField = parts[0]; // e.g., "time"
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;

        // --- FIX IS HERE ---
        // Create a Sort object first
        Sort sortOrder = Sort.by(direction, sortField);
        // Then create the PageRequest using the Sort object
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        // --- END FIX ---

        // Alternative using a different PageRequest.of overload:
        // Pageable pageable = PageRequest.of(page, size, direction, sortField);


        Page<Comment> comments = userService.findCommentsByUser(user, pageable);
        Page<CommentDTO> commentsDTO = comments.map(comment -> {
            CommentDTO commentDTO = new CommentDTO();
            commentDTO.setId(comment.getId());
            commentDTO.setText(comment.getText());
            commentDTO.setFilmId(comment.getFilm().getId());
            // You might want to add time, likes, dislikes here too if needed in the DTO
            commentDTO.setTime(comment.getTime());
            // Assuming you have logic elsewhere to calculate/fetch likes/dislikes
            // commentDTO.setLikes(...);
            // commentDTO.setDislikes(...);
            return commentDTO;
        });

        return new ResponseEntity<>(commentsDTO, HttpStatus.OK);
    }
    @GetMapping("/favorites")
    public ResponseEntity<Page<FavoritesDTO>> getFavorites(@RequestParam(required = false) int id,
                                                           @RequestParam(required = true, defaultValue = "0") int page,
                                                           @RequestParam(required = true, defaultValue = "5") int size,
                                                           @RequestParam(required = true,defaultValue = "time,desc") String sort) {

        User user = userService.findUserById(id);
        if(user == null) {
            throw new UserNotFoundException((long) id);
        }

        String[] parts = sort.split(",");
        String sortField = parts[0]; // e.g., "time"
        Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;

        Sort sortOrder = Sort.by(direction, sortField);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<Favorites> favorites = favoritesService.findFavoritesByUser(user.getId(), pageable);
        Page<FavoritesDTO> favoritesDTOS = favorites.map(favorites1 ->{
            FavoritesDTO favoriteDTO = new FavoritesDTO();
            favoriteDTO.setId(favorites1.getId());
            favoriteDTO.setUserId(favorites1.getUserId());
            favoriteDTO.setDate(favorites1.getDate());
            favoriteDTO.setFilmId(favorites1.getFilmId());
            favoriteDTO.setType(favorites1.getType());
           return favoriteDTO;

        });
        System.out.println(favoritesDTOS);
        return new ResponseEntity<>(favoritesDTOS, HttpStatus.OK);

    }
    @GetMapping("/allUsers")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> users = userService.findAllUsers();
        List<UserDTO> userDTOS = users.stream().map(user->{
            UserDTO userDTO = new UserDTO();
            userDTO.setId(user.getId());
            userDTO.setUsername(user.getUsername());
            userDTO.setFirstName(user.getFirstName());
            userDTO.setLastName(user.getLastName());
            userDTO.setEmail(user.getEmail());
            return userDTO;
        }).collect(Collectors.toList());
        return new ResponseEntity<>(userDTOS, HttpStatus.OK);
    }
}
