package com.example.orchestrator;

import com.example.common.enums.ExecutionState;
import com.example.common.enums.StepState;
import com.example.common.messaging.enums.CommandType;
import com.example.common.messaging.enums.EventType;
import com.example.common.messaging.model.BaseCommand;
import com.example.common.messaging.model.BaseEvent;
import com.example.common.messaging.model.SagaStatusMessage;
import com.example.common.messaging.model.mapper.EventCommandMapper;
import com.example.common.statics.KafkaNames;
import com.example.orchestrator.dao.*;
import com.example.orchestrator.dto.SagaExecuteResult;
import com.example.orchestrator.entity.SagaStepInstanceEntity;
import com.example.orchestrator.messaging.SagaListener;
import com.example.orchestrator.service.SagaInstanceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.*;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.clearInvocations;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false"
})
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:test-db;DB_CLOSE_DELAY=-1"
})
@EmbeddedKafka(
        partitions = 1,
        topics = {KafkaNames.ORDER_EVENTS, KafkaNames.ORDER_COMMANDS, KafkaNames.INVENTORY_EVENTS,
                KafkaNames.INVENTORY_COMMANDS, KafkaNames.PAYMENT_EVENTS, KafkaNames.PAYMENT_COMMANDS
        },
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"}
)
public class SagaTests {
    private KafkaTemplate<String, BaseEvent> eventKafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private SagaDao sagaDao;
    @Autowired
    private SagaStepDao sagaStepDao;
    @Autowired
    private SagaInstanceDao instanceDao;
    @Autowired
    private SagaStepInstanceDao stepInstanceDao;
    @Autowired
    private SagaStarterEventDao eventDao;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    @Autowired
    private SagaListener sagaListener;

    @MockitoSpyBean
    @Autowired
    private SagaInstanceService instanceService;

    @BeforeAll
    void setup() {
        eventKafkaTemplate = new KafkaTemplate<>(KafkaTestUtilsEx.eventProducerFactory(embeddedKafka));
    }

    @BeforeEach
    void clearDatabase() {
        reset(sagaListener);
        eventDao.deleteAll();
        stepInstanceDao.deleteAll();
        instanceDao.deleteAll();
    }

    @Test
    void test_embedded_kafka() {
        assertThat(embeddedKafka).isNotNull();
    }

    @Test
    void test_saga() {
        assertThat(sagaDao.count()).isEqualTo(1);
        assertThat(sagaStepDao.count()).isEqualTo(3);
    }

