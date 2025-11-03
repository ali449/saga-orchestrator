package com.example.orchestrator.dao;

import com.example.orchestrator.entity.SagaStepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SagaStepDao extends JpaRepository<SagaStepEntity, String> {
    List<SagaStepEntity> findAllBySagaIdOrderByStepOrder(String sagaId);
}
