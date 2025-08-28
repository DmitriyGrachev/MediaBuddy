package org.hrachov.com.filmproject.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hrachov.com.filmproject.annotations.PasswordValidation;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPassDTO {

    @NotBlank(message = "CODE is required")
    private String code;

    @Email
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 30, message = "Password must be between 8 and 30 characters")
    @PasswordValidation
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String passwordConfirm;
}