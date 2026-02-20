package com.accrevent.radius.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long companyId;

    private String name;

    @OneToOne(mappedBy = "company", cascade = CascadeType.ALL)
    private CompanyResearch companyResearch;

}
