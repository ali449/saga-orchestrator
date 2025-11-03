package me.example.inventory.service;

import lombok.RequiredArgsConstructor;
import me.example.inventory.dao.StockOrderDao;
import me.example.inventory.entity.StockOrderEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockOrderService {
    private final StockOrderDao dao;

    @Transactional
    public void createIfNotCompletedExists(String aggregateId) {
        Optional<StockOrderEntity> optEntity = dao.findByAggregateId(aggregateId);
        if (optEntity.isPresent()) {
            if (optEntity.get().isCompleted()) {
                throw new RuntimeException("Completed stock order for aggregateId " + aggregateId +
                        " already exists");
            }
            return;
        }
        StockOrderEntity entity = new StockOrderEntity();
        entity.setAggregateId(aggregateId);
        dao.save(entity);
    }

    public void completed(String aggregateId) {
        dao.updateIsCompletedByAggregateId(aggregateId, true);
    }

    public void delete(String aggregateId) {
        dao.deleteById(aggregateId);
    }
}