    @Test
    void test_normal_flow() {
        //0
        BaseEvent orderEvent = getOrderEvent();
        eventKafkaTemplate.send(KafkaNames.ORDER_EVENTS, orderEvent);

        //Random group not works for first time, why?
        try (var consumer = KafkaTestUtilsEx.createEventConsumer(embeddedKafka, KafkaNames.ORCHESTRATOR_GROUP)) {
            KafkaTestUtilsEx.drain(consumer, KafkaNames.ORDER_EVENTS);
            ///sagaListener.onEvent(event);
        }
        check_step(CommandType.RESERVE_STOCK, 1, 0);

        //1: Simulate Inventory Service
        BaseCommand reserveStockCommand;
        try (var consumer = KafkaTestUtilsEx.createCommandConsumer(embeddedKafka)) {
            List<ConsumerRecord<String, BaseCommand>> records = KafkaTestUtilsEx
                    .drain(consumer, KafkaNames.INVENTORY_COMMANDS);
            assertThat(records.size()).isEqualTo(1);
            reserveStockCommand = records.get(0).value();
        }
        assertThat(reserveStockCommand).isNotNull();
        assertThat(reserveStockCommand.getCommandType()).isEqualTo(CommandType.RESERVE_STOCK);

        BaseEvent stockReservedEvent = EventCommandMapper.toEvent(reserveStockCommand, EventType.STOCK_RESERVED);
        eventKafkaTemplate.send(KafkaNames.INVENTORY_EVENTS, stockReservedEvent);

        //2
        check_step(CommandType.PROCESS_PAYMENT, 2, 1);

        //3: Simulate Payment Service
        BaseCommand processPaymentCommand;
        try (var consumer = KafkaTestUtilsEx.createCommandConsumer(embeddedKafka)) {
            List<ConsumerRecord<String, BaseCommand>> records = KafkaTestUtilsEx
                    .drain(consumer, KafkaNames.PAYMENT_COMMANDS);
            assertThat(records.size()).isEqualTo(1);
            processPaymentCommand = records.get(0).value();
        }
        assertThat(processPaymentCommand).isNotNull();
        assertThat(processPaymentCommand.getCommandType()).isEqualTo(CommandType.PROCESS_PAYMENT);

        BaseEvent paymentProcessedEvent = EventCommandMapper.toEvent(processPaymentCommand,
                EventType.PAYMENT_SUCCEEDED);
        eventKafkaTemplate.send(KafkaNames.PAYMENT_EVENTS, paymentProcessedEvent);

        //4
        check_step(CommandType.COMPLETE_ORDER, 3, 2);


        //3: Simulate Order Service
        BaseCommand orderCompletedCommand;
        try (var consumer = KafkaTestUtilsEx.createCommandConsumer(embeddedKafka)) {
            List<ConsumerRecord<String, BaseCommand>> records = KafkaTestUtilsEx
                    .drain(consumer, KafkaNames.ORDER_COMMANDS);
            assertThat(records.size()).isEqualTo(1);
            orderCompletedCommand = records.get(0).value();
        }
        assertThat(orderCompletedCommand).isNotNull();
        assertThat(orderCompletedCommand.getCommandType()).isEqualTo(CommandType.COMPLETE_ORDER);

        BaseEvent orderCompletedEvent = EventCommandMapper.toEvent(orderCompletedCommand,
                EventType.ORDER_COMPLETED);
        eventKafkaTemplate.send(KafkaNames.PAYMENT_EVENTS, orderCompletedEvent);

        check_step(null, 4, 3);

        check_database(orderCompletedCommand.getSagaInstanceId());
    }

    private void check_database(String sagaInstanceId) {
        var instances = instanceDao.findAll();
        assertThat(instances.size()).isEqualTo(1);

        var instance = instanceDao.findByIdWithSteps(sagaInstanceId).orElse(null);
        assertThat(instance).isNotNull();
        assertThat(instance.getStatus()).isEqualTo(ExecutionState.COMPLETED);

        var stepInstances = instance.getSteps();
        assertThat(stepInstances.size()).isEqualTo(3);
        for (SagaStepInstanceEntity stepInstance : stepInstances) {
            assertThat(stepInstance.getStatus()).isEqualTo(StepState.COMPLETED);
        }
    }

    private void check_step(CommandType expectedCommandType, int expectedStep, int expectedStepSize) {
        ArgumentCaptor<SagaExecuteResult> captor = ArgumentCaptor.forClass(SagaExecuteResult.class);
        verify(sagaListener, timeout(5000)).afterSagaCommit(captor.capture());

        SagaExecuteResult result = captor.getValue();
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();

        if (expectedCommandType != null) {
            BaseCommand command = result.outBox().command();
            assertThat(command.getCommandType()).isEqualTo(expectedCommandType);
        } else {
            assertThat(result.outBox()).isNull();
        }

        SagaStatusMessage status = result.status();
        assertThat(status).isNotNull();
        assertThat(status.currentStep()).isEqualTo(expectedStep);
        assertThat(status.steps()).hasSize(expectedStepSize);

        clearInvocations(sagaListener);
    }

    @Test
    void test_fail_step_0() {
        BaseEvent orderEvent = getOrderEvent();

        doThrow(new RuntimeException("OOPS!"))
                .when(instanceService)
                .run(argThat(event -> event.getEventType() == EventType.ORDER_CREATED));

        eventKafkaTemplate.send(KafkaNames.ORDER_EVENTS, orderEvent);

        try (var consumer = KafkaTestUtilsEx.createEventConsumer(embeddedKafka, KafkaNames.ORCHESTRATOR_GROUP)) {
            KafkaTestUtilsEx.drain(consumer, KafkaNames.ORDER_EVENTS);
        }

        verify(instanceService, timeout(5000).atLeastOnce())
                .onEachConsumerRetry(any(), any(), anyInt());
        verify(instanceService, timeout(5000))
                .onConsumerRetryExhausted(any(), any());

        check_database_step0_failed();
    }

