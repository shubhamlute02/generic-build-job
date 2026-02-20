package com.accrevent.radius.dto;


import lombok.Data;

import java.util.List;

@Data
public class WorkspaceDTO {
    private Long workspaceId;
    private String workspaceName;
    private String description;
    private String createdOn;
    private List<CampaignDTO> campaigns;
    private List<CampaignDTO> outreachCampaigns;
    private List<OpportunityDTO> opportunities;
    private String owner;
    private long opportunityCount;
}
