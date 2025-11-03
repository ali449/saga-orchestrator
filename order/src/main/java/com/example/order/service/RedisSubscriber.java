package com.example.order.service;

import com.example.common.messaging.model.SagaStatusMessage;
import com.example.common.utils.Try;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RedisSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(RedisSubscriber.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public void onMessage(String message, String pattern) {
        logger.info("Received from Redis: {}",  message);
        if (!StringUtils.hasText(message)) {
            logger.error("Empty message received");
            return;
        }
        try {
            SagaStatusMessage status = objectMapper.readValue(message, SagaStatusMessage.class);
            messagingTemplate.convertAndSend("/topic/orders/" + status.aggregateId(), message);
        } catch (Exception e) {
            logger.error("onMessage failed", e);
        }
    }
}

