package com.example.common.messaging;

import com.example.common.messaging.model.BaseCommand;

public interface CommandPublisher {
    void publish(String topic, BaseCommand command);
}

