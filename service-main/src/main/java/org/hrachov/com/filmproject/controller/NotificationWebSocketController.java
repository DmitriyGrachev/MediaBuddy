package org.hrachov.com.filmproject.controller;


import org.hrachov.com.filmproject.model.notification.WebSocketEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Controller
public class NotificationWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketEventListener webSocketEventListener; // Use our custom tracker instead
    private static final Logger logger = LoggerFactory.getLogger(NotificationWebSocketController.class);
    private final RedisTemplate<String,String> redisTemplate;

    public NotificationWebSocketController(SimpMessagingTemplate messagingTemplate,
                                           WebSocketEventListener webSocketEventListener,
                                           RedisTemplate<String,String> redisTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.webSocketEventListener = webSocketEventListener;
        this.redisTemplate = redisTemplate;
    }

    public void sendNotification(String username, String message) {
        try {
            boolean userConnected = webSocketEventListener.isUserConnected(username);

            if (userConnected) {
                Map<String, Object> notification = new HashMap<>();
                notification.put("message", message);
                notification.put("timestamp", Instant.now().toString());
                notification.put("type", "REACTION");

                messagingTemplate.convertAndSendToUser(
                        username,
                        "/queue/notifications",
                        notification
                );

                logger.info("Notification sent to user: {} (session: {})",
                        username, webSocketEventListener.getSessionIdForUser(username));
            } else {
                logger.warn("User {} is not connected, notification not sent. Connected users: {}",
                        username, webSocketEventListener.getConnectedUsers());
                // Store notification in database for later delivery
            }
        } catch (Exception e) {
            logger.error("Failed to send notification to user {}: {}", username, e.getMessage(), e);
        }
    }

    @MessageMapping("/ping")
    @SendToUser("/queue/notifications")
    public Map<String, String> handlePing(Principal principal) {
        redisTemplate.opsForValue().set("user:" + principal.getName() + ":status", "online", Duration.ofSeconds(20));

        Map<String, String> response = new HashMap<>();
        //Random random = new Random();
        //int randomInt = random.nextInt(100);
        response.put("type", "PONG");
        response.put("timestamp", Instant.now().toString());

        if (principal != null) {
            logger.debug("Ping received from user: {}", principal.getName());
        }

        return response;
    }
}