package me.example.inventory.entity;

import com.example.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "stock_reservation")
public class StockOrderEntity extends BaseEntity {

    //e.g. Order Id
    @Column(nullable = false, unique = true)
    private String aggregateId;

    @Column(nullable = false)
    private boolean completed;
}
