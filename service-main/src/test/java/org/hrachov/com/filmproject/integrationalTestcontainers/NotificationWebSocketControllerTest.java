package org.hrachov.com.filmproject.integrationalTestcontainers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hrachov.com.filmproject.controller.NotificationWebSocketController;
import org.hrachov.com.filmproject.model.Role;
import org.hrachov.com.filmproject.model.User;
import org.hrachov.com.filmproject.model.notification.WebSocketEventListener;
import org.hrachov.com.filmproject.repository.jpa.UserRepository;
import org.hrachov.com.filmproject.security.*;
import org.hrachov.com.filmproject.service.impl.BlockServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.rabbit.core.RabbitAdmin;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;


import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.concurrent.TimeoutException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class NotificationWebSocketControllerTest extends IntegrationTestBase{
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private WebSocketEventListener webSocketEventListener; // Use our custom tracker instead
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    @Autowired
    private RabbitAdmin rabbitAdmin;
    @Autowired
    private UserRepository userRepository;
    @LocalServerPort
    private int port;

    @Autowired
    private JwtUtils jwtUtils; // Для создания токена

    @Autowired
    private NotificationWebSocketController notificationController; // Для вызова метода


    private final ObjectMapper objectMapper = new ObjectMapper();

    private String wsUrl;
    private WebSocketStompClient stompClient;
    private User testUser;
    @Autowired
    private BlockServiceImpl blockServiceImpl;
    @Autowired
    private SimpUserRegistry simpUserRegistry;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        // 1. Создаем пользователя в базе данных, чтобы Security мог его найти
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password");
        testUser.setRoles(new HashSet<Role>(List.of(Role.ROLE_REGULAR)));
        testUser.setEmail("testuser@gmail.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRegistrationDate(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        wsUrl = String.format("ws://localhost:%d/ws", port);

        // Настраиваем клиент, который умеет работать с SockJS
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }
    @Test
    @DisplayName("Test send notification")
    public void testSendNotification() throws ExecutionException, InterruptedException, TimeoutException {
        // 1. ГОТОВИМСЯ ПОЛУЧИТЬ СООБЩЕНИЕ
        // CompletableFuture будет ждать, пока придет ответ от сервера
        CompletableFuture<Map<String, Object>> resultFuture = new CompletableFuture<>();

        // Создаем валидный токен для нашего тестового пользователя
        UserDetailsImpl userDetails = new UserDetailsImpl(testUser);
        String token = jwtUtils.generateToken(userDetails);

        // 2. ПОДКЛЮЧАЕМСЯ И ПОДПИСЫВАЕМСЯ
        StompSession stompSession = stompClient.connectAsync(wsUrl + "?token=" + token, new StompSessionHandlerAdapter() {
        }).get(5, TimeUnit.SECONDS);

        stompSession.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                // Указываем, в какой тип десериализовать полезную нагрузку
                return new TypeReference<Map<String, Object>>() {}.getType();
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // Когда сообщение получено, завершаем наш Future с результатом
                resultFuture.complete((Map<String, Object>) payload);
            }
        });

        // 3. ТРИГГЕР: ВЫЗЫВАЕМ ДЕЙСТВИЕ, КОТОРОЕ ОТПРАВИТ УВЕДОМЛЕНИЕ
        String notificationMessage = "Hello, testuser!";
        notificationController.sendNotification(testUser.getUsername(), notificationMessage);

        // 4. ПРОВЕРКА: ЖДЕМ ПОЛУЧЕНИЯ СООБЩЕНИЯ И ПРОВЕРЯЕМ ЕГО СОДЕРЖИМОЕ
        Map<String, Object> receivedNotification = resultFuture.get(10, TimeUnit.SECONDS);

        assertThat(receivedNotification).isNotNull();
        assertThat(receivedNotification.get("message")).isEqualTo(notificationMessage);
        assertThat(receivedNotification.get("type")).isEqualTo("REACTION");

        // 5. ОЧИСТКА
        stompSession.disconnect();
    }
    @Test
    @DisplayName("Check if ping works")
    public void testPing() throws ExecutionException, InterruptedException, TimeoutException, JsonProcessingException {
        CompletableFuture<Map<String, String>> expectingPing = new CompletableFuture<>();

        UserDetailsImpl userDetails = new UserDetailsImpl(testUser);
        String token = jwtUtils.generateToken(userDetails);

        StompSession stompSession = stompClient.connectAsync(wsUrl + "?token=" + token, new StompSessionHandlerAdapter() {
        }).get(5, TimeUnit.SECONDS);

        stompSession.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                // Указываем, в какой тип десериализовать полезную нагрузку
                return new TypeReference<Map<String, Object>>() {}.getType();
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // Когда сообщение получено, завершаем наш Future с результатом
                expectingPing.complete((Map<String, String>) payload);
            }
        });
        String username = null;
        for(SimpUser simpUser : simpUserRegistry.getUsers()){
            Principal principal = simpUser.getPrincipal();
            System.out.println(principal.getName() + " Found principal " + principal);
            if(principal != null) {
                stompSession.send("/app/ping", null); // "/app" — префикс MessageMapping
                username = principal.getName();
            }
        }

        Map<String, String> receivedNotification = expectingPing.get(60, TimeUnit.SECONDS);
        assertThat(receivedNotification).isNotNull();
        assertThat(receivedNotification.get("type")).isEqualTo("PONG");

        if(username != null) {
            String str = objectMapper.writeValueAsString(redisTemplate.opsForValue().get("user:" + username + ":status"));
            System.out.println(str);
            assertTrue(str.contains("online"));
        }
        stompSession.disconnect();
    }

}
