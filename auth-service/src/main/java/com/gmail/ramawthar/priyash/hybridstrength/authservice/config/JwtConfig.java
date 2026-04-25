package com.gmail.ramawthar.priyash.hybridstrength.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;

/**
 * Configuration for JWT token generation and validation.
 * Loads RSA key pair and expiry settings from application properties.
 * <p>
 * In production, replace the generated key pair with keys loaded from
 * a secrets manager or PEM files via {@code jwt.public-key-location}
 * and {@code jwt.private-key-location} properties.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private Duration accessTokenExpiry = Duration.ofMinutes(15);
    private Duration refreshTokenExpiry = Duration.ofDays(7);

    // Lazily initialised key pair — generated once on startup
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    public JwtConfig() {
        generateKeyPair();
    }

    private void generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            this.publicKey = (RSAPublicKey) keyPair.getPublic();
            this.privateKey = (RSAPrivateKey) keyPair.getPrivate();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA key pair generation failed", e);
        }
    }

    @Bean
    public RSAPublicKey rsaPublicKey() {
        return publicKey;
    }

    @Bean
    public RSAPrivateKey rsaPrivateKey() {
        return privateKey;
    }

    public Duration getAccessTokenExpiry() {
        return accessTokenExpiry;
    }

    public void setAccessTokenExpiry(Duration accessTokenExpiry) {
        this.accessTokenExpiry = accessTokenExpiry;
    }

    public Duration getRefreshTokenExpiry() {
        return refreshTokenExpiry;
    }

    public void setRefreshTokenExpiry(Duration refreshTokenExpiry) {
        this.refreshTokenExpiry = refreshTokenExpiry;
    }
}
