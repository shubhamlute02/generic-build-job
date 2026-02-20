package com.accrevent.radius.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResearchTopicDTO {
    private Long id;
    private String topic;
    private String description;


}
