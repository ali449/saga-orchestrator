package com.example.orchestrator.dao;

import com.example.orchestrator.entity.SagaInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SagaInstanceDao extends JpaRepository<SagaInstanceEntity, String> {
    @Query("SELECT s FROM SagaInstanceEntity s LEFT JOIN FETCH s.steps WHERE s.id = :id")
    Optional<SagaInstanceEntity> findByIdWithSteps(String id);

    boolean existsByAggregateId(String triggerAggregateId);
}
