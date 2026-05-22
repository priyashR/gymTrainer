package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.event;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.domain.SectionProgress;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain event published when a user completes a workout session.
 * Consumed by the Progress Tracker Service via RabbitMQ.
 */
public record SessionCompletedEvent(
        UUID eventId,
        Instant occurredAt,
        String userId,
        UUID sessionId,
        UUID programId,
        int weekNumber,
        int dayNumber,
        boolean standalone,
        List<SectionProgress> sectionProgresses,
        Instant startedAt,
        Instant completedAt
) {
}
