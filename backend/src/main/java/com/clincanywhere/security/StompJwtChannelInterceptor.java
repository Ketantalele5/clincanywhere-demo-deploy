package com.clincanywhere.security;

import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.jspecify.annotations.NonNull;

@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {
    private final JwtService jwt;
    private final AccountUserDetailsService users;
    public StompJwtChannelInterceptor(JwtService jwt, AccountUserDetailsService users) { this.jwt = jwt; this.users = users; }
    @Override public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> headers = accessor.getNativeHeader("Authorization");
            String authorization = headers == null ? null : headers.stream().findFirst().orElse(null);
            if (authorization == null || !authorization.startsWith("Bearer ")) {
                throw new IllegalArgumentException("WebSocket connection requires a bearer token");
            }
            try {
                String token = authorization.substring(7);
                var claims = jwt.claims(token);
                String username = claims.getSubject();
                var user = users.loadUserByUsername(username);
                if (!jwt.isValid(claims, user)) throw new IllegalArgumentException("Invalid WebSocket token");
                accessor.setUser(new UsernamePasswordAuthenticationToken(user.getUsername(), null, user.getAuthorities()));
            } catch (RuntimeException invalidToken) {
                throw new IllegalArgumentException("Invalid WebSocket authentication", invalidToken);
            }
        }
        String destination = accessor == null ? null : accessor.getDestination();
        if (accessor != null && accessor.getUser() != null && destination != null
                && (destination.startsWith("/topic/") || destination.startsWith("/app/"))) {
            throw new IllegalArgumentException("Client publish destinations are disabled");
        }
        if (accessor != null && StompCommand.SEND.equals(accessor.getCommand())) {
            throw new IllegalArgumentException("Client message publishing is disabled");
        }
        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                && (destination == null || !(destination.equals("/user/queue/messages")
                || destination.equals("/user/queue/notifications")))) {
            throw new IllegalArgumentException("Subscription destination is not allowed");
        }
        return message;
    }
}
