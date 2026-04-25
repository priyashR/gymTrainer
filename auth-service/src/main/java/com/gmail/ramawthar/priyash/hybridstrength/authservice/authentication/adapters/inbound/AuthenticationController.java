package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.adapters.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.adapters.inbound.dto.AccessTokenResponse;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.adapters.inbound.dto.LoginRequest;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.domain.TokenPair;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.inbound.LoginUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.ports.inbound.RefreshTokenUseCase;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.config.JwtConfig;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);
    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String REFRESH_TOKEN_PATH = "/api/v1/auth/refresh";

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final JwtConfig jwtConfig;

    public AuthenticationController(LoginUseCase loginUseCase,
                                    RefreshTokenUseCase refreshTokenUseCase,
                                    JwtConfig jwtConfig) {
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.jwtConfig = jwtConfig;
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request received");

        TokenPair tokenPair = loginUseCase.login(request.email(), request.password());

        AccessTokenResponse response = new AccessTokenResponse(
                tokenPair.getAccessToken(),
                "Bearer",
                jwtConfig.getAccessTokenExpiry().toSeconds()
        );

        ResponseCookie cookie = buildRefreshTokenCookie(tokenPair.getRefreshToken());

        log.info("Login successful");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE) String refreshToken) {
        log.info("Token refresh request received");

        TokenPair tokenPair = refreshTokenUseCase.refresh(refreshToken);

        AccessTokenResponse response = new AccessTokenResponse(
                tokenPair.getAccessToken(),
                "Bearer",
                jwtConfig.getAccessTokenExpiry().toSeconds()
        );

        ResponseCookie cookie = buildRefreshTokenCookie(tokenPair.getRefreshToken());

        log.info("Token refresh successful");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    private ResponseCookie buildRefreshTokenCookie(String rawRefreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, rawRefreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(REFRESH_TOKEN_PATH)
                .maxAge(jwtConfig.getRefreshTokenExpiry().toSeconds())
                .build();
    }
}
