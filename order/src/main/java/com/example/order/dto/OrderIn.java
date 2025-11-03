package com.example.order.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record OrderIn(String stockId, int quantity) {

    public ObjectNode toSagaPayload(ObjectMapper objectMapper) {
        return objectMapper.createObjectNode()
                .put("stockId", stockId)
                .put("quantity", quantity);
                //.put("startDate", startDate.format(DateTimeFormatter.ISO_DATE))
                //.put("endDate", endDate.format(DateTimeFormatter.ISO_DATE));
    }
}
