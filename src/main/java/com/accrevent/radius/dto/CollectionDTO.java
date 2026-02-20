package com.accrevent.radius.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
public class CollectionDTO {

    private Long marketingCollectionId;
    private String displayName;
    private List<Long> marketingStoryIds;
    private Long workspaceId;
    private Long inWorkVersionCount;
    private String collectionFolderId;
}
