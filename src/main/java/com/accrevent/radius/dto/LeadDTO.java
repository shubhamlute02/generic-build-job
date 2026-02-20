package com.accrevent.radius.dto;
import lombok.Data;

@Data
public class LeadDTO {

    private Long leadId;
    private String leadName;
    private String leadTitle;
    private String description;
    private Long campaignId;
    private Long createdOn;
    private Boolean isSystemComment = false;
    private Boolean isArchived = false;
    private String businessUnit;
    private String path;

}
