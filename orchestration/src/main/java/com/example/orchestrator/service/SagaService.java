package com.example.orchestrator.service;

import com.example.common.statics.KafkaNames;
import com.example.common.messaging.enums.CommandType;
import com.example.common.messaging.enums.EventType;
import com.example.common.statics.SagaConstants;
import com.example.orchestrator.dao.SagaDao;
import com.example.orchestrator.dto.SagaDto;
import com.example.orchestrator.entity.SagaEntity;
import com.example.orchestrator.entity.SagaStepEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SagaService {
    private final SagaDao dao;

    //TODO: Read from .yaml file
    @Transactional
    public void createOrderStockSaga() {
        SagaEntity entity = new SagaEntity();
        entity.setName("OrderStockSaga");
        entity.setDefinedVersion(1);
        entity.setTriggerEvent(EventType.ORDER_CREATED);
        entity.setExpiration(SagaConstants.TOTAL_WORKFLOW_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        //Step 1: Reserve Stock
        SagaStepEntity reserveStock = new SagaStepEntity();
        reserveStock.setStepOrder(1);
        reserveStock.setName("Reserve Stock");
        reserveStock.setCommandType(CommandType.RESERVE_STOCK);
        reserveStock.setExpectedEventType(EventType.STOCK_RESERVED);
        reserveStock.setOnFailureCommand(CommandType.RELEASE_STOCK);
        reserveStock.setCommandTopic(KafkaNames.INVENTORY_COMMANDS);
        reserveStock.setSaga(entity);

        // Step 2: Process Payment
        SagaStepEntity processPayment = new SagaStepEntity();
        processPayment.setStepOrder(2);
        processPayment.setName("Process Payment");
        processPayment.setCommandType(CommandType.PROCESS_PAYMENT);
        processPayment.setExpectedEventType(EventType.PAYMENT_SUCCEEDED);
        processPayment.setOnFailureCommand(CommandType.REFUND_PAYMENT);
        processPayment.setCommandTopic(KafkaNames.PAYMENT_COMMANDS);
        processPayment.setSaga(entity);

        // Step 3: Complete Order
        SagaStepEntity completeOrder = new SagaStepEntity();
        completeOrder.setStepOrder(3);
        completeOrder.setName("Complete Order");
        completeOrder.setCommandType(CommandType.COMPLETE_ORDER);
        completeOrder.setExpectedEventType(EventType.ORDER_COMPLETED);
        completeOrder.setOnFailureCommand(CommandType.CANCEL_ORDER);
        completeOrder.setCommandTopic(KafkaNames.ORDER_COMMANDS);
        completeOrder.setSaga(entity);

        entity.setSteps(Set.of(reserveStock, processPayment, completeOrder));

        dao.save(entity);
    }

    public String getIdByTrigger(EventType eventType) {
        return dao.findByTriggerEvent(eventType)
                .map(SagaEntity::getId)
                .orElse(null);
    }

    public SagaDto getByTrigger(EventType eventType) {
        return dao.findByTriggerEvent(eventType)
                .map(SagaDto::new)
                .orElse(null);
    }

    public boolean existsByName(String name) {
        return dao.existsByName(name);
    }
}
