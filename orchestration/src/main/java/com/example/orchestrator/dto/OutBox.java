package com.example.orchestrator.dto;

import com.example.common.messaging.enums.EventType;
import com.example.common.messaging.model.BaseCommand;

public record OutBox(String topic, BaseCommand command, EventType triggerEvent) {
}
