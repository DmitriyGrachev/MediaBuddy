package org.hrachov.com.filmproject.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hrachov.com.filmproject.model.Role;
import org.hrachov.com.filmproject.model.User;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    // private Set<String> roles; // Если у вас есть роли и они нужны на фронте

    private boolean isBlocked;
    private String blockReason;
    private String blockedUntil; // Можно LocalDateTime, но строка проще для JSON
    private String blockedAt;    // Аналогично
    private Set<String> roles;

    // Конструктор для маппинга
    public UserResponseDTO(User user, boolean isBlocked, String blockReason, String blockedUntil, String blockedAt) {
        this.id = user.getId();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.isBlocked = isBlocked;
        this.blockReason = blockReason;
        this.blockedUntil = blockedUntil;
        this.blockedAt = blockedAt;
        this.roles  = user.getRoles().stream().map(Role::name).collect(Collectors.toSet());
    }
}
