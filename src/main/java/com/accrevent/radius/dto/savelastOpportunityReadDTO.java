
package com.accrevent.radius.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class savelastOpportunityReadDTO {
    @NotNull(message = "username is required")
    private String userName;
    @NotNull(message = "opportunityId is required")
    private Long opportunityId;
    @NotNull(message = "lastOpportunityView is required")
    private Long lastOpportunityViewed;
}