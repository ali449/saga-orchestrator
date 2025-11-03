package com.example.orchestrator.dao;

import com.example.common.messaging.enums.EventType;
import com.example.orchestrator.entity.SagaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SagaDao extends JpaRepository<SagaEntity, String> {
    Optional<SagaEntity> findByTriggerEvent(EventType eventType);

    boolean existsByName(String name);
}
