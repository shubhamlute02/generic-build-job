package com.accrevent.radius.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table
public class SelectedLifecycle {

    @Id
    private Long Id;
    private Long campaign;
    private Long opportunity;
    private Long Lead;
    private String type;
    private Long requirement;
    private Long staffSelectionProcess;
}
