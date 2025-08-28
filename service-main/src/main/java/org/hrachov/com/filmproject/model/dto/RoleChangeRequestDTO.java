package org.hrachov.com.filmproject.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Set;

@Data
public class RoleChangeRequestDTO {

    @NotNull
    private Long userId;

    @NotNull // Пустой Set будет означать удаление всех ролей (кроме, возможно, одной роли по умолчанию, если такая логика есть)
    // Если нужно, чтобы хотя бы одна роль всегда была, используйте @NotEmpty
    private Set<String> roles; // Набор строковых представлений ролей, например: ["ROLE_ADMIN", "ROLE_VIP"]
}