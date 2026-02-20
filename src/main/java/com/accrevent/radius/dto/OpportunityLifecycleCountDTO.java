package com.accrevent.radius.dto;

import lombok.Data;

@Data
public class OpportunityLifecycleCountDTO {
    private long discovery;
    private long proposal;
    private long customerEvaluating;
    private long closedWon;

    public OpportunityLifecycleCountDTO(long discovery, long proposal, long customerEvaluating, long closedWon) {
        this.discovery = discovery;
        this.proposal = proposal;
        this.customerEvaluating = customerEvaluating;
        this.closedWon = closedWon;
    }
}
