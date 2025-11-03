package com.example.common.messaging.model;

import com.example.common.messaging.enums.EventType;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class BaseEvent {
    private final String eventId = UUID.randomUUID().toString();
    private String sagaId;//SagaEntity#id
    private String sagaInstanceId;//SagaInstanceEntity#id
    private String aggregateId;
    private final LocalDateTime occurredAt = LocalDateTime.now();
    private EventType eventType;
    private JsonNode payload;

    //Added to prevent duplication in sending failure command when a resource requested
    private boolean failureEvent;

    public BaseEvent(String aggregateId, EventType eventType, JsonNode payload) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
    }

    public BaseEvent(String aggregateId, EventType eventType, JsonNode payload, String sagaId,
                     String sagaInstanceId) {
        this(aggregateId, eventType, payload);
        this.sagaId = sagaId;
        this.sagaInstanceId = sagaInstanceId;
    }

    public void markThisIsFailureEvent() {
        this.failureEvent = true;
    }

    @Override
    public String toString() {
        return "BaseEvent{" +
                "eventId='" + eventId + '\'' +
                ", sagaId='" + sagaId + '\'' +
                ", sagaInstanceId='" + sagaInstanceId + '\'' +
                ", domainId='" + aggregateId + '\'' +
                ", occurredAt=" + occurredAt +
                ", eventType=" + eventType +
                ", payload='" + payload + '\'' +
                '}';
    }
}
