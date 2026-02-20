package com.accrevent.radius.controller;

import com.accrevent.radius.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OutreachCampaignMaturityController {

    private final EmailOutreachCampaignMaturityRecordRegisterService emailMaturityService;
    private final LinkedInOutreachCampaignMaturityRecordRegisterService maturityService;
    private final PhoneOutreachCampaignMaturityRecordRegisterService phoneMaturityService;
   private final MarketingMeetingRegisterService marketingMeetingRegisterService;
   private final PhoneCallingRegisterService phoneCallingRegisterService;

    public OutreachCampaignMaturityController(EmailOutreachCampaignMaturityRecordRegisterService emailMaturityService, LinkedInOutreachCampaignMaturityRecordRegisterService maturityService, PhoneOutreachCampaignMaturityRecordRegisterService phoneMaturityService, MarketingMeetingRegisterService marketingMeetingRegisterService, PhoneCallingRegisterService phoneCallingRegisterService) {
        this.emailMaturityService = emailMaturityService;
        this.maturityService = maturityService;
        this.phoneMaturityService = phoneMaturityService;
        this.marketingMeetingRegisterService= marketingMeetingRegisterService;
        this.phoneCallingRegisterService = phoneCallingRegisterService;
    }

    @GetMapping("/getEmailOutreachCampaignMaturityReport")
    public ResponseEntity<?> getEmailOutreachCampaignMaturityReport(@RequestParam Long campaignId) {
        return ResponseEntity.ok(emailMaturityService.getEmailOutreachMaturityReport(campaignId));
    }

    @GetMapping("/getLinkedInOutreachCampaignMaturityReport")
    public ResponseEntity<?> getLinkedInOutreachCampaignMaturityReport(@RequestParam Long campaignId) {
        return ResponseEntity.ok(maturityService.getLinkedInMaturityReport(campaignId));
    }

    @GetMapping("/getPhoneOutreachCampaignMaturityReport")
    public ResponseEntity<?> getPhoneOutreachCampaignMaturityReport(@RequestParam Long campaignId) {
        return ResponseEntity.ok(phoneMaturityService.getPhoneMaturityReport(campaignId));
    }

    @GetMapping("/getMarketingMeetingRegisterReport")
    public ResponseEntity<?> getMarketingMeetingRegisterReport() {
        return ResponseEntity.ok(marketingMeetingRegisterService.getMarketingMeetingRegisterReport());
    }

    @GetMapping("/getPhoneCallingRegisterReport")
    public ResponseEntity<?> getPhoneCallingRegisterReport(@RequestParam Long campaignId) {
        return ResponseEntity.ok(phoneCallingRegisterService.getPhoneCallingMaturityReport(campaignId));
    }
}
