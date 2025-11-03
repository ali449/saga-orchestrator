package com.example.common.messaging.model.mapper;

import com.example.common.messaging.enums.EventType;
import com.example.common.messaging.model.BaseCommand;
import com.example.common.messaging.model.BaseEvent;

public abstract class EventCommandMapper {
    public static BaseEvent toEvent(BaseCommand command, EventType eventType) {
        return new BaseEvent(command.getAggregateId(), eventType, command.getPayload(), command.getSagaId(),
                command.getSagaInstanceId());
    }
}
