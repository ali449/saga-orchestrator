package com.example.orchestrator.dao;

import com.example.orchestrator.entity.SagaStepInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SagaStepInstanceDao extends JpaRepository<SagaStepInstanceEntity, String> {

}
