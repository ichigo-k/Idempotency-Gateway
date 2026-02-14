package com.finsafe.idempotency_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class RedisConfig {

    @Bean(name = "redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory factory,
            ObjectMapper mapper
    ) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        RedisSerializer<String> keySer = new StringRedisSerializer();
        RedisSerializer<Object> valSer = new GenericJacksonJsonRedisSerializer(mapper);

        template.setKeySerializer(keySer);
        template.setValueSerializer(valSer);
        template.setHashKeySerializer(keySer);
        template.setHashValueSerializer(valSer);

        template.afterPropertiesSet();
        return template;
    }
}
