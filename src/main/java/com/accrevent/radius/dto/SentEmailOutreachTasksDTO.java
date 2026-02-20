package com.accrevent.radius.dto;

import lombok.Data;

import java.util.List;

@Data
public class SentEmailOutreachTasksDTO {
    private List<EmailOutreachTaskDTO> taskEmailOutreachList;
}
