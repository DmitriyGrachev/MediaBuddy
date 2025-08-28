package org.hrachov.com.filmproject.model.notification;

import lombok.AllArgsConstructor;
import org.hrachov.com.filmproject.websocket.UserStatusNotifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@AllArgsConstructor
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);
    // userSessions: maps username to its current session ID
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();
    // sessionUsers: maps session ID to username
    private final Map<String, String> sessionUsers = new ConcurrentHashMap<>();
    private final RedisTemplate<String, String> redisTemplate;
    private final UserStatusNotifierService userStatusNotifierService;
    private final RabbitAdmin rabbitAdmin;


    // Executor for scheduling delayed disconnects
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Map to hold scheduled disconnect tasks by username
    private final Map<String, Runnable> pendingDisconnects = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        Principal principal = accessor.getUser();

        if (principal != null && principal.getName() != null && sessionId != null) {
            String username = principal.getName();

            // Отменяем отложенное отключение, если оно есть
            Runnable pendingTask = pendingDisconnects.remove(username);
            if (pendingTask != null) {
                scheduler.execute(() -> {});
                logger.info("Cancelled pending disconnect for user: {}", username);
            }

            // Добавляем пользователя в Redis как подключенного
            redisTemplate.opsForSet().add("connected_users", username);
            redisTemplate.opsForValue().set("user:" + username + ":status", "online", Duration.ofSeconds(60));

            // Уведомляем всех через WebSocket
            userStatusNotifierService.broadcastUserStatus(username, true);

            logger.info("User '{}' connected with session '{}'.", username, sessionId);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        if (sessionId != null) {
            String username = sessionUsers.get(sessionId);
            if (username != null) {
                Runnable disconnectTask = () -> {
                    // Проверяем, действительно ли пользователь отключен
                    Boolean isStillConnected = redisTemplate.opsForSet().isMember("connected_users", username);
                    if (isStillConnected != null && !isStillConnected) {
                        redisTemplate.opsForSet().remove("connected_users", username);
                        redisTemplate.opsForValue().set("user:" + username + ":status", "offline");

                        userStatusNotifierService.broadcastUserStatus(username, false);
                        //notifications-usersxzxakui
                        String queueName = "notifications-user" + sessionId;
                        rabbitAdmin.deleteQueue(queueName);

                        logger.info("User '{}' truly disconnected from session '{}'.", username, sessionId);
                    }
                    pendingDisconnects.remove(username);
                };

                pendingDisconnects.put(username, disconnectTask);
                scheduler.schedule(disconnectTask, 3, TimeUnit.SECONDS);
            }
        }
    }

    public boolean isUserConnected(String username) {
        String status = redisTemplate.opsForValue().get("user:" + username + ":status");
        return "online".equals(status);
    }

    public Set<String> getConnectedUsers() {
        return new HashSet<>(userSessions.keySet());
    }

    public String getSessionIdForUser(String username) {
        return userSessions.get(username);
    }

    public Map<String, String> getUserSessionsMapForDebug() {
        return new ConcurrentHashMap<>(userSessions);
    }
}
