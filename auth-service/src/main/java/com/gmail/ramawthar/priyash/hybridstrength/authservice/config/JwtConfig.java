package com.gmail.ramawthar.priyash.hybridstrength.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;

/**
 * Configuration for JWT token generation and validation.
 * Loads RSA key pair and expiry settings from application properties.
 * <p>
 * When {@code jwt.public-key-pem} and {@code jwt.private-key-pem} are configured,
 * those PEM strings are used. Otherwise a random key pair is generated at startup
 * (useful for integration tests that override the beans).
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    private Duration accessTokenExpiry = Duration.ofMinutes(15);
    private Duration refreshTokenExpiry = Duration.ofDays(7);
    private String publicKeyPem;
    private String privateKeyPem;

    // Lazily initialised key pair
    private RSAPublicKey publicKey;
    private RSAPrivateKey privateKey;

    @Bean
    public RSAPublicKey rsaPublicKey() {
        if (publicKey == null) {
            initKeys();
        }
        return publicKey;
    }

    @Bean
    public RSAPrivateKey rsaPrivateKey() {
        if (privateKey == null) {
            initKeys();
        }
        return privateKey;
    }

    private void initKeys() {
        if (publicKeyPem != null && !publicKeyPem.isBlank()
                && privateKeyPem != null && !privateKeyPem.isBlank()) {
            this.publicKey = parsePublicKeyFromPem(publicKeyPem);
            this.privateKey = parsePrivateKeyFromPem(privateKeyPem);
        } else {
            generateKeyPair();
        }
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

    private RSAPublicKey parsePublicKeyFromPem(String pem) {
        try {
            String stripped = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(stripped);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) kf.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA public key from PEM", e);
        }
    }

    private RSAPrivateKey parsePrivateKeyFromPem(String pem) {
        try {
            String stripped = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(stripped);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse RSA private key from PEM", e);
        }
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

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }

    public String getPrivateKeyPem() {
        return privateKeyPem;
    }

    public void setPrivateKeyPem(String privateKeyPem) {
        this.privateKeyPem = privateKeyPem;
    }
}
