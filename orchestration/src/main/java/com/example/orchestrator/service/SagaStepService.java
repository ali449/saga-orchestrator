package com.example.orchestrator.service;

import com.example.orchestrator.dao.SagaStepDao;
import com.example.orchestrator.dto.SagaStepDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SagaStepService {
    private final SagaStepDao dao;

    public List<SagaStepDto> getAllBySagaIdSorted(String sagaId) {
        return dao.findAllBySagaIdOrderByStepOrder(sagaId).stream()
                .map(SagaStepDto::new)
                .toList();
    }
}
