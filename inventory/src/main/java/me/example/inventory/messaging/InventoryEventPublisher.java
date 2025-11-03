package me.example.inventory.messaging;

import com.example.common.messaging.EventPublisher;
import com.example.common.messaging.model.BaseEvent;
import com.example.common.statics.KafkaNames;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, BaseEvent> kafkaTemplate;

    @Override
    public void publish(String topic, BaseEvent event) {
        kafkaTemplate.send(topic, event);
    }

    public void publishEvent(BaseEvent event) {
        publish(KafkaNames.INVENTORY_EVENTS, event);
    }
}
