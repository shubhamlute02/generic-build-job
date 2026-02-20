package com.accrevent.radius.dto;

import lombok.Data;

@Data
public class InWorkVersionResponseDTO {
    private Long versionId;
    private String version;
    private String status;

    private Long marketingStoryId;
    private String marketingStoryTitle;

    private String contentType;
    private String contentConsumer;
    private String description;

    private Long latestDueDate;
    private boolean inactive;

}

