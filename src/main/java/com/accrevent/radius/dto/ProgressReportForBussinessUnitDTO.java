package com.accrevent.radius.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ProgressReportForBussinessUnitDTO {

    private String campaignId;
    private Long plannedStartDate;
    private Long actualStartDate;
    private Long plannedEndDate;
    private Long actualEndDate;
    private int noOfTargetCompanies;
    private Map<String, Integer> noOfContacts;
    Map<String, Map<String, Integer>> lifecycleCounts;

}
