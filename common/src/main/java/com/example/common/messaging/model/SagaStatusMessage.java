package com.example.common.messaging.model;

import com.example.common.enums.ExecutionState;

import java.util.List;

public record SagaStatusMessage(String aggregateId, Integer currentStep, ExecutionState status,
                                List<SagaStepStatusMessage> steps) {
}
