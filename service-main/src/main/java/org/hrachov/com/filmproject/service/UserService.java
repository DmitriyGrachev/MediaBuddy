package org.hrachov.com.filmproject.service;

import org.hrachov.com.filmproject.model.Comment;
import org.hrachov.com.filmproject.model.Film;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.dto.ResetPassDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    User findUserById(long id);

    User findByUsername(String username);
    Page<Comment> findCommentsByUser(User user, Pageable pageable);
    void resetPassword(ResetPassDTO resetPassDTO);
    List<User> findAllUsers();
}
