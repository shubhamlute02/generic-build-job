package com.accrevent.radius.controller;

import com.accrevent.radius.util.LifecycleName;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController

@RequestMapping("/constantLifecycle")
public class ConstantLifecycleController {

    @GetMapping("/getOutreachTaskLifecycleNames")
    public ResponseEntity<List<String>> getOutreachTaskLifecycleNames() {
        return ResponseEntity.ok(LifecycleName.getEmailOutreachTaskLifecycleNames());
    }

    @GetMapping("/getTaskLifecycleNames")
    public ResponseEntity<List<String>> getTaskLifecycleNames() {
        return ResponseEntity.ok(LifecycleName.getTaskLifecycleNames());
    }

    @GetMapping("/getCampaignLifecycleNames")
    public ResponseEntity<List<String>> getCampaignLifecycleNames() {
        return ResponseEntity.ok(LifecycleName.getCampaignLifecycleNames());
    }

    @GetMapping("/getOutreachCampaignLifecycleNames")
    public ResponseEntity<List<String>> getOutreachCampaignLifecycleNames() {
        return ResponseEntity.ok(LifecycleName.getOutreachCampaignLifecycleNames());
    }

    @GetMapping("/getLeadLifecycleNames")
    public ResponseEntity<List<String>> getLeadLifecycleNames() {
        return ResponseEntity.ok(LifecycleName.getLeadLifecycleNames());
    }

    @GetMapping("/getOpportunityLifecycleNames")
    public ResponseEntity<List<String>> getOpportunityLifecycleNames() {
        return ResponseEntity.ok(LifecycleName.getOpportunityLifecycleNames());
    }

    @GetMapping("/getLinkedInOutreachTaskLifecycleNames")
    public ResponseEntity<List<String>> getLinkedInOutreachTaskLifecycleNames() {
        return ResponseEntity.ok(LifecycleName.getLinkedInOutreachTaskLifecycleNames());
    }

    @GetMapping("/getPhoneOutreachTaskLifecycleNames")
    public ResponseEntity<List<String>> getPhoneOutreachTaskLifecycleNames() {
        return ResponseEntity.ok(LifecycleName.getPhoneOutreachTaskLifecycleNames());
    }


    @GetMapping("/getPromotionAutomationPhoneOutreachTaskLifecycleNames")
    public ResponseEntity<List<String>> getPromotionAutomationPhoneOutreachTaskLifecycleNames() {
        return ResponseEntity.ok(LifecycleName.getPromotionAutomationPhoneOutreachTaskLifecycleNames());
    }


    @GetMapping("/getVersionLifecycleNames")
    public ResponseEntity<List<String>> getVersionLifecycleNames() {
        return ResponseEntity.ok(LifecycleName.getVersionLifecycleNames());
    }

}
