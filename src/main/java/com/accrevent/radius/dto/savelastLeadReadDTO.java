package com.accrevent.radius.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class savelastLeadReadDTO {
    @NotNull(message = "username is required")
    private String userName;
    @NotNull(message = "LeadId is required")
    private Long leadId;
    @NotNull(message = "lastLeadViewed is required")
    private Long lastLeadViewed;
}