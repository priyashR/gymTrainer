package com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.adapters.inbound.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotNull @Email String email,
    @NotNull @Size(min = 8) String password
) {}
