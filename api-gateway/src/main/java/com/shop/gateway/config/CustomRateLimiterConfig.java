package com.shop.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;

@Configuration
public class CustomRateLimiterConfig {

    @Bean
    public RedisRateLimiter customRedisRateLimiter() {
        return new RedisRateLimiter(10, 20); // Default replenish rate and burst capacity
    }
}