    private void check_database_step0_failed() {
        var instances = instanceDao.findAll();
        assertThat(instances.size()).isEqualTo(0);
    }

    @Test
    void test_fail_step_2() {

        doThrow(new RuntimeException("OOPS!"))
                .when(instanceService)
                .run(argThat(event -> event.getEventType() == EventType.STOCK_RESERVED));

        //0
        BaseEvent orderEvent = getOrderEvent();
        eventKafkaTemplate.send(KafkaNames.ORDER_EVENTS, orderEvent);

        try (var consumer = KafkaTestUtilsEx.createEventConsumer(embeddedKafka)) {
            var recordList = KafkaTestUtilsEx.drain(consumer, KafkaNames.ORDER_EVENTS);
            ConsumerRecord<String, BaseEvent> record = recordList.get(0);
            sagaListener.onEvent(record.value());//We need call it directly for first time, why?
        }
        check_step(CommandType.RESERVE_STOCK, 1, 0);

        //1: Simulate Inventory Service
        BaseCommand reserveStockCommand;
        try (var consumer = KafkaTestUtilsEx.createCommandConsumer(embeddedKafka)) {
            List<ConsumerRecord<String, BaseCommand>> records = KafkaTestUtilsEx
                    .drain(consumer, KafkaNames.INVENTORY_COMMANDS);
            assertThat(records.size()).isEqualTo(1);
            reserveStockCommand = records.get(0).value();
        }
        assertThat(reserveStockCommand).isNotNull();
        assertThat(reserveStockCommand.getCommandType()).isEqualTo(CommandType.RESERVE_STOCK);

        BaseEvent stockReservedEvent = EventCommandMapper.toEvent(reserveStockCommand, EventType.STOCK_RESERVED);
        eventKafkaTemplate.send(KafkaNames.INVENTORY_EVENTS, stockReservedEvent);

        verify(instanceService, timeout(5000).times(3))
                .onEachConsumerRetry(any(), any(), anyInt());
        verify(instanceService, timeout(5000))
                .onConsumerRetryExhausted(any(), any());

        check_database_step1_failed(stockReservedEvent.getSagaInstanceId());
    }

    private void check_database_step1_failed(String sagaInstanceId) {
        var instances = instanceDao.findAll();
        assertThat(instances.size()).isEqualTo(1);

        var instance = instanceDao.findByIdWithSteps(sagaInstanceId).orElse(null);
        assertThat(instance).isNotNull();
        assertThat(instance.getStatus()).isEqualTo(ExecutionState.COMPENSATING);
        
        assertThat(instance.getSteps().size()).isEqualTo(0);
    }

    private BaseEvent getOrderEvent() {
        JsonNode payload = null;
        try {
            payload = objectMapper.readTree(orderJson());
        } catch (JsonProcessingException ignored) {

        }

        String orderId = UUID.randomUUID().toString();
        return new BaseEvent(orderId, EventType.ORDER_CREATED, payload);
    }

    private String orderJson() {
        return """
                {
                   "stockId": "700",
                   "quantity": 100
                 }
                """;
    }

     /*Consumer<String, BaseCommand> orderConsumer = KafkaTestUtilsEx.createCommandConsumer(embeddedKafka,
                KafkaNames.ORDER_GROUP);
        embeddedKafka.consumeFromAnEmbeddedTopic(orderConsumer, KafkaNames.ORDER_COMMANDS);

        ConsumerRecord<String, BaseCommand> record = KafkaTestUtils.getSingleRecord(orderConsumer,
                KafkaNames.ORDER_COMMANDS,
                Duration.ofMillis(5000));

        //assertThat(record.value()).usingRecursiveComparison().isEqualTo(orderEvent);*/
}
