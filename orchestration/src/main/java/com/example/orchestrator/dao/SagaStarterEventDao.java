package com.example.orchestrator.dao;

import com.example.orchestrator.entity.SagaStarterEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SagaStarterEventDao extends JpaRepository<SagaStarterEventEntity, String> {

}
