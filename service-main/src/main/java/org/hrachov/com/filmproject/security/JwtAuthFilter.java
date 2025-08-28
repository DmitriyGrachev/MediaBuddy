package org.hrachov.com.filmproject.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse,
                                    FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = httpServletRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }

        final String token = authHeader.substring(7);
        try {
            // Проверяем подпись токена
            if (!jwtUtils.isTokenSignatureValid(token)) {
                System.out.println("Invalid token signature: " + token);
                sendUnauthorizedResponse(httpServletResponse, "Invalid or malformed token");
                return;
            }

            // Проверяем срок действия
            if (jwtUtils.isTokenExpired(token)) {
                System.out.println("Token expired: " + token);
                sendUnauthorizedResponse(httpServletResponse, "Token has expired");
                return;
            }

            // Извлекаем имя пользователя
            final String username = jwtUtils.extractUsername(token);
            System.out.println("Extracted username from token: " + username);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {

                    List<String> roles = jwtUtils.extractRoles(token);
                    var authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    if (jwtUtils.validateToken(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpServletRequest));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (UsernameNotFoundException e) {
                    System.err.println("Token processing failed. Exception type: " + e.getClass().getName() + ", Message: " + e.getMessage());
                    e.printStackTrace(); // This will print the full stack trace to standard error
                    sendUnauthorizedResponse(httpServletResponse, "Token processing failed: " + e.getMessage());
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Token processing failed: " + e.getMessage());
            sendUnauthorizedResponse(httpServletResponse, "Token processing failed: " + e.getMessage());
            return;
        }

        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + message + "\"}");
    }
}