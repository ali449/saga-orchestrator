package com.example.orchestrator.entity;

import com.example.common.entity.BaseEntity;
import com.example.common.messaging.enums.CommandType;
import com.example.common.messaging.enums.EventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

//Defines a single command/event pair in the saga.
@Setter
@Getter
@Entity
@Table(name = "saga_step")
public class SagaStepEntity extends BaseEntity {
    //Execution order
    @Column(nullable = false)
    private Integer stepOrder;

    //Step name ("ReserveStock")
    @Column(nullable = false)
    private String name;

    //Command name ("ReserveStockCommand")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CommandType commandType;

    //Kafka topic to send command
    @Column(nullable = false)
    private String commandTopic;

    //Expected event to continue ("StockReservedEvent")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType expectedEventType;

    //Compensating command name
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CommandType onFailureCommand;

    //Expression or event field deciding next step
    @Column
    private String nextStepCondition;

    @Column(name = "saga_id", nullable = false, insertable = false, updatable = false)
    private String sagaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saga_id", nullable = false)
    private SagaEntity saga;

    @OneToMany(mappedBy = "step", cascade = CascadeType.REMOVE)
    private Set<SagaStepInstanceEntity> instances = new HashSet<>();
}
