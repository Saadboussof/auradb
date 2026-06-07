package com.auradb.route.config;

import com.auradb.common.model.RouteInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, RouteInfo> routeRedisTemplate(ReactiveRedisConnectionFactory factory) {
        // We must register the JavaTimeModule so Jackson can serialize the 'Instant' timestamp inside our Record
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Value Serializer (Turns RouteInfo into human-readable JSON instead of binary)
        Jackson2JsonRedisSerializer<RouteInfo> valueSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, RouteInfo.class);
        
        // Key Serializer (Turns String "netflix" into Redis String)
        StringRedisSerializer keySerializer = new StringRedisSerializer();

        RedisSerializationContext<String, RouteInfo> context = RedisSerializationContext
                .<String, RouteInfo>newSerializationContext(keySerializer)
                .value(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
