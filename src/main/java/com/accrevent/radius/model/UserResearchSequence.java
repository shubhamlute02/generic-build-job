package com.accrevent.radius.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class UserResearchSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userResearchSequenceId;

    private String userName;
    private Integer sequenceOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_research_id")
    private CompanyResearch companyResearch;
}
