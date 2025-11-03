package com.example.orchestrator.dto;

import com.example.common.messaging.enums.CommandType;
import com.example.common.messaging.enums.EventType;
import com.example.orchestrator.entity.SagaStepEntity;

public record SagaStepDto(
        String id,
        Integer stepOrder,
        String name,
        CommandType commandType,
        String commandTopic,
        EventType expectedEventType,
        CommandType onFailureCommand) {

    public SagaStepDto(SagaStepEntity entity) {
        this(entity.getId(), entity.getStepOrder(), entity.getName(), entity.getCommandType(),
                entity.getCommandTopic(), entity.getExpectedEventType(), entity.getOnFailureCommand());
    }
}
