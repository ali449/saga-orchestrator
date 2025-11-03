package com.example.orchestrator.messaging;

import com.example.common.messaging.model.SagaStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;

@Component
public class SagaStatusPublisher {
    private static final Logger logger = LoggerFactory.getLogger(SagaStatusPublisher.class);
    private final RedisTemplate<String, SagaStatusMessage> redisTemplate;
    private final ChannelTopic topic;

    public SagaStatusPublisher(
            @Qualifier("statusRedisTemplate") RedisTemplate<String, SagaStatusMessage> redisTemplate,
            ChannelTopic topic) {
        this.redisTemplate = redisTemplate;
        this.topic = topic;
    }

    public void publish(SagaStatusMessage status) {
        redisTemplate.convertAndSend(topic.getTopic(), status);
        logger.info("Published to Redis: {}", status);
    }
}
