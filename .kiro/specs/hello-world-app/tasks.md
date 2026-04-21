# Implementation Plan: Hello World App

## Overview

Implement a minimal Spring Boot web application that serves "Hello Priyash" at the root URL using a `GreetingService` and `GreetingController`, with unit and property-based tests.

## Tasks

- [x] 1. Set up Spring Boot project structure
  - Create a Maven or Gradle project with `spring-boot-starter-web` and `jqwik` dependencies
  - Create the main application class `HelloWorldApplication` with `@SpringBootApplication` and `main` method
  - _Requirements: 2.1, 2.2_

- [x] 2. Implement GreetingService
  - [x] 2.1 Create `GreetingService` class annotated with `@Service`
    - Define `MESSAGE = "Hello Priyash"` constant
    - Implement `renderGreeting()` returning an HTML string containing `MESSAGE`
    - _Requirements: 1.1, 1.2_

  - [x] 2.2 Write property test for GreetingService (Property 3)
    - **Property 3: Greeting renderer always embeds the message**
    - **Validates: Requirements 1.1**
    - Use jqwik `@Property` with `@ForAll @NotBlank String message` to verify rendered HTML contains the message

- [x] 3. Implement GreetingController
  - [x] 3.1 Create `GreetingController` class annotated with `@RestController`
    - Inject `GreetingService` via constructor
    - Implement `@GetMapping("/")` method returning `ResponseEntity<String>` with `text/html` content type and body from `greetingService.renderGreeting()`
    - _Requirements: 1.1, 1.2_

  - [x] 3.2 Write unit tests for GreetingController
    - Use `@WebMvcTest(GreetingController.class)` with `MockMvc`
    - Test `GET /` returns HTTP 200 with `Content-Type: text/html` and body containing `"Hello Priyash"`
    - _Requirements: 1.1, 1.2_

  - [x] 3.3 Write property tests for GreetingController (Properties 1 & 2)
    - **Property 1: Greeting message is always present in the response body**
    - **Property 2: Response always has HTTP 200 status**
    - **Validates: Requirements 1.1, 1.2, 2.1**
    - Use jqwik `@Property(tries = 100)` to verify `GET /` always returns 200 and body contains `"Hello Priyash"`

- [x] 4. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Wire application and verify context loads
  - [x] 5.1 Confirm `HelloWorldApplication` wires `GreetingService` and `GreetingController` via Spring auto-configuration
    - _Requirements: 2.1, 2.2_

  - [x] 5.2 Write Spring context integration test
    - Use `@SpringBootTest` to verify the application context loads without errors
    - _Requirements: 2.1, 2.2_

- [x] 6. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP
- Each task references specific requirements for traceability
- Property tests use jqwik; unit tests use JUnit 5 with Spring's `@WebMvcTest`
