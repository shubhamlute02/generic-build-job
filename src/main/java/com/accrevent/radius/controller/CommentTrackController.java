package com.accrevent.radius.controller;

import com.accrevent.radius.model.CampaignCommentTrack;
import com.accrevent.radius.model.LeadCommentTrack;
import com.accrevent.radius.model.OpportunityCommentTrack;
import com.accrevent.radius.model.TaskCommentTrack;
import com.accrevent.radius.service.CampaignCommentTrackService;
import com.accrevent.radius.service.LeadCommentTrackService;
import com.accrevent.radius.service.OpportunityCommentTrackService;
import com.accrevent.radius.service.TaskCommentTrackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/CommentTrackByUser")
public class CommentTrackController {

    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "failure";
    private static final String USER_TASK_ID_REQUIRED = "UserId and task Id must be provided in the request body.";
    private static final String USER_LEAD_ID_REQUIRED = "UserId and lead Id must be provided in the request body.";
    private static final String USER_OPPORTUNITY_ID_REQUIRED = "UserId and opportunity Id must be provided in the request body.";
    private static final String USER_CAMPAIGN_ID_REQUIRED = "UserId and campaign Id must be provided in the request body.";
    private final TaskCommentTrackService taskCommentTrackService;
    private final LeadCommentTrackService leadCommentTrackService;
    private final OpportunityCommentTrackService opportunityCommentTrackService;
    private final CampaignCommentTrackService campaignCommentTrackService;

    public CommentTrackController(TaskCommentTrackService taskCommentTrackService, LeadCommentTrackService leadCommentTrackService, OpportunityCommentTrackService opportunityCommentTrackService, CampaignCommentTrackService campaignCommentTrackService) {
        this.taskCommentTrackService = taskCommentTrackService;
        this.leadCommentTrackService = leadCommentTrackService;
        this.opportunityCommentTrackService = opportunityCommentTrackService;
        this.campaignCommentTrackService = campaignCommentTrackService;
    }

    @PostMapping("/CreateOrUpdateTaskCommentTrackByUser")
    public ResponseEntity<Map<String,Object>> CreateOrUpdateTaskCommentTrackByUser(@RequestBody TaskCommentTrack taskCommentTrack){
        if(taskCommentTrack.getTaskId() == null ||  taskCommentTrack.getUserId() == null){
            return buildResponse(HttpStatus.OK, ERROR_STATUS, USER_TASK_ID_REQUIRED);
        }
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("TaskCommentTrack", taskCommentTrackService.createOrUpdateTaskCommentTrack(taskCommentTrack));
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "TaskCommentTrack created or Updated successfully.", responseBody);
    }

    @PostMapping("/CreateOrUpdateLeadCommentTrackByUser")
    public ResponseEntity<Map<String,Object>> CreateOrUpdateLeadCommentTrackByUser(@RequestBody LeadCommentTrack leadCommentTrack){
        if(leadCommentTrack.getLeadId() == null ||  leadCommentTrack.getUserId() == null){
            return buildResponse(HttpStatus.OK, ERROR_STATUS, USER_LEAD_ID_REQUIRED);
        }
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("LeadCommentTrack", leadCommentTrackService.createOrUpdateLeadCommentTrack(leadCommentTrack));
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "LeadCommentTrack created or Updated successfully.", responseBody);
    }

    @PostMapping("/CreateOrUpdateOpportunityCommentTrackByUser")
    public ResponseEntity<Map<String,Object>> CreateOrUpdateOpportunityCommentTrackByUser(@RequestBody OpportunityCommentTrack opportunityCommentTrack){
        if(opportunityCommentTrack.getOpportunityId() == null ||  opportunityCommentTrack.getUserId() == null){
            return buildResponse(HttpStatus.OK, ERROR_STATUS, USER_OPPORTUNITY_ID_REQUIRED);
        }
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("OpportunityCommentTrack", opportunityCommentTrackService.createOrUpdateOpportunityCommentTrack(opportunityCommentTrack));
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "OpportunityCommentTrack created or Updated successfully.", responseBody);
    }

    @PostMapping("/CreateOrUpdateCampaignCommentTrackByUser")
    public ResponseEntity<Map<String,Object>> CreateOrUpdateCampaignCommentTrackByUser(@RequestBody CampaignCommentTrack campaignCommentTrack){
        if(campaignCommentTrack.getCampaignId() == null ||  campaignCommentTrack.getUserId() == null){
            return buildResponse(HttpStatus.OK, ERROR_STATUS, USER_OPPORTUNITY_ID_REQUIRED);
        }
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("CampaignCommentTrack", campaignCommentTrackService.createOrUpdateCampaignCommentTrack(campaignCommentTrack));
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Campaign CommentTrack created or Updated successfully.", responseBody);
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
