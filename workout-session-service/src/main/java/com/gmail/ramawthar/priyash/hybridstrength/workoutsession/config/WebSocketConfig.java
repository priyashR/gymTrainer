package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time session state updates.
 * Uses STOMP over WebSocket with a simple in-memory message broker.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketJwtInterceptor webSocketJwtInterceptor;

    public WebSocketConfig(WebSocketJwtInterceptor webSocketJwtInterceptor) {
        this.webSocketJwtInterceptor = webSocketJwtInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable a simple in-memory broker for topic subscriptions
        registry.enableSimpleBroker("/topic");
        // Prefix for messages from client to server (not used currently but good practice)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP endpoint for WebSocket connections
        registry.addEndpoint("/ws/sessions")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Register JWT interceptor for WebSocket CONNECT authentication
        registration.interceptors(webSocketJwtInterceptor);
    }
}
