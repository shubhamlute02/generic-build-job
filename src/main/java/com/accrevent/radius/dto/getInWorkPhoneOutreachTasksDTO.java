package com.accrevent.radius.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class getInWorkPhoneOutreachTasksDTO {
    private Long taskId;
    private String taskName;
    private Long nextWorkDate;
}
