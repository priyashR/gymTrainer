# Testing Standards

## Strategy

Every service must have tests at three levels: unit, property-based, and integration. Frontend has unit and E2E coverage.

## Backend Testing Layers

### Unit Tests (JUnit 5 + Mockito)

- Test domain logic and use cases in isolation
- Mock all outbound ports (repositories, external clients)
- No Spring context — plain Java instantiation only
- Naming: `MethodName_StateUnderTest_ExpectedBehaviour`
- Target: all domain objects, use cases, and non-trivial service logic

### Property-Based Tests (jqwik)

- Used to verify correctness properties that must hold for all valid inputs
- Minimum 100 iterations per property (`@Property(tries = 100)`)
- Required for:
  - Workout/Program parse → format → parse round-trip (must produce equivalent domain object)
  - 1RM Epley formula correctness across valid weight/rep ranges
  - Pagination boundary conditions
  - Any function with a formal invariant defined in the requirements
- Properties should be defined in a dedicated `*PropertyTest` class per domain

### Integration Tests (@SpringBootTest against local dev instances)

- Use the running local dev instances of PostgreSQL and RabbitMQ (see `docker/`) — ~~Testcontainers~~ not used due to local environment constraints
- No mocks for infrastructure — tests hit real services
- Test full request/response cycles through the HTTP layer
- Test RabbitMQ message publishing and consumption end-to-end
- Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` with `TestRestTemplate` or `WebTestClient`
- One integration test suite per service covering the primary happy paths and key failure scenarios
- Flyway migrations must run successfully as part of the integration test startup
- Connection details configured in `application-test.yml` pointing at the local dev instances

## Frontend Testing

### Unit Tests (Vitest + React Testing Library)

- Test individual components in isolation
- Focus on user interactions and rendered output — not implementation details
- Mock API calls and external dependencies
- Required for all interactive client components (Theater Mode timer, lap counter, rest timer)

### E2E Tests (Playwright)

- Cover critical user journeys end-to-end against a running stack
- Required journeys:
  - User registration and login
  - Generate and save a workout
  - Start and complete a session in Theater Mode
  - View progress dashboard
- Run against a local Docker Compose environment

## Test Data

- Use builders or factory methods for test data construction — no raw constructors with long argument lists
- Never use production data in tests
- Integration tests must clean up after themselves (use `@Transactional` rollback or explicit teardown)

## Coverage Expectations

- Domain and use case layer: high coverage expected (aim for >80% line coverage)
- Adapter layer: covered by integration tests, not unit tests
- Coverage is a guide, not a target — a well-written property test is worth more than 10 trivial unit tests
