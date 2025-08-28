package org.hrachov.com.filmproject.security.securityDTO;

import lombok.Data;
import org.hrachov.com.filmproject.model.Role;

import java.util.Set;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String username;
    private String firstName;
    private String lastName;

}
