package org.hrachov.com.filmproject.controller;
import org.hrachov.com.filmproject.exception.UserNotLoggedException;
import org.hrachov.com.filmproject.model.*;
import org.hrachov.com.filmproject.model.dto.FavoritesDTO;
import org.hrachov.com.filmproject.security.CurrentUserService;
import org.hrachov.com.filmproject.security.JwtUtils;
import org.hrachov.com.filmproject.security.UserDetailsImpl;
import org.hrachov.com.filmproject.service.impl.FavoritesServiceImpl;
import org.hrachov.com.filmproject.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private CurrentUserService currentUserService;

    @MockBean
    private UserServiceImpl userService;

    @MockBean
    private FavoritesServiceImpl favoritesService;
    @Autowired
    private CharacterEncodingFilter characterEncodingFilter;


    @Test
    void getUserProfile_shouldReturnUserDto_whenUserIsLoggedIn() throws Exception {
        // Set up the test user
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        Set<Role> authorities = new HashSet<>();
        authorities.add(Role.valueOf("ROLE_REGULAR"));
        testUser.setRoles(authorities);

        UserDetailsImpl userDetails = new UserDetailsImpl(testUser);

        // Configure mock behavior
        when(currentUserService.getCurrentUser()).thenReturn(userDetails);
        when(userService.findByUsername("testuser")).thenReturn(testUser);

        // Perform the GET request and verify the response
        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }
    @Test
    void getUserProfile_shouldReturnUserDto_whenUserIsNotLoggedIn() throws Exception {
        when(currentUserService.getCurrentUser())
                .thenThrow(new RuntimeException("Something went wrong"));

        mockMvc.perform(get("/api/user/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("User not logged in"))
                .andExpect(jsonPath("$.message").value("User not logged in")); // или другой текст, если он в Exception

    }

    @Test
    void getComments_ShouldReturnPage_whenUserIsLoggedIn() throws Exception {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        Set<Role> authorities = new HashSet<>();
        authorities.add(Role.ROLE_REGULAR);
        testUser.setRoles(authorities);

        Film film = new Film();
        film.setId(1L);
        film.setTitle("Film 1");

        Pageable pageable = PageRequest.of(0, 5, Sort.by("time").descending());
        Page<Comment> page = new PageImpl<>(List.of(
                new Comment(testUser,film,"first comment"),
                new Comment(testUser,film,"second comment"),
                new Comment(testUser,film,"third comment"),
                new Comment(testUser,film,"fourth comment"),
                new Comment(testUser,film,"fifth comment")
        ),pageable,5);

        when(userService.findUserById(1L))
                .thenReturn(testUser);
        when(userService.findCommentsByUser(testUser,pageable))
                .thenReturn(page);

        mockMvc.perform(get("/api/user/comments?id=1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))       // номер страницы, 0-based
                .andExpect(jsonPath("$.size").value(5))         // размер страницы
                .andExpect(jsonPath("$.content[0].text").value("first comment"));
    }
    @Test
    void getComments_ShouldThrowException() throws Exception {
        when(userService.findUserById(1L))
        .thenReturn(null);

        mockMvc.perform(get("/api/user/comments?id=1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("User not found"))
                .andExpect(jsonPath("$.message").value("User with id 1 not found"));
    }

    @Test
    void getPageFavorites_ShouldReturnPage_whenUserIsLoggedIn() throws Exception {
        Pageable pageable = PageRequest.of(0, 5, Sort.by("time").descending());
        Page page= new PageImpl(List.of(
                new Favorites("1",1L,1L,"movie",null),
                new Favorites("2",1L,2L,"movie",null),
                new Favorites("3",1L,3L,"serial",null),
                new Favorites("4",1L,4L,"serial",null),
                new Favorites("5",1L,5L,"movie",null)
                ),pageable,5);

        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        when(userService.findUserById(1L))
        .thenReturn(testUser);
        when(favoritesService.findFavoritesByUser(testUser.getId(),pageable))
                .thenReturn(page);

        mockMvc.perform(get("/api/user/favorites?id=1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.content[0].type").value("movie"))
                .andExpect(jsonPath("$.content[1].type").value("movie"))
                .andExpect(jsonPath("$.content[2].type").value("serial"))
                .andExpect(jsonPath("$.content[3].type").value("serial"))
                .andExpect(jsonPath("$.content[4].type").value("movie"));
    }
    @Test
    void getPageFavorites_ShouldThrowException() throws Exception {
        when(userService.findUserById(1L))
        .thenReturn(null);

        mockMvc.perform(get("/api/user/favorites?id=1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("User not found"))
                .andExpect(jsonPath("$.message").value("User with id 1 not found"));

    }

    @Test
    void getAllUsers_ShouldReturnList() throws Exception {
        User testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        User testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setUsername("testuser2");

        when(userService.findAllUsers())
                .thenReturn(List.of(testUser, testUser2));

        mockMvc.perform(get("/api/user/allUsers"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.[0].id").value(1))
                .andExpect(jsonPath("$.[0].username").value("testuser"))
                .andExpect(jsonPath("$.[1].id").value(2))
                .andExpect(jsonPath("$.[1].username").value("testuser2"));

    }
}
