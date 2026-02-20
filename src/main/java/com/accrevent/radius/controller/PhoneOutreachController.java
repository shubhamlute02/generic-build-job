package com.accrevent.radius.controller;

import com.accrevent.radius.service.LoadPhoneOutreachService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PhoneOutreachController {

    private static final Logger logger = LoggerFactory.getLogger(PhoneOutreachController.class);

    private final LoadPhoneOutreachService loadPhoneOutreachService;

    public PhoneOutreachController(LoadPhoneOutreachService loadPhoneOutreachService) {
        this.loadPhoneOutreachService = loadPhoneOutreachService;
    }

    @PostMapping("/loadPhoneOutreachFromFile")
    public ResponseEntity<String> loadPhoneOutreachFromFile(
            @RequestParam String campaignId,
            @RequestParam String filePath) {

        logger.info("Received request to load PHONE outreach tasks for campaign {} from file {}", campaignId, filePath);

        try {
            Long campaignIdLong = Long.parseLong(campaignId);
            loadPhoneOutreachService.loadPhoneOutreach(campaignIdLong, filePath);
            return ResponseEntity.ok("Phone outreach loaded successfully from file: " + filePath);
        } catch (NumberFormatException e) {
            logger.error("Invalid campaign ID format: {}", campaignId);
            return ResponseEntity.badRequest().body("Invalid campaign ID format");
        } catch (Exception e) {
            logger.error("Error loading phone outreach from file", e);
            return ResponseEntity.internalServerError().body("Error loading phone outreach: " + e.getMessage());
        }
    }
}
