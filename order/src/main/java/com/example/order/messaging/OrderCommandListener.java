package com.example.order.messaging;

import com.example.common.messaging.enums.CommandType;
import com.example.common.messaging.enums.EventType;
import com.example.common.messaging.model.BaseCommand;
import com.example.common.messaging.model.mapper.EventCommandMapper;
import com.example.common.statics.KafkaNames;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderCommandListener {
    private static final Logger logger = LoggerFactory.getLogger(OrderCommandListener.class);
    private final OrderEventPublisher publisher;

    @KafkaListener(topics = KafkaNames.ORDER_COMMANDS, groupId = KafkaNames.ORDER_GROUP)
    public void onCommand(BaseCommand command) {
        logger.info("Order service received {} ", command);
        if (command.getCommandType() == CommandType.COMPLETE_ORDER) {
            publisher.publishEvent(EventCommandMapper.toEvent(command, EventType.ORDER_COMPLETED));
        } else if (command.getCommandType() == CommandType.CANCEL_ORDER) {
            logger.warn("Canceling order {}", command.getAggregateId());
            publisher.publishEvent(EventCommandMapper.toEvent(command, EventType.ORDER_CANCELLED));

        }
    }
}
