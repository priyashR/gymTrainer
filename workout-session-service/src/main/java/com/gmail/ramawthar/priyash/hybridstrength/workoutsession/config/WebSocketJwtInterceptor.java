package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.config;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * STOMP channel interceptor that authenticates WebSocket CONNECT frames
 * using a JWT token passed via the native STOMP "Authorization" header
 * or a "token" header (for query parameter passthrough).
 */
@Component
public class WebSocketJwtInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(WebSocketJwtInterceptor.class);

    private final JWSVerifier verifier;

    public WebSocketJwtInterceptor(RSAPublicKey rsaPublicKey) {
        this.verifier = new RSASSAVerifier(rsaPublicKey);
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token != null) {
                UUID userId = validateAndExtractUserId(token);
                if (userId != null) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                    accessor.setUser(auth);
                    log.debug("WebSocket CONNECT authenticated for user: {}", userId);
                } else {
                    log.warn("WebSocket CONNECT with invalid JWT token");
                }
            } else {
                log.warn("WebSocket CONNECT without token");
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        // Try native STOMP header "Authorization"
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.get(0);
            if (header.startsWith("Bearer ")) {
                return header.substring(7);
            }
            return header;
        }

        // Try "token" header (for query parameter passthrough)
        List<String> tokenHeaders = accessor.getNativeHeader("token");
        if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
            return tokenHeaders.get(0);
        }

        return null;
    }

    private UUID validateAndExtractUserId(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            if (!signedJWT.verify(verifier)) {
                return null;
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Check expiration
            Date expiry = claims.getExpirationTime();
            if (expiry != null && expiry.toInstant().isBefore(Instant.now())) {
                return null;
            }

            return UUID.fromString(claims.getSubject());
        } catch (Exception e) {
            log.debug("JWT validation failed for WebSocket: {}", e.getMessage());
            return null;
        }
    }
}
