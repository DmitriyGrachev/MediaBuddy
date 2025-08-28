package org.hrachov.com.filmproject.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.server.HandshakeFailureException;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    private final JwtUtils jwtUtils;
    private final org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Autowired
    public JwtHandshakeInterceptor(JwtUtils jwtUtils,
                                   org.springframework.security.core.userdetails.UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        // Пропускаем все SockJS-транспорты, оставляя проверку только на реальный WebSocket-upgrade
        String path = request.getURI().getPath();
        if (!path.endsWith("/websocket")) {
            return true;
        }

        if (!(request instanceof ServletServerHttpRequest servlet)) {
            logger.warn("Handshake failed: not a ServletServerHttpRequest, path={}", path);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        HttpServletRequest http = servlet.getServletRequest();

        // 1) Извлекаем из ?token=
        String token = http.getParameter("token");

        // 2) Или из заголовка Authorization
        if (!StringUtils.hasText(token)) {
            String authHeader = http.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }

        if (!StringUtils.hasText(token)) {
            logger.warn("Handshake failed: no JWT token provided");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // Извлекаем имя пользователя
        String username;
        try {
            username = jwtUtils.extractUsername(token);
        } catch (Exception ex) {
            logger.warn("Handshake failed: cannot extract username from token", ex);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // Загружаем UserDetails и валидируем токен
        var userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtUtils.validateToken(token, userDetails)) {
            logger.warn("Handshake failed: token validation failed for user={}", username);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        // Создаём Authentication как Principal
        UsernamePasswordAuthenticationToken principal =
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );

        // Кладём в атрибуты под ключом DefaultHandshakeHandler.PRINCIPAL_ATTRIBUTE,
        // чтобы Spring автоматически подхватил его как simpUser
        attributes.put("principal", principal);

        logger.info("Handshake success: user '{}' bound to WebSocket session", username);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               @Nullable Exception exception) {
        if (exception != null) {
            logger.error("WebSocket handshake error: {}", exception.getMessage(), exception);
        } else {
            logger.debug("WebSocket handshake completed: URI={}", request.getURI());
        }
    }
}