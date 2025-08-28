package org.hrachov.com.filmproject.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BlockUserRequest {
    @NotBlank
    private long userId;
    @NotBlank
    private String reason;
    //Can be null
    private LocalDateTime blockedUntil;

    public BlockUserRequest() {

    }
}
