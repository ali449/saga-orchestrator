package com.example.common.messaging.model;

import com.example.common.messaging.enums.CommandType;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
public class BaseCommand {
    private final String commandId = UUID.randomUUID().toString();
    private String sagaId;
    private String sagaInstanceId;
    private String aggregateId;
    private final LocalDateTime occurredAt = LocalDateTime.now();
    private CommandType commandType;
    private JsonNode payload;

    public BaseCommand(String aggregateId, CommandType commandType, JsonNode payload, String sagaId,
                       String sagaInstanceId) {
        this.aggregateId = aggregateId;
        this.commandType = commandType;
        this.payload = payload;
        this.sagaId = sagaId;
        this.sagaInstanceId = sagaInstanceId;
    }

    @Override
    public String toString() {
        return "BaseCommand{" +
                "commandId='" + commandId + '\'' +
                ", sagaId='" + sagaId + '\'' +
                ", sagaInstanceId='" + sagaInstanceId + '\'' +
                ", domainId='" + aggregateId + '\'' +
                ", occurredAt=" + occurredAt +
                ", commandType=" + commandType +
                ", payload='" + payload + '\'' +
                '}';
    }
}
