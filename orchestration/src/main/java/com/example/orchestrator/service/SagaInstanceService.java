package com.example.orchestrator.service;

import com.example.common.enums.ExecutionState;
import com.example.common.enums.StepState;
import com.example.common.messaging.enums.EventType;
import com.example.common.messaging.model.BaseCommand;
import com.example.common.messaging.model.BaseEvent;
import com.example.common.messaging.model.SagaStatusMessage;
import com.example.common.utils.Try;
import com.example.orchestrator.dao.SagaInstanceDao;
import com.example.orchestrator.dto.*;
import com.example.orchestrator.dto.mapper.SagaStatusMessageMapper;
import com.example.orchestrator.exception.SagaInitException;
import com.example.orchestrator.entity.SagaInstanceEntity;
import com.example.orchestrator.entity.SagaStepInstanceEntity;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SagaInstanceService {
    private static final Logger logger = LoggerFactory.getLogger(SagaInstanceService.class);
    private final SagaService sagaService;
    private final SagaStepService sagaStepService;
    private final ApplicationEventPublisher eventPublisher;
    private final SagaInstanceDao dao;
    private final SagaTimeoutService sagaTimeoutService;

    @Transactional
    public void run(BaseEvent event) {
        SagaHolder holder = init(event);
        SagaInstanceEntity instance = holder.instance();
        if (!holder.success()) {
            publishResult(SagaExecuteResult.failBuilder()
                    .sagaInstanceId(event.getSagaInstanceId())
                    .message("Init failed for " + event)
                    .status(SagaStatusMessageMapper.map(instance))
                    .build()
            );
            return;
        }

        if (holder.start() && instance.hasExpireTime()) {
            sagaTimeoutService.scheduleTimeout(event, holder.sagaId(), instance.getId(), instance.getExpiresAt());
        }

        advice(event, holder.start(), holder.sagaId(), instance, holder.sagaSteps());
    }

    @Transactional
    public void advice(BaseEvent event, boolean start, String sagaId, SagaInstanceEntity instance,
                       List<SagaStepDto> sagaSteps) {
        String sagaInstanceId = instance.getId();

        if (sagaSteps.isEmpty()) {
            logger.error("No steps found for for sagaId={}, eventType={}", sagaId, event.getEventType());
            publishResult(SagaExecuteResult.failBuilder()
                    .sagaInstanceId(sagaInstanceId)
                    .message("No steps found")
                    .status(SagaStatusMessageMapper.map(instance))
                    .build()
            );
            return;
        }
        SagaStepDto currentStep;
        if (!start) {
            currentStep = sagaSteps.get(instance.getCurrentStep() - 1);
            if (!isValidTransition(event, currentStep)) {
                logger.error("Invalid state for sagaInstanceId={}, eventType={}", sagaInstanceId, event.getEventType());
                publishResult(SagaExecuteResult.failBuilder()
                        .sagaInstanceId(sagaInstanceId)
                        .message("Invalid state transition")
                        .status(SagaStatusMessageMapper.map(instance))
                        .build()
                );
                return;
            }

            SagaStepInstanceEntity stepInstance = getOrCreateStepInstance(instance, currentStep,
                    event.getEventType());
            stepInstance.incrementRetryCount();
            stepInstance.setStatus(StepState.COMPLETED);
        }

        instance.setCurrentStep(instance.getCurrentStep() + 1);

        OutBox outBox = null;
        if (instance.getCurrentStep() <= sagaSteps.size()) {
            SagaStepDto nextStep = sagaSteps.stream()
                    .filter(step -> step.stepOrder().equals(instance.getCurrentStep()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Invalid saga step"));

            outBox = new OutBox(
                    nextStep.commandTopic(),
                    new BaseCommand(event.getAggregateId(), nextStep.commandType(),
                            instance.getContextData(), sagaId, sagaInstanceId),
                    event.getEventType()
            );
        } else {
            instance.setStatus(ExecutionState.COMPLETED);
            if (instance.hasExpireTime()) {
                sagaTimeoutService.cancelSchedule(instance.getTriggerEventId());
            }
        }

        dao.save(instance);

        publishResult(SagaExecuteResult.successBuilder()
                .sagaInstanceId(sagaInstanceId)
                .outBox(outBox)
                .status(SagaStatusMessageMapper.map(instance))
                .triggeringEvent(event)
                .build()
        );
    }

    private void publishResult(SagaExecuteResult result) {
        eventPublisher.publishEvent(result);
    }

    private SagaHolder init(BaseEvent event) {
        String sagaInstanceId = event.getSagaInstanceId();
        boolean isStartEvent = isStart(sagaInstanceId);

        try {
            SagaInstanceEntity instance;
            if (isStartEvent) {
                validateInit(event);
                instance = createNewSagaInstance(event);
            } else {
                instance = loadExistingSagaInstance(sagaInstanceId);
                validateInstance(instance);
            }

            List<SagaStepDto> steps = sagaStepService.getAllBySagaIdSorted(instance.getSagaId());

            return SagaHolder.success(instance.getSagaId(), instance, steps, isStartEvent);
        } catch (SagaInitException e) {
            logger.error(e.getMessage());
            return SagaHolder.fail();
        }
    }

    private void validateInit(BaseEvent event) {
        if (existsByAggregateId(event.getAggregateId())) {
            throw new SagaInitException("Saga already exists for aggregateId " + event.getAggregateId());
        }
    }

    private void validateInstance(SagaInstanceEntity instance) {
        if (instance.isTerminated()) {
            throw new SagaInitException("Saga already terminated");
        }
        if (instance.isExpired()) {
            throw new SagaInitException("Saga already expired");
        }
    }

    private SagaInstanceEntity createNewSagaInstance(BaseEvent event) {
        SagaDto saga = sagaService.getByTrigger(event.getEventType());
        if (saga == null) {
            throw new SagaInitException("No saga definition for event type " + event.getEventType());
        }

        return dao.save(new SagaInstanceEntity(saga, event));
    }

    private SagaInstanceEntity loadExistingSagaInstance(String sagaInstanceId) {
        return dao.findByIdWithSteps(sagaInstanceId)
                .orElseThrow(() ->
                        new SagaInitException("No saga instance found for sagaInstanceId " + sagaInstanceId)
                );
    }

    private static boolean isStart(String sagaInstanceId) {
        return !StringUtils.hasText(sagaInstanceId);
    }

    private boolean existsByAggregateId(String aggregateId) {
        return dao.existsByAggregateId(aggregateId);
    }

    @Transactional
    public void onEachConsumerRetry(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
        if (deliveryAttempt > 0) {
            logger.warn("Retrying for {}, topic={}, error: {}", deliveryAttempt, record.topic(), ex.getMessage());
        }
    }

    @Transactional
    public void onConsumerRetryExhausted(ConsumerRecord<?, ?> record, Exception ex) {
        logger.warn("Consumer retry exhausted", ex);
        Object value = record.value();
        if (!(value instanceof BaseEvent event)) {
            logger.warn("Unexpected event type: {}", value.getClass());
            return;
        }
        handleSagaFailureAndPublishCompensationCommands(event);
        moveToDeadLetter(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onProducerRetryExhausted(BaseCommand command, EventType triggerEvent) {
        Try.run(() -> markSagaFailed(command, triggerEvent))
                .onFailure(ex -> {
                    logger.error("Producer retry failed", ex);
                    rollbackCurrentTransaction();
                });
        moveToDeadLetter(command);

        SagaStatusMessage status = getSagaStatusMessage(command.getSagaInstanceId());
        publishResult(SagaExecuteResult.successBuilder()
                .sagaInstanceId(command.getSagaInstanceId())
                .status(status)
                .build()
        );
    }

    @Transactional
    public void onTimeoutOrFailure(BaseEvent event) {
        logger.warn("onTimeoutOrFailure {}", event);
        handleSagaFailureAndPublishCompensationCommands(event);
        moveToDeadLetter(event);
    }

    private void handleSagaFailureAndPublishCompensationCommands(BaseEvent event) {
        List<OutBox> compensationCommands = Try.of(() -> handleSagaFailureAndPrepareCompensationCommands(event))
                .onFailure(e -> {
                    logger.error("Compensation failed", e);
                    rollbackCurrentTransaction();
                })
                .getOrElse(Collections.emptyList());

        SagaStatusMessage status = getSagaStatusMessage(event.getSagaInstanceId());

        compensationCommands.forEach(compensationCommand ->
                publishResult(SagaExecuteResult.successBuilder()
                        .sagaInstanceId(event.getSagaInstanceId())
                        .outBox(compensationCommand)
                        .status(status)
                        .build()
                )
        );
    }

    private SagaStatusMessage getSagaStatusMessage(String sagaInstanceId) {
        Optional<SagaInstanceEntity> optInstance = Try.of(() -> dao.findByIdWithSteps(sagaInstanceId))
                .getOrNull();
        if (optInstance != null && optInstance.isPresent()) {
            return SagaStatusMessageMapper.map(optInstance.get());
        }
        return null;
    }

    private List<OutBox> handleSagaFailureAndPrepareCompensationCommands(BaseEvent event) {
        return handleSagaFailureAndPrepareCompensationCommands(event.getSagaId(), event.getSagaInstanceId(),
                event.getAggregateId(), event.getPayload(),
                event.getEventType(), true, event.isFailureEvent());
    }

    private void markSagaFailed(BaseCommand command, EventType triggerEvent) {
        handleSagaFailureAndPrepareCompensationCommands(command.getSagaId(), command.getSagaInstanceId(),
                command.getAggregateId(),
                command.getPayload(), triggerEvent, false, false);
    }

    private List<OutBox> handleSagaFailureAndPrepareCompensationCommands(
            String sagaId, String sagaInstanceId, String aggregateId, JsonNode payload, EventType triggerEvent,
            boolean shouldCompensate, boolean isFailureEvent) {
        boolean start = isStart(sagaInstanceId);
        List<OutBox> outBoxList = new ArrayList<>();
        List<SagaStepDto> sagaSteps = getSagaSteps(sagaId, triggerEvent, start);
        if (!start) {
            dao.findByIdWithSteps(sagaInstanceId).ifPresent(instance -> {
                if (instance.isTerminated()) {
                    logger.error("Instance {} currently terminated with status {}.",
                            sagaInstanceId, instance.getStatus());
                    return;
                }

                if (!shouldCompensate) {
                    instance.setStatus(ExecutionState.FAILED);
                    dao.save(instance);
                    return;
                }
                instance.setStatus(ExecutionState.COMPENSATING);
                markLastStepInstanceAsFailed(instance);
                dao.save(instance);

                //Add last element after the if-else
                int currentStep = Math.min(instance.getCurrentStep(), sagaSteps.size() - 1);
                for (int i = currentStep; i > 0; i--) {
                    SagaStepDto sagaStep = sagaSteps.get(i - 1);

                    if (needPublishFailureCommand(isFailureEvent, sagaStep, triggerEvent)) {
                        BaseCommand outBoxCommand = new BaseCommand(
                                aggregateId, sagaStep.onFailureCommand(), payload, sagaId, sagaInstanceId
                        );
                        outBoxList.add(new OutBox(sagaStep.commandTopic(), outBoxCommand, triggerEvent));
                    }
                }
            });
        }

        SagaStepDto lastSagaStep = sagaSteps.stream()
                .max(Comparator.comparing(SagaStepDto::stepOrder))
                .orElseThrow(() -> new IllegalStateException("No steps found!"));

        if (needPublishFailureCommand(isFailureEvent, lastSagaStep, triggerEvent)) {
            BaseCommand outBoxCommand = new BaseCommand(
                    aggregateId, lastSagaStep.onFailureCommand(), payload, sagaId, sagaInstanceId
            );
            outBoxList.add(new OutBox(lastSagaStep.commandTopic(), outBoxCommand, triggerEvent));
        }

        return outBoxList;
    }

    /**
     * For prevent duplication (e.g. if RESERVE_STOCK failed, we don't need to publish RELEASE_STOCK command)
     */
    private static boolean needPublishFailureCommand(boolean isFailureEvent, SagaStepDto sagaStep,
                                                     EventType triggerEvent) {
        return !(isFailureEvent && sagaStep.expectedEventType() == triggerEvent);
    }

    private List<SagaStepDto> getSagaSteps(String sagaId, EventType triggerEvent, boolean isStart) {
        if (isStart) {
            sagaId = sagaService.getIdByTrigger(triggerEvent);
            if (sagaId == null) {
                throw new IllegalStateException("No saga found for event " + triggerEvent);
            }
        }
        return sagaStepService.getAllBySagaIdSorted(sagaId);
    }

    private void markLastStepInstanceAsFailed(SagaInstanceEntity instance) {
        instance.getSteps().stream()
                .max(Comparator.comparing(SagaStepInstanceEntity::getStepOrder))
                .ifPresent(lastStep -> lastStep.setStatus(StepState.FAILED));
    }

    @Transactional
    public void onCompensatoryEvent(BaseEvent event) {
        //Try: Because we don't want handle compensatory events in consumer container.
        SagaInstanceEntity instance = Try.of(() -> updateSagaCompensationStatus(event))
                .onFailure(ex -> {
                    logger.error("onCompensatoryEvent failed", ex);
                    rollbackCurrentTransaction();
                    moveToDeadLetter(event);
                })
                .getOrNull();

        if (instance != null) {
            SagaStatusMessage status = SagaStatusMessageMapper.map(instance);
            publishResult(SagaExecuteResult.successBuilder()
                    .sagaInstanceId(event.getSagaInstanceId())
                    .status(status)
                    .build()
            );
        }
    }

    private SagaInstanceEntity updateSagaCompensationStatus(BaseEvent event) {
        SagaInstanceEntity instance = dao.findByIdWithSteps(event.getSagaInstanceId()).orElse(null);
        if (instance == null) {
            logger.warn("No instance found for event {}", event);
            return null;
        }

        for (SagaStepInstanceEntity stepInstance : instance.getSteps()) {
            if (stepInstance.getEventReceived() == event.getEventType().getCompensatoryRelatedEvent()) {
                stepInstance.setStatus(StepState.COMPENSATED);
            }
        }

        List<SagaStepInstanceEntity> stepInstances = instance.getSteps().stream()
                .sorted(Comparator.comparing(SagaStepInstanceEntity::getStepOrder))
                .toList();
        int currentStep = instance.getCurrentStep();
        boolean allCompensated = true;
        for (int i = 0; i < currentStep && i < stepInstances.size(); i++) {
            allCompensated = allCompensated && (StepState.COMPENSATED == stepInstances.get(i).getStatus());
        }

        if (allCompensated) {
            instance.setStatus(ExecutionState.FAILED);
        }
        return dao.save(instance);
    }

    public void moveToDeadLetter(Object obj) {
        logger.error("Error {}: {}", obj.getClass().getSimpleName(), obj);
    }

    private static boolean isValidTransition(BaseEvent event, SagaStepDto currentStep) {
        return event.getEventType() == currentStep.expectedEventType();
    }

    private static SagaStepInstanceEntity getOrCreateStepInstance(SagaInstanceEntity instance,
                                                                  SagaStepDto step, EventType eventType) {
        return instance.getSteps().stream()
                .filter(a -> a.getStepId().equals(step.id()))
                .findFirst()
                .orElseGet(() -> {
                    SagaStepInstanceEntity stepInstance = new SagaStepInstanceEntity();
                    stepInstance.setCommandType(step.commandType());
                    stepInstance.setEventReceived(eventType);
                    stepInstance.setStatus(StepState.IN_PROGRESS);
                    stepInstance.setRetryCount((short) 0);
                    stepInstance.setStepOrder(step.stepOrder());
                    stepInstance.setName(step.name());
                    stepInstance.setInstance(instance);
                    stepInstance.setStepId(step.id());

                    instance.getSteps().add(stepInstance);
                    return stepInstance;
                });
    }

    private static void rollbackCurrentTransaction() {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }
}
