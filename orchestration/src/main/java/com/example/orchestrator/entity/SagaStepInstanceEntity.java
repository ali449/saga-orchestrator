package com.example.orchestrator.entity;

import com.example.common.entity.BaseEntity;
import com.example.common.enums.StepState;
import com.example.common.messaging.enums.CommandType;
import com.example.common.messaging.enums.EventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

//Defines a single command/event pair in the saga.
@Setter
@Getter
@Entity
@Table(name = "saga_step_instance",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"instance_id", "step_id"})}
)
public class SagaStepInstanceEntity extends BaseEntity {
    //Command executed
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CommandType commandType;

    //Event received
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventReceived;

    //Step state
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StepState status;

    //Retry attempts
    @Column(nullable = false)
    private Short retryCount;

    //Failure info
    @Column
    private String errorDetails;

    @Column(nullable = false)
    private Integer stepOrder;

    //Step name ("ReserveStock")
    @Column(nullable = false)
    private String name;

    @Column(name = "instance_id", nullable = false, insertable = false, updatable = false)
    private String instanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    private SagaInstanceEntity instance;

    @Column(name = "step_id", nullable = false)
    private String stepId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false, insertable = false, updatable = false)
    private SagaStepEntity step;

    public void incrementRetryCount() {
        retryCount++;
    }
}
