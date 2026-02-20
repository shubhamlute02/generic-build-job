package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class UserCampaignSpecSeq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userCampaignSpecSequenceId;
    private String userName;
    private int sequenceOrder;

    @ManyToOne
    @JoinColumn(name = "specificationId",referencedColumnName = "specificationId")
    @JsonBackReference
    private CampaignSpecification campaignSpecification;
}
