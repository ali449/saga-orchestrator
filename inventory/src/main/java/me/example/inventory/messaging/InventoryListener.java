package me.example.inventory.messaging;

import com.example.common.messaging.model.BaseCommand;
import com.example.common.messaging.model.BaseEvent;
import com.example.common.statics.KafkaNames;
import lombok.RequiredArgsConstructor;
import me.example.inventory.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InventoryListener {
    private static final Logger logger = LoggerFactory.getLogger(InventoryListener.class);
    private final StockService stockService;

    @KafkaListener(topics = KafkaNames.INVENTORY_COMMANDS, groupId = KafkaNames.INVENTORY_GROUP)
    public void onCommand(BaseCommand command) { //TODO: Add retry policy
        logger.info("Inventory service received command {} ", command);
        stockService.onCommand(command);
    }

    @KafkaListener(topics = KafkaNames.SAGA_EVENTS, groupId = KafkaNames.INVENTORY_SAGA_GROUP)
    public void onEvent(BaseEvent sagaEvent) {
        logger.info("Inventory service received event {} ", sagaEvent);
        stockService.onEvent(sagaEvent);
    }
}
