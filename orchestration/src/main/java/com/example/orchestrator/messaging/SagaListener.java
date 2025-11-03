package com.example.orchestrator.messaging;

import com.example.common.messaging.model.BaseEvent;
import com.example.common.statics.KafkaNames;
import com.example.common.utils.Try;
import com.example.orchestrator.dto.OutBox;
import com.example.orchestrator.dto.SagaExecuteResult;
import com.example.orchestrator.dto.SagaTimeoutEvent;
import com.example.orchestrator.service.SagaInstanceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SagaListener {
    private static final Logger logger = LoggerFactory.getLogger(SagaListener.class);
    private final SagaInstanceService sagaInstanceService;
    private final RetryTemplate retryTemplate;
    private final SagaCommandPublisher commandPublisher;
    private final SagaStatusPublisher statusPublisher;
    private final SagaEventPublisher eventPublisher;

    @KafkaListener(topics = {KafkaNames.ORDER_EVENTS, KafkaNames.INVENTORY_EVENTS, KafkaNames.PAYMENT_EVENTS},
            groupId = KafkaNames.ORCHESTRATOR_GROUP)
    public void onEvent(BaseEvent event) {
        //TODO: Add idempotency pattern with Redis + DB
        logger.info("Orchestrator service received {}", event);
        if (event.getEventType().isCompensatoryEvent()) {
            sagaInstanceService.onCompensatoryEvent(event);
        } else if (event.isFailureEvent()) {
            sagaInstanceService.onTimeoutOrFailure(event);
        } else {
            sagaInstanceService.run(event);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterSagaCommit(SagaExecuteResult result) {
        if (result.success()) {
            OutBox outBox = result.outBox();
            if (outBox != null) {
                retryTemplate.execute(context -> {
                    logger.warn("Retrying producer for {}", context.getRetryCount());
                    commandPublisher.publish(outBox.topic(), outBox.command());
                    return null;
                }, context -> {
                    logger.warn("Retry producer reached max limit {}", context.getRetryCount());
                    sagaInstanceService.onProducerRetryExhausted(outBox.command(), outBox.triggerEvent());
                    return null;
                });
            }
        } else {
            sagaInstanceService.moveToDeadLetter(result);
        }

        Try.run(() -> statusPublisher.publish(result.status()))
                .onFailure(ex -> logger.error("Publishing status failed: {}", ex.getMessage()));

        Try.run(() -> eventPublisher.publishSagaCompletedEvent(result))
                .onFailure(ex -> logger.error("Publishing completed event failed: {}", ex.getMessage()));
    }

    @EventListener
    public void onTimeout(SagaTimeoutEvent timeoutEvent) {
        sagaInstanceService.onTimeoutOrFailure(timeoutEvent.toEvent());
    }
}
