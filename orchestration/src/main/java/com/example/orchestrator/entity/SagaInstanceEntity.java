package com.example.orchestrator.entity;

import com.example.common.entity.BaseEntity;
import com.example.common.enums.ExecutionState;
import com.example.common.messaging.model.BaseEvent;
import com.example.orchestrator.converter.JsonNodeConverter;
import com.example.orchestrator.dto.SagaDto;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

//Represents a running instance of a SAGA triggered by an event.
@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "saga_instance")
public class SagaInstanceEntity extends BaseEntity {

    //The event id that starts saga
    @Column(nullable = false, unique = true)
    private String triggerEventId;

    //The originating business ID (e.g., orderId)
    @Column(nullable = false, unique = true)
    private String aggregateId;

    //Index of current step
    @Column(nullable = false)
    private Integer currentStep;

    //Execution state
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ExecutionState status;

    //Context variables (e.g., orderId, paymentId, productId)
    @Convert(converter = JsonNodeConverter.class)
    @Column(columnDefinition = "CLOB", nullable = false)
    private JsonNode contextData;

    @Column
    private LocalDateTime expiresAt;

    @Column(name = "saga_id", nullable = false)
    private String sagaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saga_id", nullable = false, insertable = false, updatable = false)
    private SagaEntity saga;

    @OneToMany(mappedBy = "instance", cascade = CascadeType.ALL)
    private Set<SagaStepInstanceEntity> steps = new HashSet<>();

    public SagaInstanceEntity(SagaDto saga, BaseEvent event) {
        this.triggerEventId = event.getEventId();
        this.aggregateId = event.getAggregateId();
        this.currentStep = 0;
        this.status = ExecutionState.RUNNING;
        this.contextData = event.getPayload();
        this.sagaId = saga.id();
        if (saga.expirationDuration() != null) {
            this.expiresAt = LocalDateTime.now().plus(saga.expirationDuration());
        }
    }

    public boolean isTerminated() {
        return status == ExecutionState.COMPLETED || status == ExecutionState.FAILED;
    }

    public boolean hasExpireTime() {
        return expiresAt != null;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public Duration getRemainingTime() {
        if (expiresAt == null) {
            return Duration.ZERO;
        }
        return Duration.between(LocalDateTime.now(), expiresAt);
    }
}
