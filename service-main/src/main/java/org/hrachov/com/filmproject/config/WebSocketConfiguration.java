package org.hrachov.com.filmproject.config;

import org.hrachov.com.filmproject.model.notification.LoggingChannelInterceptor;
import org.hrachov.com.filmproject.security.JwtHandshakeHandler;
import org.hrachov.com.filmproject.security.JwtHandshakeInterceptor;
import org.hrachov.com.filmproject.security.JwtUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

import static org.thymeleaf.spring6.context.SpringContextUtils.getApplicationContext;

@Profile("!test")
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final LoggingChannelInterceptor loggingChannelInterceptor;
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final JwtHandshakeHandler jwtHandshakeHandler;

    public WebSocketConfiguration(JwtHandshakeInterceptor jwtHandshakeInterceptor,
                                  LoggingChannelInterceptor loggingChannelInterceptor,
                                  JwtUtils jwtUtils, UserDetailsService userDetailsService,
                                  JwtHandshakeHandler jwtHandshakeHandler) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.loggingChannelInterceptor = loggingChannelInterceptor;
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
        this.jwtHandshakeHandler = jwtHandshakeHandler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(jwtHandshakeHandler)      // <-- здесь
                //.addInterceptors(jwtHandshakeInterceptor)      // ваш Interceptor остаётся для других нужд
                .withSockJS()
                .setHeartbeatTime(25_000)
                .setDisconnectDelay(5_000);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(loggingChannelInterceptor);
        registration.taskExecutor().corePoolSize(8).maxPoolSize(16);
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor().corePoolSize(8).maxPoolSize(16);
    }
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost("localhost")
                .setRelayPort(61613)
                .setClientLogin("admin")
                .setClientPasscode("secret")
                .setSystemLogin("admin")
                .setSystemPasscode("secret")
                .setSystemHeartbeatSendInterval(20000)
                .setSystemHeartbeatReceiveInterval(20000)
                // Указываем виртуальный хост RabbitMQ. По умолчанию это "/",
                // но явное указание - хорошая практика.
                .setVirtualHost("/")
                // These are the critical settings for user registry
                .setUserDestinationBroadcast("/topic/unresolved-user-destinations")
                .setUserRegistryBroadcast("/topic/user-registry");

        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
}
