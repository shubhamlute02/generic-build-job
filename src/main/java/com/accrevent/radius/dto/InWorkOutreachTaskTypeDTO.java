package com.accrevent.radius.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InWorkOutreachTaskTypeDTO {
    private Long campaignId;
    private String campaignName;
    private String lifecycleStatus;
    private List<getInWorkPhoneOutreachTasksDTO> taskList;
}
