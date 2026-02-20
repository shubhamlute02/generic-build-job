package com.accrevent.radius.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name ="linkedin_outreach_campaign_maturity_record_register")
public class LinkedInOutreachCampaignMaturityRecordRegister {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long date;

    @Column(nullable = false)
    private Long campaignId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private Long count;
}
