package com.example.orchestrator.dto;

import com.example.orchestrator.entity.SagaInstanceEntity;

import java.util.List;

public record SagaHolder(String sagaId, SagaInstanceEntity instance,
                         List<SagaStepDto> sagaSteps, boolean start, boolean success) {

    public static SagaHolder success(String sagaId, SagaInstanceEntity instance,
                                     List<SagaStepDto> sagaSteps, boolean start) {
        return new SagaHolder(sagaId, instance, sagaSteps, start, true);
    }

    public static SagaHolder fail() {
        return new SagaHolder(null, null, null, false, false);
    }
}
