package org.hrachov.com.filmproject.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.hrachov.com.filmproject.model.Role;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import software.amazon.awssdk.services.connect.model.UpdateUserIdentityInfoResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String sub = oAuth2User.getAttribute("sub"); // Google user ID
        String password = UUID.randomUUID().toString() + "/oauth";
        // 1. Найти пользователя или создать нового
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(email.split("@")[0]);
            newUser.setFirstName(name);
            newUser.setRegistrationDate(LocalDateTime.now());
            newUser.setProvider("google");
            newUser.setProviderId(sub);
            newUser.setRoles(Set.of(Role.ROLE_REGULAR));
            newUser.setPassword(password);
            return userRepository.save(newUser);
        });

        // 2. Сформировать UserDetailsImpl вручную
        UserDetailsImpl userDetails = new UserDetailsImpl(user);

        // 3. Сгенерировать токен
        String token = jwtUtils.generateToken(userDetails);

        // 4. Редирект на фронт с токеном в URL
        String redirectUrl = UriComponentsBuilder.fromUriString("http://localhost:8080/oauth2/success") // Ваш URL фронтенда
                .queryParam("token", token)
                .build().toUriString();
        response.sendRedirect(redirectUrl);
    }
}
