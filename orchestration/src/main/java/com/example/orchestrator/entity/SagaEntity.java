package com.example.orchestrator.entity;

import com.example.common.entity.BaseEntity;
import com.example.common.messaging.enums.EventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Setter
@Getter
@Entity
@Table(name = "saga")
public class SagaEntity extends BaseEntity {

    //Business name (e.g., "OrderFulfillment")
    @Column(nullable = false)
    private String name;

    //Version number (to evolve safely)
    @Column(nullable = false)
    private Integer definedVersion;

    //Indicates if this workflow is in use
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    //Event name that starts workflow (e.g., "OrderCreated")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType triggerEvent;

    //Total saga expiration
    @Column
    private Duration expirationDuration;

    @OneToMany(mappedBy = "saga", cascade = {CascadeType.REMOVE, CascadeType.PERSIST})
    private Set<SagaStepEntity> steps = new HashSet<>();

    @OneToMany(mappedBy = "saga", cascade = CascadeType.REMOVE)
    private Set<SagaInstanceEntity> instances = new HashSet<>();

    public void setExpiration(int expirationValue, TimeUnit expirationUnit) {
        this.expirationDuration = Duration.of(expirationValue, expirationUnit.toChronoUnit());
    }

    @PrePersist
    @PreUpdate
    private void validateStep() {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalStateException("At least one step is required");
        }
    }
}
