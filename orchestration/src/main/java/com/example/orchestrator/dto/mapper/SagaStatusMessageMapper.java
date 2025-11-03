package com.example.orchestrator.dto.mapper;

import com.example.common.messaging.model.SagaStatusMessage;
import com.example.common.messaging.model.SagaStepStatusMessage;
import com.example.orchestrator.entity.SagaInstanceEntity;
import org.hibernate.Hibernate;

import java.util.ArrayList;
import java.util.List;

public abstract class SagaStatusMessageMapper {

    public static SagaStatusMessage map(SagaInstanceEntity instance) {
        if (Hibernate.isInitialized(instance) && instance != null) {
            List<SagaStepStatusMessage> steps = new ArrayList<>();
            if (Hibernate.isInitialized(instance.getSteps())) {
                instance.getSteps().forEach(stepInstance -> {
                    steps.add(new SagaStepStatusMessage(stepInstance.getStepOrder(),
                            stepInstance.getName(), stepInstance.getStatus(), stepInstance.getRetryCount())
                    );
                });
            }

            return new SagaStatusMessage(instance.getAggregateId(), instance.getCurrentStep(),
                    instance.getStatus(), steps);
        }
        return null;
    }
}
