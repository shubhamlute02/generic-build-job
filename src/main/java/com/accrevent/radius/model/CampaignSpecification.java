package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
@Entity
public class CampaignSpecification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long specificationId;
    private String title;
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaignId", nullable = false,referencedColumnName ="campaignId")
    @JsonBackReference
    private Campaign campaign;

    @OneToMany(mappedBy = "campaignSpecification",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<UserCampaignSpecSeq> userCampaignSpecSeqList = new ArrayList<>();
}
