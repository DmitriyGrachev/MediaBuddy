package org.hrachov.com.filmproject.security.securityDTO;

import lombok.Data;

@Data
public class AuthRequest {
    private String username;
    private String password;
}
