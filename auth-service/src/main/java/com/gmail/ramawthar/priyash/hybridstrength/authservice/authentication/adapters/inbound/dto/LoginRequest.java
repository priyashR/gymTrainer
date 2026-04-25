package com.gmail.ramawthar.priyash.hybridstrength.authservice.authentication.adapters.inbound.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
    @NotNull @Email String email,
    @NotNull String password
) {}
