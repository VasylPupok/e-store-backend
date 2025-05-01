package com.shop.cart.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String CART_EXCHANGE = "cart.exchange";
    public static final String CART_UPDATED_QUEUE = "cart.updated.queue";
    public static final String CART_UPDATED_ROUTING_KEY = "cart.updated";
    public static final String RESERVATION_EXPIRED_QUEUE = "reservation.expired.queue";
    public static final String RESERVATION_EXPIRED_ROUTING_KEY = "reservation.expired";
    public static final String QUEUE_REQUEST_COMPLETED_QUEUE = "queue.request.completed.queue";
    public static final String QUEUE_REQUEST_COMPLETED_ROUTING_KEY = "queue.request.completed";

    @Bean
    public TopicExchange cartExchange() {
        return new TopicExchange(CART_EXCHANGE);
    }

    @Bean
    public Queue cartUpdatedQueue() {
        return new Queue(CART_UPDATED_QUEUE);
    }

    @Bean
    public Queue reservationExpiredQueue() {
        return new Queue(RESERVATION_EXPIRED_QUEUE);
    }

    @Bean
    public Queue queueRequestCompletedQueue() {
        return new Queue(QUEUE_REQUEST_COMPLETED_QUEUE);
    }

    @Bean
    public Binding cartUpdatedBinding(Queue cartUpdatedQueue, TopicExchange cartExchange) {
        return BindingBuilder.bind(cartUpdatedQueue).to(cartExchange).with(CART_UPDATED_ROUTING_KEY);
    }

    @Bean
    public Binding reservationExpiredBinding(Queue reservationExpiredQueue, TopicExchange cartExchange) {
        return BindingBuilder.bind(reservationExpiredQueue).to(cartExchange).with(RESERVATION_EXPIRED_ROUTING_KEY);
    }

    @Bean
    public Binding queueRequestCompletedBinding(Queue queueRequestCompletedQueue, TopicExchange cartExchange) {
        return BindingBuilder.bind(queueRequestCompletedQueue).to(cartExchange).with(QUEUE_REQUEST_COMPLETED_ROUTING_KEY);
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