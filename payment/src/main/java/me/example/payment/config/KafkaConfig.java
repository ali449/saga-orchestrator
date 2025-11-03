package me.example.payment.config;

import com.example.common.messaging.model.BaseCommand;
import com.example.common.messaging.model.BaseEvent;
import com.example.common.statics.KafkaNames;
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
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

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
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(KafkaNames.PAYMENT_EVENTS)
                .partitions(PARTITIONS)
                .replicas(REPLICATION)
                .build();
    }

    // ====================
    // KafkaTemplate Bean
    // ====================
    @Bean
    public ProducerFactory<String, BaseEvent> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, BaseEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, BaseCommand> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, BaseCommand.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BaseCommand> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, BaseCommand> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
