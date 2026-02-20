package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ConstantLifecycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long constantLifecycleId;

    @Column(nullable = false)
    private Long cycleId;
    @Column(nullable = false)
    private String cycleName;
}
