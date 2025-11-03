package com.example.orchestrator.entity;

import com.example.common.messaging.enums.EventType;
import com.example.orchestrator.converter.JsonNodeConverter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "saga_starter_event")
public class SagaStarterEventEntity {
    @Id
    @Column(unique = true, updatable = false, nullable = false)
    private String id;

    @Column(name = "saga_id", nullable = false)
    private String sagaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saga_id", nullable = false, insertable = false, updatable = false)
    private SagaEntity saga;

    @Column(name = "instance_id", nullable = false)
    private String sagaInstanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false, insertable = false, updatable = false)
    private SagaInstanceEntity instance;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Convert(converter = JsonNodeConverter.class)
    @Column(columnDefinition = "CLOB", nullable = false)
    private JsonNode payload;
}
