package org.hrachov.com.filmproject.config;

import org.hrachov.com.filmproject.model.notification.LoggingChannelInterceptor;
import org.hrachov.com.filmproject.security.JwtHandshakeHandler;
import org.hrachov.com.filmproject.security.JwtHandshakeInterceptor;
import org.hrachov.com.filmproject.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@Profile("test")
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${spring.rabbitmq.relay.host}")
    private String relayHost;

    @Value("${spring.rabbitmq.relay.port}")
    private int relayPort;

    @Value("${spring.rabbitmq.relay.username}")
    private String clientLogin;

    @Value("${spring.rabbitmq.relay.password}")
    private String clientPasscode;

    @Value("${spring.rabbitmq.relay.system-login}")
    private String systemLogin;

    @Value("${spring.rabbitmq.relay.system-passcode}")
    private String systemPasscode;

    @Value("${spring.rabbitmq.relay.virtual-host}")
    private String virtualHost;

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final LoggingChannelInterceptor loggingChannelInterceptor;
    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final JwtHandshakeHandler jwtHandshakeHandler;

    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor,
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
                // наружные (клиентские) креды
                .setClientLogin(clientLogin)
                .setClientPasscode(clientPasscode)
                // «внутренние» (system) креды для heartbeat‑пакетов
                .setSystemLogin(systemLogin)
                .setSystemPasscode(systemPasscode)
                // хост/порт
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                // виртуальный хост
                .setVirtualHost(virtualHost)
                // интервалы сердцебиения (рекомендую оставить)
                .setSystemHeartbeatSendInterval(20_000)
                .setSystemHeartbeatReceiveInterval(20_000)
                // маршруты для destination‑broadcast
                .setUserDestinationBroadcast("/topic/unresolved-user-destinations")
                .setUserRegistryBroadcast("/topic/user-registry");

        // префиксы ваших @MessageMapping и пользовательских destination’ов
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }
}
