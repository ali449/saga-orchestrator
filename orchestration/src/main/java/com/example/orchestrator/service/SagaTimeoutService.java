package com.example.orchestrator.service;

import com.example.common.messaging.model.BaseEvent;
import com.example.common.utils.Try;
import com.example.orchestrator.dao.SagaStarterEventDao;
import com.example.orchestrator.dto.SagaTimeoutEvent;
import com.example.orchestrator.entity.SagaStarterEventEntity;
import com.example.orchestrator.scheduling.SagaTimeoutScheduler;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SagaTimeoutService {
    private static final Logger logger = LoggerFactory.getLogger(SagaTimeoutService.class);
    private final SagaStarterEventDao dao;
    private final SagaTimeoutScheduler sagaTimeoutScheduler;
    private final ApplicationEventPublisher eventPublisher;

    @PostConstruct
    public void registerCallback() {
        sagaTimeoutScheduler.registerTimeoutCallback(this::handleSagaTimeout);
    }

    @Transactional
    public void scheduleTimeout(BaseEvent event, String sagaId, String sagaInstanceId, LocalDateTime expireAt) {
        logger.info("Scheduling new timeout for instance {}", sagaInstanceId);
        create(event, sagaId, sagaInstanceId);
        sagaTimeoutScheduler.scheduleTimeout(event.getEventId(), expireAt);
    }

    @Transactional
    public void cancelSchedule(String id) {
        logger.info("Cancelling timeout for event {}", id);
        dao.deleteById(id);
        sagaTimeoutScheduler.cancelTimeout(id);
    }

    private void handleSagaTimeout(String eventId) {
        logger.info("Timeout triggered for event: {}", eventId);
        Try.run(() -> dao.findById(eventId).ifPresentOrElse(entity -> {
                    eventPublisher.publishEvent(new SagaTimeoutEvent(entity));
                },
                () -> logger.error("Event {} not found", eventId)))
                .onFailure(ex -> {
                    logger.error("Saga timeout error", ex);
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
                });
    }

    public void create(BaseEvent event, String sagaId, String sagaInstanceId) {
        SagaStarterEventEntity entity = new SagaStarterEventEntity();
        entity.setId(event.getEventId());
        entity.setSagaId(sagaId);
        entity.setSagaInstanceId(sagaInstanceId);
        entity.setAggregateId(event.getAggregateId());
        entity.setOccurredAt(event.getOccurredAt());
        entity.setEventType(event.getEventType());
        entity.setPayload(event.getPayload());

        dao.save(entity);
    }
}
