package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class UserOpportunitySequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userOpportunitySequenceId;
    private String userName;
    private int sequenceOrder;

    @ManyToOne
    @JoinColumn(name = "opportunityId",referencedColumnName = "opportunityId")
    @JsonBackReference
    private Opportunity opportunity;
}
