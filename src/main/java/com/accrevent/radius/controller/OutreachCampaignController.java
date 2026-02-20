package com.accrevent.radius.controller;

import com.accrevent.radius.service.LoadOutreachCampaignService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController

public class OutreachCampaignController {

    private static final Logger logger = LoggerFactory.getLogger(OutreachCampaignController.class);

    private final LoadOutreachCampaignService loadOutreachCampaignService;

    public OutreachCampaignController( LoadOutreachCampaignService loadOutreachCampaignService) {
        this.loadOutreachCampaignService = loadOutreachCampaignService;

    }

    @PostMapping("/loadOutreachCampaignFromFile")
    public ResponseEntity<String> loadOutreachCampaignFromFile(
            @RequestParam String campaignId,
            @RequestParam String filePath) {

        logger.info("Received request to load campaign {} from file {}", campaignId, filePath);

        try {
            Long campaignIdLong = Long.parseLong(campaignId);
            loadOutreachCampaignService.loadOutreachCampaign(campaignIdLong, filePath);
            return ResponseEntity.ok("Campaign loaded successfully from file: " + filePath);
        } catch (NumberFormatException e) {
            logger.error("Invalid campaign ID format: {}", campaignId);
            return ResponseEntity.badRequest().body("Invalid campaign ID format");
        } catch (Exception e) {
            logger.error("Error loading campaign from file", e);
            return ResponseEntity.internalServerError().body("Error loading campaign: " + e.getMessage());
        }
    }
}