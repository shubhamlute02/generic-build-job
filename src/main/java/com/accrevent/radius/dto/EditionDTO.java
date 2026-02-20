package com.accrevent.radius.dto;

import lombok.Data;

import java.util.List;

@Data
public class EditionDTO {
    private Long id;
    private String contentType;

    private List<VersionDTO> versions;

    private String editionFolderId;
}
