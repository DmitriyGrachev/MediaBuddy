package org.hrachov.com.filmproject.websocket;

import lombok.AllArgsConstructor;
import org.checkerframework.checker.units.qual.A;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class UserStatusNotifierService {
    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastUserStatus(String username, boolean online) {
        Map<String, Object> statusPayload = new HashMap<>();
        statusPayload.put("type", "status");
        statusPayload.put("user", username);
        statusPayload.put("online", online);

        messagingTemplate.convertAndSend("/topic/user-status", statusPayload);
    }
}
