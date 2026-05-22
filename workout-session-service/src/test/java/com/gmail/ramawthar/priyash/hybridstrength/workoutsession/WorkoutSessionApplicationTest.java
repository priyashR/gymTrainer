package com.gmail.ramawthar.priyash.hybridstrength.workoutsession;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.progression.ports.outbound.EnrollmentRepository;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionEventPublisher;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionNotifier;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionRepository;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.WorkoutFetcher;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class WorkoutSessionApplicationTest {

    // Mock outbound ports that don't have adapter implementations yet (task 7)
    @MockitoBean
    private EnrollmentRepository enrollmentRepository;

    @MockitoBean
    private SessionRepository sessionRepository;

    @MockitoBean
    private WorkoutFetcher workoutFetcher;

    @MockitoBean
    private SessionEventPublisher sessionEventPublisher;

    @MockitoBean
    private SessionNotifier sessionNotifier;

    @Test
    void contextLoads() {
    }
}
