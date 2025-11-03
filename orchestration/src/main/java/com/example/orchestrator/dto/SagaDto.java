package com.example.orchestrator.dto;

import com.example.orchestrator.entity.SagaEntity;

import java.time.Duration;

public record SagaDto(String id, Duration expirationDuration) {
    public SagaDto(SagaEntity entity) {
        this(entity.getId(), entity.getExpirationDuration());
    }
}
