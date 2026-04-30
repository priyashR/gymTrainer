package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.common.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

/**
 * Extracts and validates the JWT Bearer token from the Authorization header.
 * On success, sets the SecurityContext with the authenticated user's ID.
 * On failure (missing, malformed, expired), does nothing — Spring Security
 * will reject the request with 401 via the configured AuthenticationEntryPoint.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JWSVerifier verifier;

    public JwtAuthenticationFilter(RSAPublicKey publicKey) {
        this.verifier = new RSASSAVerifier(publicKey);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null) {
            UUID userId = validateAndExtractUserId(token);
            if (userId != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user: {}", userId);
            }
        }

        filterChain.doFilter(request, response);
    }

    private UUID validateAndExtractUserId(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(verifier)) {
                return null;
            }
            Date expiration = jwt.getJWTClaimsSet().getExpirationTime();
            if (expiration == null || expiration.toInstant().isBefore(Instant.now())) {
                return null;
            }
            return UUID.fromString(jwt.getJWTClaimsSet().getSubject());
        } catch (ParseException | JOSEException | IllegalArgumentException e) {
            log.debug("Token validation failed: {}", e.getMessage());
            return null;
        }
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
