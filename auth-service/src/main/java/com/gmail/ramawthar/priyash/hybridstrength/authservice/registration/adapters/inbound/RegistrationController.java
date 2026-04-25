package com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.adapters.inbound;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.adapters.inbound.dto.RegisterRequest;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.adapters.inbound.dto.RegisterResponse;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.domain.User;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.ports.inbound.RegisterUserUseCase;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class RegistrationController {

    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);

    private final RegisterUserUseCase registerUserUseCase;

    public RegistrationController(RegisterUserUseCase registerUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration request received for email: {}", request.email());

        User user = registerUserUseCase.register(request.email(), request.password());

        RegisterResponse response = new RegisterResponse(user.getId(), user.getEmail(), user.getCreatedAt());

        log.info("User registered successfully with id: {}", user.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
