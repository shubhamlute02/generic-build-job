package com.accrevent.radius.dto;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class VersionDTO {
    private Long versionId;
    private String version;
    private String description;
    private String sharepointPath;
    private String sharepointUrl;
    private String status;
    private Long editionId;
    private String folderId;

}
