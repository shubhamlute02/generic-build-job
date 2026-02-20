package com.accrevent.radius.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
@Entity
@Data
@Table(
        name = "company_research",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "company_id")
        }
)
public class CompanyResearch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "company_id", referencedColumnName = "companyId", unique = true)
    private Company company;

    private String revenue;
    private String employeeCount;
    private String productOrServices;
    private String hqCountry;


    @OneToMany(mappedBy = "companyResearch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ResearchTopic> research = new ArrayList<>();
}
