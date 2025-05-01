package com.shop.product.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String PRODUCT_EXCHANGE = "product.exchange";
    public static final String STOCK_UPDATE_QUEUE = "stock.update.queue";
    public static final String STOCK_UPDATE_ROUTING_KEY = "stock.update";
    public static final String PRODUCT_RESERVED_QUEUE = "product.reserved.queue";
    public static final String PRODUCT_RESERVED_ROUTING_KEY = "product.reserved";
    public static final String RESERVATION_EXPIRED_QUEUE = "reservation.expired.queue";
    public static final String RESERVATION_EXPIRED_ROUTING_KEY = "reservation.expired";

    @Bean
    public TopicExchange productExchange() {
        return new TopicExchange(PRODUCT_EXCHANGE);
    }

    @Bean
    public Queue stockUpdateQueue() {
        return new Queue(STOCK_UPDATE_QUEUE);
    }

    @Bean
    public Queue productReservedQueue() {
        return new Queue(PRODUCT_RESERVED_QUEUE);
    }

    @Bean
    public Queue reservationExpiredQueue() {
        return new Queue(RESERVATION_EXPIRED_QUEUE);
    }

    @Bean
    public Binding stockUpdateBinding(Queue stockUpdateQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(stockUpdateQueue).to(productExchange).with(STOCK_UPDATE_ROUTING_KEY);
    }

    @Bean
    public Binding productReservedBinding(Queue productReservedQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(productReservedQueue).to(productExchange).with(PRODUCT_RESERVED_ROUTING_KEY);
    }

    @Bean
    public Binding reservationExpiredBinding(Queue reservationExpiredQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(reservationExpiredQueue).to(productExchange).with(RESERVATION_EXPIRED_ROUTING_KEY);
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
