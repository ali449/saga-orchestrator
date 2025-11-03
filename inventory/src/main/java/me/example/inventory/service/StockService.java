package me.example.inventory.service;

import com.example.common.messaging.enums.CommandType;
import com.example.common.messaging.enums.EventType;
import com.example.common.messaging.model.BaseCommand;
import com.example.common.messaging.model.BaseEvent;
import com.example.common.messaging.model.mapper.EventCommandMapper;
import lombok.RequiredArgsConstructor;
import me.example.inventory.dto.StockIn;
import me.example.inventory.dto.StockOut;
import me.example.inventory.messaging.InventoryEventPublisher;
import me.example.inventory.scheduling.RedisStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {
    private static final Logger logger = LoggerFactory.getLogger(StockService.class);
    private final InventoryEventPublisher publisher;
    private final RedisStockService redisStockService;
    private final StockOrderService stockOrderService;

    @Transactional
    public void onCommand(BaseCommand command) {
        if (command.getCommandType() == CommandType.RESERVE_STOCK) {
            reserveStock(command);
        } else if (command.getCommandType() == CommandType.RELEASE_STOCK) {
            releaseStock(command);
        }
    }

    @Transactional
    public void onEvent(BaseEvent event) {
        if (!event.isFailureEvent() && event.getEventType().isCompletedEvent()) {
            completeReservation(event);
        }
    }

    private void completeReservation(BaseEvent event) {
        try {
            stockOrderService.completed(event.getAggregateId());

            String stockId = event.getPayload().get("stockId").asText();
            redisStockService.deleteFromActiveReservations(stockId);
        } catch (Exception e) {
            logger.error("Failed to complete stock order {} due to {}", event.getAggregateId(), e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }

    private void reserveStock(BaseCommand command) {
        boolean success = false;
        try {
            stockOrderService.createIfNotCompletedExists(command.getAggregateId());

            String id = command.getPayload().get("stockId").asText();
            int quantity = command.getPayload().get("quantity").asInt();
            success = redisStockService.reserveStock(command.getAggregateId(), id, quantity);
        } catch (Exception e) {
            logger.error("Reserve stock order failed for {} due to {} ", command.getAggregateId(), e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } finally {
            publishEventFoReserveCommand(command, success);
        }
    }

    private void publishEventFoReserveCommand(BaseCommand command, boolean isReservationSuccessful) {
        BaseEvent event = EventCommandMapper.toEvent(command, EventType.STOCK_RESERVED);
        if (!isReservationSuccessful) {
            event.markThisIsFailureEvent();
        }
        publisher.publishEvent(event);
    }

    private void releaseStock(BaseCommand command) {
        logger.warn("Releasing stock {}", command.getAggregateId());
        boolean success = false;
        try {
            stockOrderService.delete(command.getAggregateId());

            String id = command.getPayload().get("stockId").asText();
            success = redisStockService.releaseStock(command.getAggregateId(), id);
        } catch (Exception e) {
            logger.error("Release stock order failed for {} due to {}", command.getAggregateId(), e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } finally {
            if (success) {
                publisher.publishEvent(EventCommandMapper.toEvent(command, EventType.STOCK_RELEASED));
            }
        }
    }

    @Transactional
    public String create(StockIn model) {
        String id = model.name();
        increaseStock(id, model.quantity());
        return id;
    }

    private void increaseStock(String id, int amount) {
        long result = redisStockService.increaseStockAmount(id, amount);
        logger.info("Increase stock result: {}", result);
    }

    public StockOut getById(String id) {
        int stockAmount = redisStockService.getStockAmount(id);
        logger.info("Get stock amount: {}", stockAmount);
        return new StockOut(id, id, stockAmount);
    }

    @Transactional
    public boolean delete(String id) {
        boolean result = redisStockService.clearStockAmount(id);
        logger.info("Clear stock amount: {}", result);
        return result;
    }
}
