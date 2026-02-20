package com.accrevent.radius.controller;

import com.accrevent.radius.service.DataCleansingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dataCleansing")
public class DataCleansingController {

    private final DataCleansingService dataCleansingService;

    public DataCleansingController(DataCleansingService dataCleansingService) {
        this.dataCleansingService = dataCleansingService;
    }

    @PutMapping("/buIOT")
    public ResponseEntity<String> updateBusinessUnitIOT() {
        String result = dataCleansingService.updateBusinessUnitIOT();
        return ResponseEntity.ok(result);
    }

    @PutMapping("/updateLifecycleStatusConvention")
    public ResponseEntity<String> updateTaskStatus() {

        int updated = dataCleansingService.updatelifecyclestatusConvention();
        return ResponseEntity.ok("Total updated tasks: " + updated);
    }

    @PutMapping("/updateOldOutreachTaskType")
    public ResponseEntity<String> updateOutreachType() {
        String result = dataCleansingService.updateOutreachTaskType();
        return ResponseEntity.ok(result);
    }

    @PutMapping("/updateLeadStatusConvention")
    public ResponseEntity<String> updateLeadStatusConvention() {

        int updated = dataCleansingService.updateLeadStatusConvention();
        return ResponseEntity.ok("Total updated Leads: " + updated);
    }

    @PutMapping("/updateOpportunityStatusConvention")
    public ResponseEntity<String> updateOpportunityStatusConvention() {

        int updated = dataCleansingService.updateOpportunityStatusConvention();
        return ResponseEntity.ok("Total updated opportunities: " + updated);
    }

    @PutMapping("/updateCampaignStatusConvention")
    public ResponseEntity<String> updateCampaignStatusConvention() {

        int updated = dataCleansingService.updateCampaignStatusConvention();
        return ResponseEntity.ok("Total updated campaigns: " + updated);
    }

    @PutMapping("/callingToCalled")
    public String callingToCalled() {

        dataCleansingService.changeCallingToCalled();
        return "All Calling statuses changed to Called";
    }

    @PutMapping("/closingToNegotiating")
    public String closingToNegotiating() {

        dataCleansingService.closingToNegotiating();
        return "All opportunity-closing statuses changed to negotiating";
    }



}
