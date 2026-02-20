package com.accrevent.radius.dto;

import lombok.Data;

@Data
public class CompanyDTO {

    private Long companyId;
    private String name;

    private CompanyResearchDTO companyResearch;// Added this field
}
