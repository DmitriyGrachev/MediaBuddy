package org.hrachov.com.filmproject.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final CustomOAuth2SuccessHandler oauth2SuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .authorizeHttpRequests(auth -> auth
                        // 🎯 Явно разрешённые маршруты без авторизации
                        .requestMatchers(
                                "/api/auth/**",          // POST login/register
                                "/h2-console/**",        // БД-консоль
                                "/testing/**",           // тестовые endpoints
                                "/css/**", "/js/**", "/images/**", "/favicon.ico", // статика
                                "/ws/**",                // WebSocket
                                "/api/search/films/**",  // Поиск
                                "/api/admin/addmovie",   // Специальное разрешение
                                "/api/films/**"          // <-- Тоже явно разрешаем
                        ).permitAll()

                        // 🎯 Разрешаем все GET-запросы (должно быть ниже специфичных правил!)
                        .requestMatchers(HttpMethod.GET).permitAll()

                        // 🔐 VIP-зона
                        .requestMatchers("/api/vip/**").hasAnyRole("VIP", "ADMIN")
                        .requestMatchers("/api/mpeg/**").hasRole("VIP")

                        // 🔐 Чат и рейтинг — требует входа
                        .requestMatchers("/api/chat/**").authenticated()
                        .requestMatchers("/api/rating/**").authenticated()

                        // 🔐 Админка
                        .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

                        // 🛡 Всё остальное — требует аутентификации
                        .anyRequest().authenticated()
                ).oauth2Login(oauth -> oauth
                        // точка старта: /oauth2/authorization/{registrationId}
                        .loginPage("/login")                      // ваш кастомный login page
                        .authorizationEndpoint(authz ->
                                authz.baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(redir ->
                                redir.baseUri("/login/oauth2/code/*"))
                        .successHandler(oauth2SuccessHandler)     // наш handler
                )
                .sessionManagement(sess -> sess
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                )
                .build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
