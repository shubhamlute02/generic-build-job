package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class UserCampaignSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userCampaignSequenceId;
    private String userName;
    private int sequenceOrder;

    @ManyToOne
    @JoinColumn(name = "campaignId",referencedColumnName = "campaignId")
    @JsonBackReference
    private Campaign campaign;
}
