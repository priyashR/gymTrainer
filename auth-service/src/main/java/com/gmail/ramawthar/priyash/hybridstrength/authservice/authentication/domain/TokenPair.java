package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain;

import java.util.Objects;

/**
 * Value object holding an access token and a refresh token.
 * Pure Java — no framework imports.
 */
public class TokenPair {

    private final String accessToken;
    private final String refreshToken;

    public TokenPair(String accessToken, String refreshToken) {
        this.accessToken = Objects.requireNonNull(accessToken, "accessToken must not be null");
        this.refreshToken = Objects.requireNonNull(refreshToken, "refreshToken must not be null");
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenPair tokenPair = (TokenPair) o;
        return Objects.equals(accessToken, tokenPair.accessToken)
                && Objects.equals(refreshToken, tokenPair.refreshToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, refreshToken);
    }

    @Override
    public String toString() {
        return "TokenPair{accessToken=[REDACTED], refreshToken=[REDACTED]}";
    }
}
