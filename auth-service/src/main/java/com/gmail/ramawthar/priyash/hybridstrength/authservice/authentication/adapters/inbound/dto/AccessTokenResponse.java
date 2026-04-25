package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.adapters.inbound.dto;

public record AccessTokenResponse(String accessToken, String tokenType, long expiresIn) {}
