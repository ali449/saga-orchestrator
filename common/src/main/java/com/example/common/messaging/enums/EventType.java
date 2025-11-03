package com.example.common.messaging.enums;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum EventType {
    ORDER_CREATED,
    STOCK_RESERVED,
    STOCK_RELEASED,
    PAYMENT_SUCCEEDED,
    PAYMENT_REFUNDED,
    ORDER_COMPLETED,
    ORDER_CANCELLED;

    private static final Map<EventType, EventType> compensatoryRelatedEvent;

    static {
        compensatoryRelatedEvent = new HashMap<>();
        compensatoryRelatedEvent.put(STOCK_RELEASED, STOCK_RESERVED);
        compensatoryRelatedEvent.put(PAYMENT_REFUNDED, PAYMENT_SUCCEEDED);
        compensatoryRelatedEvent.put(ORDER_CANCELLED, ORDER_COMPLETED);
    }

    public EventType getCompensatoryRelatedEvent() {
        return compensatoryRelatedEvent.get(this);
    }

    public boolean isCompensatoryEvent() {
        return compensatoryRelatedEvent.containsKey(this);
    }

    public boolean isCompletedEvent() {
        return this == ORDER_COMPLETED;
    }
}

