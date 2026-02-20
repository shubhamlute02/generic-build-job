package com.accrevent.radius.dto;

import lombok.Data;

@Data
public class EventDTO {
    private String subject;
    private Long startDate;
    private Long endDate;
    private String eventOrganizer;
}
