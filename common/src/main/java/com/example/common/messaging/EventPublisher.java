package com.example.common.messaging;

import com.example.common.messaging.model.BaseEvent;

public interface EventPublisher {
    void publish(String topic, BaseEvent event);
}
