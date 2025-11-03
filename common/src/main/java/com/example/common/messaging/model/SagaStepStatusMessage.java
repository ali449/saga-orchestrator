package com.example.common.messaging.model;

import com.example.common.enums.StepState;

public record SagaStepStatusMessage(Integer stepOrder, String name, StepState status, Short retryCount) {

}