package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class UserTaskSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userTaskSequenceId;
    private String userName;
    private int sequenceOrder;

    @ManyToOne
    @JoinColumn(name = "taskId",referencedColumnName = "taskId")
    @JsonBackReference(value = "task-sequence")
    private Task task;
}
