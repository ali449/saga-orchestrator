package me.example.payment.messaging;

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
public class PaymentCommandListener {
    private static final Logger logger = LoggerFactory.getLogger(PaymentCommandListener.class);
    private final PaymentEventPublisher publisher;

    @KafkaListener(topics = KafkaNames.PAYMENT_COMMANDS, groupId = KafkaNames.PAYMENT_GROUP)
    public void onCommand(BaseCommand command) {
        logger.info("Payment service received {}", command);
        if (command.getCommandType() == CommandType.PROCESS_PAYMENT) {
            publisher.publishEvent(EventCommandMapper.toEvent(command, EventType.PAYMENT_SUCCEEDED));
        } else if (command.getCommandType() == CommandType.REFUND_PAYMENT) {
            logger.warn("Refunding payment {}", command.getAggregateId());
            publisher.publishEvent(EventCommandMapper.toEvent(command, EventType.PAYMENT_REFUNDED));
        }
    }
}
