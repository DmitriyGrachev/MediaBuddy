package org.hrachov.com.filmproject.service;

import org.hrachov.com.filmproject.exception.UserNotFoundException;
import org.hrachov.com.filmproject.model.Comment;
import org.hrachov.com.filmproject.model.Film;
import org.hrachov.com.filmproject.model.PasswordResetToken;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.dto.ResetPassDTO;
import org.hrachov.com.filmproject.repository.jpa.CommentRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.repository.mongo.PasswordResetTokenRepository;
import org.hrachov.com.filmproject.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CommentRepository commentRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void findByUsername_shouldReturnUser_whenUserExists() {
        User expectedUser = new User();
        expectedUser.setId(1L);
        expectedUser.setUsername("testuser");

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(expectedUser));

        User actualUser = userService.findByUsername("testuser");

        assertNotNull(actualUser);
        assertEquals(expectedUser.getId(), actualUser.getId());
        assertEquals(expectedUser.getUsername(), actualUser.getUsername());

        verify(userRepository, times(1)).findByUsername("testuser");

    }

    @Test
    void findByUsername_shouldThrow_whenUserDoesNotExist() {
        when(userRepository.findByUsername("testuser")).thenThrow(new UserNotFoundException("testuser"));

        assertThrows(UserNotFoundException.class, () -> userService.findByUsername("testuser"));

        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void findCommentsByUser_shouldReturnComments_whenUserExists() {
        User expectedUser = new User();
        expectedUser.setId(1L);
        expectedUser.setUsername("testuser");

        Film film1 = new Film();
        film1.setId(1L);
        film1.setTitle("FIlm1");

        Film film2 = new Film();
        film2.setId(2L);
        film2.setTitle("Film2");

        ArrayList<Comment> comments = new ArrayList<>();
        comments.add(new Comment(expectedUser,film1,"comment film 1"));
        comments.add(new Comment(expectedUser,film2,"comment film 2"));
        Page<Comment> expectedComments = new PageImpl<Comment>(comments);

        Pageable pageable = PageRequest.of(0, 2);
        when(commentRepository.findAllCommentsByUser(expectedUser,pageable)).thenReturn(expectedComments);


        Page<Comment> actualComments = userService.findCommentsByUser(expectedUser,pageable);
        assertNotNull(actualComments);
        assertEquals(expectedComments.getTotalElements(), actualComments.getTotalElements());
        assertEquals(expectedComments.getTotalPages(), actualComments.getTotalPages());
        assertEquals(expectedComments.getContent(), actualComments.getContent());
        assertEquals(expectedComments.getContent().get(0), actualComments.getContent().get(0));

        verify(commentRepository).findAllCommentsByUser(expectedUser,pageable);
    }

    private ResetPassDTO dto;
    private PasswordResetToken validToken;
    private User validUser;

    @BeforeEach
    void setUp() {
        dto = new ResetPassDTO();
        dto.setCode("token");
        dto.setEmail("user@example.com");
        dto.setPassword("newPass");
        dto.setPasswordConfirm("newPass");

        // Настраиваем валидный токен на завтра
        validToken = new PasswordResetToken();
        validToken.setToken("token");
        validToken.setExpiryDate(Instant.now().plus(1, ChronoUnit.DAYS));
        validToken.setUserId(42L);

        // Настраиваем валидного пользователя
        validUser = new User();
        validUser.setId(42L);
        validUser.setEmail("user@example.com");
        validUser.setPassword("oldHash");
    }

    @Test
    void resetPassword_shouldWork_allDataIsValid(){
        when(passwordResetTokenRepository.findByToken("token"))
                .thenReturn(Optional.of(validToken));

        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(validUser));

        when(passwordEncoder.encode("newPass"))
                .thenReturn("newHash");

        userService.resetPassword(dto);
        assertEquals("newHash", validUser.getPassword());
        verify(userRepository).save(validUser);
    }
    @Test
    void resetPassword_shouldThrow_whenUserDoesNotExist() {
        when(passwordResetTokenRepository.findByToken("token"))
                .thenReturn(Optional.of(validToken));
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.resetPassword(dto));
        verify(userRepository, never()).save(validUser);
    }
    @Test
    void resetPassword_shouldThrow_whenTokenInvalid() {
        when(passwordResetTokenRepository.findByToken("token"))
                .thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userService.resetPassword(dto));
        verify(userRepository, never()).save(validUser);
    }
    @Test
    void resetPassword_shouldThrow_whenTokenExpired() {
        PasswordResetToken token = new PasswordResetToken();
        //можно и Instant.now оставить но для наглядности лучше минус 2 минуты
        token.setExpiryDate(Instant.now().minusSeconds(120));
        token.setToken("token");
        token.setUserId(42L);

        when(passwordResetTokenRepository.findByToken("token"))
                .thenReturn(Optional.of(token));

        assertThrows(IllegalArgumentException.class, () -> userService.resetPassword(dto));
        verify(passwordResetTokenRepository,times(1)).delete(token);
        verify(userRepository, never()).save(validUser);
    }
    @Test
    void resetPassword_shouldThrow_whenDtoIncorrect() {
        ResetPassDTO dto = new ResetPassDTO();
        dto.setCode("token");
        dto.setPassword("first");
        dto.setPasswordConfirm("second");
        dto.setEmail("user@example.com");

        when(passwordResetTokenRepository.findByToken("token"))
                .thenReturn(Optional.of(validToken));
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(validUser));

        assertThrows(IllegalArgumentException.class, () -> userService.resetPassword(dto));
        verify(userRepository, never()).save(validUser);
    }
}
