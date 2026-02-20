package com.accrevent.radius.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
@Table
public class EditLifecycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String LifecycleName;

    private String type;

    @ElementCollection
    private List<String> lifecycleStates;
}
