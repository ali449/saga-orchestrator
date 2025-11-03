package com.example.orchestrator.dto;

import com.example.common.messaging.enums.EventType;
import com.example.common.messaging.model.BaseEvent;
import com.example.orchestrator.entity.SagaStarterEventEntity;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record SagaTimeoutEvent(String id, String sagaId, String sagaInstanceId, String aggregateId,
                               LocalDateTime occurredAt, EventType eventType, JsonNode payload) {
    public SagaTimeoutEvent(SagaStarterEventEntity entity) {
        this(entity.getId(), entity.getSagaId(), entity.getSagaInstanceId(), entity.getAggregateId(),
                entity.getOccurredAt(), entity.getEventType(), entity.getPayload());
    }

    public BaseEvent toEvent() {
        return new BaseEvent(aggregateId, eventType, payload, sagaId, sagaInstanceId);
    }
}
