package com.accrevent.radius.dto;

import lombok.Data;

import java.util.List;

@Data
public class CompanyResearchDTO {

    private Long id;
    private Long companyId;
    private String revenue;
    private String employeeCount;
    private String productOrServices;
    private String hqCountry;
    private List<ResearchTopicDTO> research;
}
