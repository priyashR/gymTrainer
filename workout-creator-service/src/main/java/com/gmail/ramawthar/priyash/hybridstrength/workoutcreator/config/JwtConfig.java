package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;

/**
 * Provides the RSA public key used to verify JWT access tokens.
 * <p>
 * In production, replace the generated key pair with the public key
 * loaded from a secrets manager or PEM file shared with the auth-service.
 * The generated key pair is used for local dev and testing only.
 */
@Configuration
public class JwtConfig {

    private final RSAPublicKey publicKey;

    public JwtConfig() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA key pair generation failed", e);
        }
    }

    @Bean
    public RSAPublicKey rsaPublicKey() {
        return publicKey;
    }
}
