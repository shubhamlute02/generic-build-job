package com.accrevent.radius.controller;

import com.accrevent.radius.dto.LeadDTO;
import com.accrevent.radius.dto.savelastLeadReadDTO;
import com.accrevent.radius.model.Lead;
import com.accrevent.radius.model.UserLeadSequence;
import com.accrevent.radius.service.LeadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/lead")

public class LeadController {
    private static final Logger logger = LoggerFactory.getLogger(LeadController.class);

    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "failure";
    private static final String LEAD_ID_REQUIRED = "lead Id must be provided in the request body.";
    private static final String LEAD_CAMPAIGN_REQUIRED = "lead and campaign Id  must be provided in the request body.";
    private static final String CAMPAIGN_ID_REQUIRED = "Campaign Id must be provided in the request body.";
    private final LeadService leadService;

    public LeadController(LeadService leadService)
    {
        this.leadService = leadService;
    }

    @PutMapping("/add")
    public ResponseEntity<Map<String,Object>> createLead(@RequestBody LeadDTO leadDTO)
    {
        try
        {
            if(leadDTO.getCampaignId() == null)
            {
                logger.error(CAMPAIGN_ID_REQUIRED);
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, CAMPAIGN_ID_REQUIRED);
            }
            LeadDTO createdLead = leadService.createLead(leadDTO);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("createdLead", createdLead);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Lead successfully created.", responseBody);
        }
        catch(Exception e)
        {
            return buildResponse(HttpStatus.BAD_REQUEST,ERROR_STATUS,e.getMessage());
        }
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String,Object>> updateLead(@RequestBody LeadDTO leadDTO)
    {
        try
        {
            if(leadDTO.getCampaignId() == null || leadDTO.getLeadId() == null)
            {
                logger.error(LEAD_CAMPAIGN_REQUIRED);
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, LEAD_CAMPAIGN_REQUIRED);
            }
            LeadDTO createdLead = leadService.updateLead(leadDTO);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("updateLead", createdLead);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Lead successfully updated.", responseBody);
        }
        catch(Exception e)
        {
            return buildResponse(HttpStatus.BAD_REQUEST,ERROR_STATUS,e.getMessage());
        }
    }

    @GetMapping("/byLeadId")
    public ResponseEntity<Map<String,Object>> getLeadById(@RequestParam Long leadId)
    {
        if (leadId == null) {
            logger.error(LEAD_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, LEAD_ID_REQUIRED);
        }
        Optional<Lead>lead = leadService.getLeadById(leadId);
        if(lead.isPresent())
        {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("LeadDetail",lead.get());
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Lead Detail successfully .", responseBody);
        }else
        {
            logger.warn("Lead Detail with Lead Id: {} not found", leadId);
            return buildResponse(HttpStatus.NOT_FOUND, ERROR_STATUS, "Lead Detail with the given Lead Id not found.");
        }

    }

    @GetMapping("/getLeadByCampaignId")
    public ResponseEntity<Map<String,Object>> getLeadByCampaignId(@RequestParam Long campaignId,@RequestParam String userName)
    {
        if (campaignId == null) {
            logger.error(CAMPAIGN_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, CAMPAIGN_ID_REQUIRED);
        }
        List<LeadDTO> leadList = leadService.getLeadByCampaignId(campaignId,userName);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("LeadDetail",leadList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Lead Detail successfully .", responseBody);
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<Map<String,Object>> deleteLead(@RequestParam Long leadId){

        try {
            boolean isDeleted = leadService.deleteLead(leadId);
            if (isDeleted) {
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, leadId + " Lead successfully deleted."); // 200 OK with response body
            } else {
                logger.warn("Failed to delete lead with ID: {}", leadId);
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_STATUS, "Failed to delete the Lead.");
            }
        }catch(Exception e)
        {
            logger.warn("Deleting purpose given Lead ID: {} not found", leadId);
            return buildResponse(HttpStatus.NOT_FOUND,ERROR_STATUS,"Lead not found."); // 404 Not Found with response body

        }
    }
    @PostMapping("/updateLeadSequence")
    public ResponseEntity<Map<String,Object>> updateLeadSequence(@RequestParam String userName,@RequestBody List<LeadDTO> leads)
    {
        if(userName == null)
        {
            logger.error("user name is required");
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, "user name is required");
        }
        List<UserLeadSequence> userLeadSequenceList = leadService.updateLeadSequence(userName,leads);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("userLeadSequence", userLeadSequenceList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "User Lead sequence updated successfully.", responseBody);
    }



    @PostMapping("/saveLastReadForLead")
    public ResponseEntity<Map<String, Object>> saveLastReadForLead(@RequestBody savelastLeadReadDTO requestDTO) {
        Map<String, Object> response = new HashMap<>();

        if (requestDTO.getUserName() == null || requestDTO.getUserName().trim().isEmpty()) {
            response.put("status", "error");
            response.put("message", "Username is required");
            return ResponseEntity.badRequest().body(response);
        }
        if ((requestDTO.getLeadId() == null) ) {
            response.put("status", "error");
            response.put("message", "LeadId is required");
            return ResponseEntity.badRequest().body(response);
        }
        if (requestDTO.getLastLeadViewed() == null) {
            response.put("status", "error");
            response.put("message", "LastLeadViewed is required");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            leadService.saveLastReadForLead(requestDTO.getUserName(), requestDTO.getLeadId(), requestDTO.getLastLeadViewed());

            response.put("status", "success");
            response.put("message", "Lead last viewed data saved successfully!");
            response.put("data", requestDTO);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // If the lead was not found
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response); // 400 Bad Request
        } catch (Exception e) {
            // For any other errors
            response.put("status", "error");
            response.put("message", "An unexpected error occurred while saving last viewed data. Please check all the required field-username,leadId,LastLeadViewed should present");
            return ResponseEntity.status(500).body(response); // 500 Internal Server Error
        }
    }




    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String responseStatus, String message) {
        return buildResponse(status, responseStatus, message, new HashMap<>());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String responseStatus, String message, Map<String, Object> additionalData) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", responseStatus);
        response.put("message", message);
        response.putAll(additionalData);
        return ResponseEntity.status(status).body(response);
    }

}
