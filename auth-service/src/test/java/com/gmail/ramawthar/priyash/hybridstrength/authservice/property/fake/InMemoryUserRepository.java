package com.gmail.ramawthar.priyash.hybridstrength.authservice.property.fake;

import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.domain.User;
import com.gmail.ramawthar.priyash.hybridstrength.authservice.registration.ports.outbound.UserRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory fake for property tests — no Spring context needed.
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> store = new HashMap<>();

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(store.get(email));
    }

    @Override
    public Optional<User> findById(UUID id) {
        return store.values().stream()
                .filter(user -> user.getId().equals(id))
                .findFirst();
    }

    @Override
    public User save(User user) {
        store.put(user.getEmail(), user);
        return user;
    }

    @Override
    public boolean existsByEmail(String email) {
        return store.containsKey(email);
    }

    public void clear() {
        store.clear();
    }
}
