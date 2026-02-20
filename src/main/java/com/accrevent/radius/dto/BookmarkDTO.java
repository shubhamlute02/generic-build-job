package com.accrevent.radius.dto;

import lombok.Data;

@Data
public class BookmarkDTO {

    private String userName;
    private Long leadId;
    private Long opportunityId;
    private Boolean isBookmarked;

}
