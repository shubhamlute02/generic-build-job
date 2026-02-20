package com.accrevent.radius.dto;

import lombok.Data;

@Data
public class LeadLifecycleCountDTO {
    private long identified;
    private long research;
    private long prospecting;

    public LeadLifecycleCountDTO(long identified, long research, long prospecting) {
        this.identified = identified;
        this.research = research;
        this.prospecting = prospecting;
    }
}
