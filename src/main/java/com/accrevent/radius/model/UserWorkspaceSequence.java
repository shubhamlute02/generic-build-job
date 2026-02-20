package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@ToString(exclude = "workspace")
public class UserWorkspaceSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userWorkspaceSequenceId;
    private String userName;
    private int sequenceOrder;

    @ManyToOne
    @JoinColumn(name = "workspaceId",referencedColumnName = "workspaceId")
    @JsonBackReference
    private Workspace workspace;
}
