package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.event.SessionCompletedEvent;

/**
 * Outbound port for publishing session domain events to the message broker.
 * The implementation uses RabbitMQ with exponential backoff retry.
 */
public interface SessionEventPublisher {

    /**
     * Publishes a SessionCompleted event to the message broker.
     * Retries with exponential backoff (max 5 attempts) before logging failure.
     *
     * @param event the session completed event to publish
     */
    void publishSessionCompleted(SessionCompletedEvent event);
}
