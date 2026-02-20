package com.accrevent.radius.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class OpportunityDTO {
    private Long opportunityId;
    private String opportunityName;
    private String description;
    private Long workspaceId;
    private String requirement;
    private String customer;
    private String estimateRevenue;
    private String currency;
    private String projectTitle;
    private String businessUnit;
    private Long createdOn;
    private Boolean isSystemComment = false;
    private Boolean isArchived = false;
    private String path;

}
