package me.example.inventory.dao;

import me.example.inventory.entity.StockOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockOrderDao extends JpaRepository<StockOrderEntity, String> {
    Optional<StockOrderEntity> findByAggregateId(String aggregateId);

    @Modifying
    @Query("UPDATE StockOrderEntity e SET e.completed = :isCompleted WHERE e.aggregateId = :aggregateId")
    int updateIsCompletedByAggregateId(String aggregateId, boolean isCompleted);
}
