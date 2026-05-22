package com.gmail.ramawthar.priyash.hybridstrength.workoutsession.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ infrastructure configuration for the Workout Session Service.
 *
 * <p>Declares the {@code session.events} topic exchange and binds the
 * {@code progress-tracker.session-completed} queue with routing key
 * {@code session.completed}. Uses Jackson for JSON message serialization.
 */
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "session.events";
    public static final String QUEUE_NAME = "progress-tracker.session-completed";
    public static final String ROUTING_KEY = "session.completed";

    @Bean
    TopicExchange sessionEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    Queue sessionCompletedQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    Binding sessionCompletedBinding(Queue sessionCompletedQueue, TopicExchange sessionEventsExchange) {
        return BindingBuilder.bind(sessionCompletedQueue)
                .to(sessionEventsExchange)
                .with(ROUTING_KEY);
    }

    @Bean
    MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
