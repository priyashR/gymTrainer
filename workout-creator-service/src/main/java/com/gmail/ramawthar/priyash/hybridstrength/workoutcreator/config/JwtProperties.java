package com.gmail.ramawthar.priyash.hybridstrength.workoutcreator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds {@code jwt.*} properties from application.yml.
 * The public key is provided as a PEM-encoded string so it can be injected
 * via environment variable or config file without needing a key file on disk.
 *
 * <p>In dev/test the key pair is generated at startup (see {@link JwtConfig}).
 * In production, set {@code jwt.public-key-pem} to the RSA public key PEM.
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** PEM-encoded RSA public key (optional — if absent, a key pair is generated at startup). */
    private String publicKeyPem;

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }
}
