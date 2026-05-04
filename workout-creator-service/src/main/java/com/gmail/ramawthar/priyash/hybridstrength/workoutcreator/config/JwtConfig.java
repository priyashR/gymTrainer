package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.config;

import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;

/**
 * Provides the RSA public key used to verify incoming JWT access tokens.
 *
 * <p>The workout-creator-service is a resource server — it only needs the public key
 * to verify tokens issued by the auth-service. It never issues tokens itself.
 *
 * <p>In production, configure {@code jwt.public-key-pem} with the auth-service's
 * RSA public key. In dev/test, a fresh key pair is generated at startup (tokens
 * signed with the matching private key will verify correctly within the same JVM,
 * which is exactly what the integration tests do).
 */
@Configuration
public class JwtConfig {

    private final JwtProperties jwtProperties;

    public JwtConfig(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Bean
    public RSAPublicKey rsaPublicKey() {
        String pem = jwtProperties.getPublicKeyPem();
        if (pem != null && !pem.isBlank()) {
            return parsePublicKeyFromPem(pem);
        }
        // No PEM configured — generate a fresh key pair.
        // Useful in dev and integration tests where the test generates its own key pair
        // and registers the public key via a @TestConfiguration override.
        return (RSAPublicKey) generateKeyPair().getPublic();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private KeyPair generateKeyPair() {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            return gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA key pair generation failed", e);
        }
    }

    private RSAPublicKey parsePublicKeyFromPem(String pem) {
        try {
            RSAKey rsaKey = RSAKey.parseFromPEMEncodedObjects(pem).toRSAKey();
            return rsaKey.toRSAPublicKey();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA public key from PEM", e);
        }
    }
}
