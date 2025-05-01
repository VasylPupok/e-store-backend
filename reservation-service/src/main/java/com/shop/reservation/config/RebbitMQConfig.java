package com.shop.reservation.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public
class RabbitMQConfig {
    public static final String RESERVATION_EXCHANGE = "reservation.exchange";
    public static final String RESERVATION_CREATED_QUEUE = "reservation.created.queue";
    public static final String RESERVATION_CREATED_ROUTING_KEY = "reservation.created";
    public static final String RESERVATION_EXPIRED_QUEUE = "reservation.expired.queue";
    public static final String RESERVATION_EXPIRED_ROUTING_KEY = "reservation.expired";
    public static final String RESERVATION_COMPLETED_QUEUE = "reservation.completed.queue";
    public static final String RESERVATION_COMPLETED_ROUTING_KEY = "reservation.completed";
    public static final String RESERVATION_CANCELLED_QUEUE = "reservation.cancelled.queue";
    public static final String RESERVATION_CANCELLED_ROUTING_KEY = "reservation.cancelled";

    @Bean
    public TopicExchange reservationExchange() {
        return new TopicExchange(RESERVATION_EXCHANGE);
    }

    @Bean
    public Queue reservationCreatedQueue() {
        return new Queue(RESERVATION_CREATED_QUEUE);
    }

    @Bean
    public Queue reservationExpiredQueue() {
        return new Queue(RESERVATION_EXPIRED_QUEUE);
    }

    @Bean
    public Queue reservationCompletedQueue() {
        return new Queue(RESERVATION_COMPLETED_QUEUE);
    }

    @Bean
    public Queue reservationCancelledQueue() {
        return new Queue(RESERVATION_CANCELLED_QUEUE);
    }

    @Bean
    public Binding reservationCreatedBinding(Queue reservationCreatedQueue, TopicExchange reservationExchange) {
        return BindingBuilder.bind(reservationCreatedQueue)
                .to(reservationExchange)
                .with(RESERVATION_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding reservationExpiredBinding(Queue reservationExpiredQueue, TopicExchange reservationExchange) {
        return BindingBuilder.bind(reservationExpiredQueue)
                .to(reservationExchange)
                .with(RESERVATION_EXPIRED_ROUTING_KEY);
    }

    @Bean
    public Binding reservationCompletedBinding(Queue reservationCompletedQueue, TopicExchange reservationExchange) {
        return BindingBuilder.bind(reservationCompletedQueue)
                .to(reservationExchange)
                .with(RESERVATION_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Binding reservationCancelledBinding(Queue reservationCancelledQueue, TopicExchange reservationExchange) {
        return BindingBuilder.bind(reservationCancelledQueue)
                .to(reservationExchange)
                .with(RESERVATION_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}