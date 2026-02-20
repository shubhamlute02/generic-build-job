package com.accrevent.radius.dto;

import com.accrevent.radius.util.CampaignType;
import lombok.Data;


@Data
public class CampaignDTO {

    private Long campaignId;
    private String campaignName;
    private String description;
    private Long workspaceId;
    private Long createdOn;
    private String Owner;
    private Boolean isSystemComment = false;
    private Boolean isArchived = false;
    private Long plannedStartDate;
    private Long actualStartDate;
    private Long plannedEndDate;
    private Long actualEndDate;
    private String path;

    private String type = CampaignType.DEFAULT;

}
