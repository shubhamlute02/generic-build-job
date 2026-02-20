package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class UserLeadSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userLeadSequenceId;
    private String userName;
    private int sequenceOrder;

    @ManyToOne
    @JoinColumn(name = "leadId",referencedColumnName = "leadId")
    @JsonBackReference
    private Lead lead;
}
