package com.example.orchestrator.config;

import com.example.common.messaging.model.BaseCommand;
import com.example.common.messaging.model.BaseEvent;
import com.example.common.statics.KafkaNames;
import com.example.common.statics.SagaConstants;
import com.example.orchestrator.service.SagaInstanceService;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    private static final int PARTITIONS = 1;
    private static final short REPLICATION = 1;

    @Value(value = "${spring.kafka.bootstrap-servers}")
    private String bootstrapAddress;

    // ====================
    // KafkaAdmin (Topic Creation)
    // ====================
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        return new KafkaAdmin(configs);
    }

    // ====================
    // Command Topics
    // ====================
    @Bean
    public NewTopic orderCommandsTopic() {
        return TopicBuilder.name(KafkaNames.ORDER_COMMANDS)
                .partitions(PARTITIONS)
                .replicas(REPLICATION)
                .build();
    }

    @Bean
    public NewTopic inventoryCommandsTopic() {
        return TopicBuilder.name(KafkaNames.INVENTORY_COMMANDS)
                .partitions(PARTITIONS)
                .replicas(REPLICATION)
                .build();
    }

    @Bean
    public NewTopic paymentCommandsTopic() {
        return TopicBuilder.name(KafkaNames.PAYMENT_COMMANDS)
                .partitions(PARTITIONS)
                .replicas(REPLICATION)
                .build();
    }

    // ====================
    // Event Topics
    // ====================
    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(KafkaNames.ORDER_EVENTS)
                .partitions(PARTITIONS)
                .replicas(REPLICATION)
                .build();
    }

    @Bean
    public NewTopic inventoryEventsTopic() {
        return TopicBuilder.name(KafkaNames.INVENTORY_EVENTS)
                .partitions(PARTITIONS)
                .replicas(REPLICATION)
                .build();
    }

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(KafkaNames.PAYMENT_EVENTS)
                .partitions(PARTITIONS)
                .replicas(REPLICATION)
                .build();
    }

    @Bean
    public NewTopic sagaEventsTopic() {
        return TopicBuilder.name(KafkaNames.SAGA_EVENTS)
                .partitions(PARTITIONS)
                .replicas(REPLICATION)
                .build();
    }

    // ====================
    // KafkaTemplate Bean
    // ====================
    @Bean
    public ProducerFactory<String, BaseCommand> commandProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // --- Timeout-related properties ---
        //delivery.timeout.ms = (number of retries * (request.timeout.ms + retry.backoff.ms))
        //   + network and queue delays
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, SagaConstants.REQUEST_TIMEOUT_MS);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, SagaConstants.DELIVERY_TIMEOUT_MS);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, BaseCommand> commandKafkaTemplate() {
        return new KafkaTemplate<>(commandProducerFactory());
    }

    @Bean
    public ProducerFactory<String, BaseEvent> eventProducerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, BaseEvent> eventKafkaTemplate() {
        return new KafkaTemplate<>(eventProducerFactory());
    }

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate template = new RetryTemplate();

        FixedBackOffPolicy backOff = new FixedBackOffPolicy();
        backOff.setBackOffPeriod(SagaConstants.RETRY_INTERVAL); // 1s delay between retries
        template.setBackOffPolicy(backOff);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(SagaConstants.RETRY_THRESHOLD); // 3 retries
        template.setRetryPolicy(retryPolicy);

        return template;
    }


    @Bean
    public ConsumerFactory<String, BaseEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, BaseEvent.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BaseEvent> kafkaListenerContainerFactory(
            SagaInstanceService instanceService) {
        ConcurrentKafkaListenerContainerFactory<String, BaseEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setCommonErrorHandler(errorHandler(instanceService));
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        return factory;
    }

    //Consumer Failure
    @Bean
    public DefaultErrorHandler errorHandler(SagaInstanceService instanceService) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                instanceService::onConsumerRetryExhausted,
                new FixedBackOff(SagaConstants.RETRY_INTERVAL, SagaConstants.RETRY_THRESHOLD)
        );

        errorHandler.setRetryListeners(instanceService::onEachConsumerRetry);

        return errorHandler;
    }
}
