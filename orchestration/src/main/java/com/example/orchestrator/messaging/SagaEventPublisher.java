package com.example.orchestrator.messaging;

import com.example.common.enums.ExecutionState;
import com.example.common.messaging.EventPublisher;
import com.example.common.messaging.model.BaseEvent;
import com.example.common.statics.KafkaNames;
import com.example.orchestrator.dto.SagaExecuteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SagaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, BaseEvent> kafkaTemplate;

    @Override
    public void publish(String topic, BaseEvent event) {
        kafkaTemplate.send(topic, event);
    }

    public void publishSagaCompletedEvent(SagaExecuteResult result) {
        if (result.success() && result.status().status() == ExecutionState.COMPLETED) {
            publish(KafkaNames.SAGA_EVENTS, result.triggeringEvent());
        }
    }
}
