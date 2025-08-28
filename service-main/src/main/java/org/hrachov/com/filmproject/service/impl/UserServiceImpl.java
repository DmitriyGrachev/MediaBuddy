package org.hrachov.com.filmproject.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.exception.UserNotFoundException;
import org.hrachov.com.filmproject.model.Comment;
import org.hrachov.com.filmproject.model.Film;
import org.hrachov.com.filmproject.model.PasswordResetToken;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.dto.ResetPassDTO;
import org.hrachov.com.filmproject.repository.jpa.CommentRepository;
import org.hrachov.com.filmproject.repository.mongo.PasswordResetTokenRepository;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.JedisPooled;

import java.util.List;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public User findUserById(long id) {
        //TODO
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
    public User findByUsername(String username){
        return userRepository.findByUsername(username).orElseThrow(()-> new UserNotFoundException(username));
    }

    public Page<Comment> findCommentsByUser(User user, Pageable pageable) {
        return commentRepository.findAllCommentsByUser(user,pageable);
    }

    public void resetPassword(ResetPassDTO resetPassDTO) {
        // Проверка кода
        PasswordResetToken token = passwordResetTokenRepository.findByToken(resetPassDTO.getCode())
                .orElseThrow(() -> new IllegalArgumentException("Неверный или истекший код сброса"));

        if (token.isExpired()) {
            //Удаление вместе с ttl может нагружать бд на пока что не критично
            passwordResetTokenRepository.delete(token);
            throw new IllegalArgumentException("Срок действия кода истек");
        }

        // Проверка пользователя
        User user = userRepository.findByEmail(resetPassDTO.getEmail())
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        if (!(token.getUserId() == user.getId())) {
            throw new IllegalArgumentException("Код не соответствует пользователю");
        }

        // Проверка совпадения паролей
        if (!resetPassDTO.getPassword().equals(resetPassDTO.getPasswordConfirm())) {
            throw new IllegalArgumentException("Пароли не совпадают");
        }

        // Обновление пароля с кодированием
        user.setPassword(passwordEncoder.encode(resetPassDTO.getPassword()));
        userRepository.save(user);


    }
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
}
