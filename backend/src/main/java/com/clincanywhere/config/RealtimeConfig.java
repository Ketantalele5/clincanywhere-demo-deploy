package com.clincanywhere.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;
import com.clincanywhere.security.StompJwtChannelInterceptor;
import org.jspecify.annotations.NonNull;

@Configuration
@EnableWebSocketMessageBroker
public class RealtimeConfig implements WebSocketMessageBrokerConfigurer {
    private final String[] allowedOrigins;
    private final StompJwtChannelInterceptor authentication;
    private final String relayHost;
    private final int relayPort;
    private final String relayLogin;
    private final String relayPasscode;
    public RealtimeConfig(@Value("${app.cors.allowed-origins:http://localhost:5173}") String origins,
                          StompJwtChannelInterceptor authentication,
                          @Value("${app.realtime.relay.host:}") String relayHost,
                          @Value("${app.realtime.relay.port:61613}") int relayPort,
                          @Value("${app.realtime.relay.login:}") String relayLogin,
                          @Value("${app.realtime.relay.passcode:}") String relayPasscode,
                          @Value("${app.realtime.shared-events.enabled:false}") boolean sharedEventsEnabled,
                          org.springframework.core.env.Environment environment) {
        this.allowedOrigins = java.util.Arrays.stream(origins.split(",")).map(origin -> origin.trim()).toArray(String[]::new);
        this.authentication = authentication;
        this.relayHost=relayHost; this.relayPort=relayPort; this.relayLogin=relayLogin; this.relayPasscode=relayPasscode;
        if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("prod")) && relayHost.isBlank())
            throw new IllegalStateException("Production requires a shared STOMP broker relay; set STOMP_RELAY_HOST and credentials");
        if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("prod"))
                && (relayLogin.length() < 16 || relayPasscode.length() < 32
                || relayPasscode.startsWith("CHANGE_ME")))
            throw new IllegalStateException("Production broker credentials must use a dedicated username and high-entropy password");
        if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("prod")) && !sharedEventsEnabled)
            throw new IllegalStateException("Production requires shared notification fanout; set APP_REALTIME_SHARED_EVENTS_ENABLED=true");
        if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("prod"))) {
            String rabbitHost = environment.getProperty("spring.rabbitmq.host", "localhost");
            String rabbitUser = environment.getProperty("spring.rabbitmq.username", "");
            String rabbitPassword = environment.getProperty("spring.rabbitmq.password", "");
            if (rabbitHost.isBlank() || rabbitHost.equals("localhost") || rabbitUser.length() < 16 || rabbitPassword.length() < 32)
                throw new IllegalStateException("Production requires dedicated RabbitMQ AMQP connection settings and strong credentials");
        }
        if (sharedEventsEnabled && relayHost.isBlank())
            throw new IllegalStateException("Shared notification events require the STOMP relay to be configured");
    }
    @Override public void configureMessageBroker(@NonNull MessageBrokerRegistry registry) {
        if (!relayHost.isBlank()) {
            if (relayLogin.isBlank() || relayPasscode.isBlank()) throw new IllegalStateException("Broker relay credentials are required when relay host is configured");
            var relay = registry.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(java.util.Objects.requireNonNull(relayHost)).setRelayPort(relayPort)
                    .setClientLogin(java.util.Objects.requireNonNull(relayLogin))
                    .setClientPasscode(java.util.Objects.requireNonNull(relayPasscode))
                    .setSystemLogin(java.util.Objects.requireNonNull(relayLogin))
                    .setSystemPasscode(java.util.Objects.requireNonNull(relayPasscode));
            relay.setUserRegistryBroadcast("/topic/clincy-user-registry");
            relay.setUserDestinationBroadcast("/topic/clincy-unresolved-user-destination");
        } else registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
    @Override public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOrigins(java.util.Objects.requireNonNull(allowedOrigins));
    }
    @Override public void configureClientInboundChannel(@NonNull ChannelRegistration registration) { registration.interceptors(authentication); }
}
