package com.shop.queue.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String QUEUE_EXCHANGE = "queue.exchange";
    public static final String QUEUE_ADDED_QUEUE = "queue.added.queue";
    public static final String QUEUE_ADDED_ROUTING_KEY = "queue.added";
    public static final String QUEUE_UPDATED_QUEUE = "queue.updated.queue";
    public static final String QUEUE_UPDATED_ROUTING_KEY = "queue.updated";
    public static final String QUEUE_REQUEST_COMPLETED_QUEUE = "queue.request.completed.queue";
    public static final String QUEUE_REQUEST_COMPLETED_ROUTING_KEY = "queue.request.completed";
    public static final String PRODUCT_STOCK_UPDATED_QUEUE = "product.stock.updated.queue";
    public static final String PRODUCT_STOCK_UPDATED_ROUTING_KEY = "product.stock.updated";

    @Bean
    public TopicExchange queueExchange() {
        return new TopicExchange(QUEUE_EXCHANGE);
    }

    @Bean
    public Queue queueAddedQueue() {
        return new Queue(QUEUE_ADDED_QUEUE);
    }

    @Bean
    public Queue queueUpdatedQueue() {
        return new Queue(QUEUE_UPDATED_QUEUE);
    }

    @Bean
    public Queue queueRequestCompletedQueue() {
        return new Queue(QUEUE_REQUEST_COMPLETED_QUEUE);
    }

    @Bean
    public Queue productStockUpdatedQueue() {
        return new Queue(PRODUCT_STOCK_UPDATED_QUEUE);
    }

    @Bean
    public Binding queueAddedBinding(Queue queueAddedQueue, TopicExchange queueExchange) {
        return BindingBuilder.bind(queueAddedQueue).to(queueExchange).with(QUEUE_ADDED_ROUTING_KEY);
    }

    @Bean
    public Binding queueUpdatedBinding(Queue queueUpdatedQueue, TopicExchange queueExchange) {
        return BindingBuilder.bind(queueUpdatedQueue).to(queueExchange).with(QUEUE_UPDATED_ROUTING_KEY);
    }

    @Bean
    public Binding queueRequestCompletedBinding(Queue queueRequestCompletedQueue, TopicExchange queueExchange) {
        return BindingBuilder.bind(queueRequestCompletedQueue).to(queueExchange).with(QUEUE_REQUEST_COMPLETED_ROUTING_KEY);
    }

    @Bean
    public Binding productStockUpdatedBinding(Queue productStockUpdatedQueue, TopicExchange queueExchange) {
        return BindingBuilder.bind(productStockUpdatedQueue).to(queueExchange).with(PRODUCT_STOCK_UPDATED_ROUTING_KEY);
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
