package com.accrevent.radius.dto;

import lombok.Data;

import java.util.List;

@Data
public class MarketingStoryDTO {
    private Long id;
    private String title;
    private String description;
    private String purpose;
    private String contentConsumer;
    private Long marketingCollectionId;
    private String marketingCollectionName;
    private List<EditionDTO> editions;
    private String folderId;
}
