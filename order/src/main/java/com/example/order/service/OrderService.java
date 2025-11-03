package com.example.order.service;

import com.example.common.messaging.enums.EventType;
import com.example.common.messaging.model.BaseEvent;
import com.example.common.statics.KafkaNames;
import com.example.order.dto.OrderIn;
import com.example.order.messaging.OrderEventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {
    private final OrderEventPublisher publisher;
    private final ObjectMapper objectMapper;
    ///private final Tracer tracer;

    @Transactional
    public String create(OrderIn model) {
        /*Span span = tracer.nextSpan().name(EventType.ORDER_CREATED.toString()).start();
        try (Tracer.SpanInScope ignored = tracer.withSpan(span)){

        } finally {
            span.end();
        }*/

        String orderId = UUID.randomUUID().toString();
        BaseEvent orderCreated = new BaseEvent(orderId, EventType.ORDER_CREATED, model.toSagaPayload(objectMapper));
        publisher.publish(KafkaNames.ORDER_EVENTS, orderCreated);
        return orderId;
    }
}
