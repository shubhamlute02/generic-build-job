package com.accrevent.radius.dto;
import lombok.Data;
import java.time.ZonedDateTime;

@Data
public class CommentsDTO {
    private Long commentId;
    private String commentsTitle;
    private String commentDescription;
    private String createdBy;
    private Long createdOn;
    private Long campaignId;
    private Long opportunityId;
    private Long leadId;
    private Long taskId;
    private Long versionId;
    private boolean unreadFlag;
    private Boolean isSystemComment = false;
}
