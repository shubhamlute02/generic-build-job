package com.accrevent.radius.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoadOutreachCampaignResponseDTO {

    private int leadsLoaded;
    private int taskCreated;
    private String message;
}
