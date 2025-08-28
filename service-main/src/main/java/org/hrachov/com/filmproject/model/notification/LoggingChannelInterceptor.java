package org.hrachov.com.filmproject.model.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class LoggingChannelInterceptor implements ChannelInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoggingChannelInterceptor.class);

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        String sessionId = accessor.getSessionId();
        // Using Optional to safely access user and its name
        String username = Optional.ofNullable(accessor.getUser())
                .map(java.security.Principal::getName)
                .orElse("N/A (pre-auth or no user)");

        // Log session attributes from the accessor, which should include what JwtHandshakeInterceptor put.
        String sessionAttributes = Optional.ofNullable(accessor.getSessionAttributes())
                .map(Object::toString)
                .orElse("N/A");

        logger.info("[CHANNEL_INTERCEPTOR PRE-SEND] Channel: '{}', SessionId: '{}', Command: '{}', User: '{}', Headers: '{}', SessionAttributes: {}",
                channel.toString(), // Get a string representation of the channel
                sessionId,
                command,
                username,
                message.getHeaders(),
                sessionAttributes); // Log session attributes

        if (StompCommand.CONNECT.equals(command)) {
            logger.info("[CHANNEL_INTERCEPTOR] STOMP CONNECT frame received. simpUser from session attributes should be here if handshake interceptor worked: {}", sessionAttributes);
        }
        return message;
    }

    @Override
    public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        logger.debug("[CHANNEL_INTERCEPTOR POST-SEND] Channel: '{}', SessionId: '{}', Command: '{}', Sent: '{}'",
                channel.toString(), accessor.getSessionId(), accessor.getCommand(), sent);
    }

    @Override
    public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (ex != null) {
            logger.error("[CHANNEL_INTERCEPTOR SEND-ERROR] Channel: '{}', SessionId: '{}', Command: '{}', Exception: {}",
                    channel.toString(), accessor.getSessionId(), accessor.getCommand(), ex.getMessage(), ex);
        } else {
            logger.debug("[CHANNEL_INTERCEPTOR SEND-COMPLETE] Channel: '{}', SessionId: '{}', Command: '{}', Sent: '{}'",
                    channel.toString(), accessor.getSessionId(), accessor.getCommand(), sent);
        }
    }
}