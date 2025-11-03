package com.example.orchestrator.init;

import com.example.orchestrator.service.SagaService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer {
    private final SagaService sagaService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeData() {
        if (!sagaService.existsByName("OrderStockSaga")) {
            sagaService.createOrderStockSaga();
        }
    }
}
