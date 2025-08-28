package org.hrachov.com.filmproject.repository.jpa;

import org.hrachov.com.filmproject.model.Comment;
import org.hrachov.com.filmproject.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Page<Comment> findAllById(long id, Pageable pageable);

    Page<Comment> findAllCommentsById(Long id, Pageable pageable);

    User getUserById(Long id);
    Optional<User> getUserByUsernameOrEmail(String username, String email);

    User getUserByEmail(String email);

}
