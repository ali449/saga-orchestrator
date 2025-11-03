package com.example.orchestrator.messaging;

import com.example.common.messaging.CommandPublisher;
import com.example.common.messaging.model.BaseCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SagaCommandPublisher implements CommandPublisher {
    private final KafkaTemplate<String, BaseCommand> kafkaTemplate;

    @Override
    public void publish(String topic, BaseCommand command) {
        kafkaTemplate.send(topic, command);
    }
}
