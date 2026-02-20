package com.accrevent.radius.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Data
@Table(name = "outreach_campaign_maturity_record")
public class OutreachCampaignMaturityRecord {

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
