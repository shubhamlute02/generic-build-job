package com.accrevent.radius.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SharePointURLDTO {
    private String id;
    private String webUrl;
    private String name;
    private String parentDriveId;
    private boolean folder;
    private boolean file;
}
