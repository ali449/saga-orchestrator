package com.example.orchestrator;

import com.example.common.messaging.model.BaseCommand;
import com.example.common.messaging.model.BaseEvent;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class KafkaTestUtilsEx {

    static KafkaConsumer<String, BaseEvent> createEventConsumer(
            EmbeddedKafkaBroker embeddedKafka, String group) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(embeddedKafka.getBrokersAsString(),
                group, "true");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, BaseEvent.class);
        return new KafkaConsumer<>(props);
    }

    static KafkaConsumer<String, BaseEvent> createEventConsumer(
            EmbeddedKafkaBroker embeddedKafka) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(embeddedKafka.getBrokersAsString(),
                "test-" + UUID.randomUUID(), "true");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, BaseEvent.class);
        return new KafkaConsumer<>(props);
    }

    static KafkaConsumer<String, BaseCommand> createCommandConsumer(
            EmbeddedKafkaBroker embeddedKafka) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(embeddedKafka.getBrokersAsString(),
                "test-" + UUID.randomUUID(), "true");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, BaseCommand.class);
        return new KafkaConsumer<>(props);
    }

    static ProducerFactory<String, BaseEvent> eventProducerFactory(EmbeddedKafkaBroker embeddedKafka) {
        Map<String, Object> configs = KafkaTestUtils.producerProps(embeddedKafka);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configs);
    }

    static <V> List<ConsumerRecord<String, V>> drain(Consumer<String, V> consumer,
                                                     String topic) {
        List<ConsumerRecord<String, V>> recordsList = new ArrayList<>();

        int expectedCount = 1;
        consumer.subscribe(List.of(topic));
        AtomicInteger received = new AtomicInteger();
        long deadline = System.currentTimeMillis() + 5_000;

        while (System.currentTimeMillis() < deadline && received.get() < expectedCount) {
            ConsumerRecords<String, V> records = consumer.poll(Duration.ofMillis(200));
            for (ConsumerRecord<String, V> r : records) {
                recordsList.add(r);
                received.getAndIncrement();
                System.out.println("Received: " + r.value());
            }
        }

        if (received.get() < expectedCount) {
            throw new AssertionError(
                    "Expected " + expectedCount + " records but received " + received
                            + " before timeout");
        }
        consumer.unsubscribe();

        return recordsList;
    }
}

