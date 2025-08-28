package org.hrachov.com.filmproject.security;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class JwtHandshakeHandler extends DefaultHandshakeHandler {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    public JwtHandshakeHandler(JwtUtils jwtUtils,
                               UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        // 1) извлекаем токен из query-param или заголовка
        String token = null;
        if (request instanceof ServletServerHttpRequest servletReq) {
            token = servletReq.getServletRequest().getParameter("token");
            if (token == null) {
                String authHeader = servletReq.getServletRequest().getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            }
        }

        if (!StringUtils.hasText(token)) {
            throw new AuthenticationCredentialsNotFoundException("No JWT token found in handshake request");
        }

        // 2) валидируем токен
        String username = jwtUtils.extractUsername(token);
        UserDetails user = userDetailsService.loadUserByUsername(username);
        if (!jwtUtils.validateToken(token, user)) {
            throw new BadCredentialsException("Invalid JWT token");
        }

        // 3) создаём Authentication и устанавливаем в SecurityContext
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 4) возвращаем Principal (будет доступен в simpUser)
        return auth;
    }
}
