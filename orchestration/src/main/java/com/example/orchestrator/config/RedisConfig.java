package com.example.orchestrator.config;

import com.example.common.messaging.model.SagaStatusMessage;
import com.example.common.statics.RedisChannelNames;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ChannelTopic orderStatusTopic() {
        return new ChannelTopic(RedisChannelNames.ORDER_STATUS);
    }


    @Bean(name = "statusRedisTemplate")
    public RedisTemplate<String, SagaStatusMessage> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, SagaStatusMessage> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON serializer for values
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(SagaStatusMessage.class));
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(SagaStatusMessage.class));

        template.afterPropertiesSet();
        return template;
    }
}
