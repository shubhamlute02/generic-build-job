package com.accrevent.radius.dto;

import lombok.Data;

@Data
public class SalesPipelineCountDTO {

    private int identified = 0;
    private int research = 0;
    private int prospecting = 0;
    private int discovery = 0;
    private int proposal = 0;
    private int customerEvaluating = 0;
    private int closedWon = 0;
    private int closedLost = 0;
}
