package com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.adapters.inbound.dto;

import java.time.Instant;
import java.util.UUID;

public record RegisterResponse(UUID id, String email, Instant createdAt) {}
