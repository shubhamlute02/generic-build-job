package com.accrevent.radius.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class ResearchTopic {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_research_id",referencedColumnName = "id",nullable = false)
    private CompanyResearch companyResearch;
}
