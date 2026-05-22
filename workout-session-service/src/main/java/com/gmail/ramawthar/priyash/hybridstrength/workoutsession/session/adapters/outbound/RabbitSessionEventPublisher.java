package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.adapters.outbound;

import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.common.event.SessionCompletedEvent;
import com.gmail.ramawthar.priyash.hybridstrength.workoutsession.session.ports.outbound.SessionEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ-backed implementation of the {@link SessionEventPublisher} outbound port.
 * Publishes {@link SessionCompletedEvent} to the {@code session.events} topic exchange
 * with routing key {@code session.completed}.
 * <p>
 * Implements exponential backoff retry (1s initial, multiplier 2, max 5 attempts).
 * On final failure, logs at ERROR with the full event payload for manual replay.
 */
@Component
public class RabbitSessionEventPublisher implements SessionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitSessionEventPublisher.class);

    private static final String EXCHANGE = "session.events";
    private static final String ROUTING_KEY = "session.completed";
    private static final int MAX_ATTEMPTS = 5;
    private static final long INITIAL_DELAY_MS = 1000;
    private static final int MULTIPLIER = 2;

    private final RabbitTemplate rabbitTemplate;

    public RabbitSessionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishSessionCompleted(SessionCompletedEvent event) {
        long delay = INITIAL_DELAY_MS;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, event);
                log.info("Published SessionCompleted event: eventId={}, sessionId={}",
                        event.eventId(), event.sessionId());
                return;
            } catch (AmqpException e) {
                log.warn("Failed to publish SessionCompleted event (attempt {}/{}): {}",
                        attempt, MAX_ATTEMPTS, e.getMessage());

                if (attempt == MAX_ATTEMPTS) {
                    log.error("Exhausted all {} retry attempts for SessionCompleted event. " +
                                    "Event payload for manual replay: eventId={}, sessionId={}, userId={}, " +
                                    "programId={}, weekNumber={}, dayNumber={}, standalone={}, startedAt={}, completedAt={}",
                            MAX_ATTEMPTS,
                            event.eventId(), event.sessionId(), event.userId(),
                            event.programId(), event.weekNumber(), event.dayNumber(),
                            event.standalone(), event.startedAt(), event.completedAt());
                    return;
                }

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Retry interrupted for SessionCompleted event: eventId={}", event.eventId());
                    return;
                }
                delay *= MULTIPLIER;
            }
        }
    }
}
