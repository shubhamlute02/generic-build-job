package com.accrevent.radius.controller;

import com.accrevent.radius.dto.CampaignDTO;
import com.accrevent.radius.dto.CampaignSpecificationDTO;
import com.accrevent.radius.dto.TaskDTO;
import com.accrevent.radius.model.Campaign;
import com.accrevent.radius.model.UserCampaignSequence;
import com.accrevent.radius.model.UserCampaignSpecSeq;
import com.accrevent.radius.model.UserTaskSequence;
import com.accrevent.radius.service.CampaignService;
import com.accrevent.radius.util.CampaignType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/campaign")

public class CampaignController {
    private static final Logger logger = LoggerFactory.getLogger(CampaignController.class);

    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "failure";
    private static final String CAMPAIGN_ID_REQUIRED = "Campaign Id must be provided in the request body.";
    private static final String CAMPAIGN_WORKSPACE_ID_REQUIRED = "Campaign and Workspace Id must be provided in the request body.";
    private static final String WORKSPACE_ID_REQUIRED = "Workspace Id must be provided in the request body.";
    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService)
    {
        this.campaignService = campaignService;
    }

        @GetMapping("/getAll")
        public ResponseEntity<Map<String, Object>> getAllCampaign()
        {
            Map<String, Object> responseBody = new HashMap<>();
            try
            {
                List<Campaign> campaignlist = campaignService.getAllCampaign();
                if (campaignlist.isEmpty()) {
                    return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "No campaign Exist");
                }
                responseBody.put("Campaign", campaignService.getAllCampaign());
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "get all campaign successfully.", responseBody);
            }
            catch(Exception e)
            {
                return buildResponse(HttpStatus.NOT_FOUND, ERROR_STATUS, e.getMessage());
            }
        }


    @PostMapping("/addSpecification")
    public ResponseEntity<Map<String,Object>> createCampaignSpecification(@RequestBody CampaignSpecificationDTO campaignSpecificationDTO)
    {
        try
        {
            if (campaignSpecificationDTO.getCampaignId() == null)
            {
                logger.error(CAMPAIGN_ID_REQUIRED);
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, CAMPAIGN_ID_REQUIRED);
            }
            CampaignSpecificationDTO createdCampaign = campaignService.createCampaignSpecification(campaignSpecificationDTO);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("createdCampaignSpecification", createdCampaign);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Campaign Specification successfully created.", responseBody);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return buildResponse(HttpStatus.BAD_REQUEST,ERROR_STATUS,e.getMessage());
        }
    }
    @PutMapping("/add")
    public ResponseEntity<Map<String,Object>> createCampaign(@RequestBody CampaignDTO campaignDTO)
    {
        try
        {
            if (campaignDTO.getWorkspaceId() == null)
            {
                logger.error(WORKSPACE_ID_REQUIRED);
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, WORKSPACE_ID_REQUIRED);
            }
            CampaignDTO createdCampaign = campaignService.createCampaign(campaignDTO);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("createdCampaign", createdCampaign);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Campaign successfully created.", responseBody);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return buildResponse(HttpStatus.BAD_REQUEST,ERROR_STATUS,e.getMessage());
        }
    }
    @PutMapping("/update")
    public ResponseEntity<Map<String,Object>> updateCampaign(@RequestBody CampaignDTO campaignDTO)
    {
        try
        {
            if (campaignDTO.getWorkspaceId() == null || campaignDTO.getCampaignId() == null)
            {
                logger.error(CAMPAIGN_WORKSPACE_ID_REQUIRED);
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, WORKSPACE_ID_REQUIRED);
            }
            CampaignDTO createdCampaign = campaignService.updateCampaign(campaignDTO);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("updatedCampaign", createdCampaign);

//            responseBody.put("isSystemComment", createdCampaign.isSystemComment());
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Campaign successfully updated.", responseBody);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return buildResponse(HttpStatus.BAD_REQUEST,ERROR_STATUS,e.getMessage());
        }
    }

    @GetMapping("/getOutreachCampaigns")
    public ResponseEntity<Map<String, Object>> getOutreachCampaigns() {
        List<CampaignDTO> outreachCampaigns = campaignService.getCampaignsByType(CampaignType.OUTREACH);

        Map<String, Object> response = new HashMap<>();
        response.put("Campaign", outreachCampaigns);

        return buildResponse(HttpStatus.OK, "success", "Outreach campaigns retrieved", response);
    }

    @GetMapping("/getOutreachCampaignsByWorkspaceId")
    public ResponseEntity<Map<String, Object>> getOutreachCampaignsByWorkspaceId(@RequestParam Long workspaceId) {
        if (workspaceId == null) {
            return buildResponse(HttpStatus.BAD_REQUEST, "failure", "Workspace Id must be provided.");
        }

        List<CampaignDTO> outreachCampaigns = campaignService.getOutreachCampaignsByWorkspaceId(workspaceId);
        if (outreachCampaigns.isEmpty()) {
            return buildResponse(HttpStatus.OK, "success", "No outreach campaigns found.");
        }

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("Campaign", outreachCampaigns);
        return buildResponse(HttpStatus.OK, "success", "Outreach campaigns fetched successfully.", responseBody);
    }




    @GetMapping("/getCampaignsByWorkspaceId")
    public ResponseEntity<Map<String,Object>> getCampaignByWorkspaceId(@RequestParam Long workspaceId)
    {
        if(workspaceId == null)
        {
            logger.error(WORKSPACE_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, WORKSPACE_ID_REQUIRED);
        }
        List<CampaignDTO> campaigns = campaignService.getCampaignByWorkspaceId(workspaceId);
        if(campaigns.isEmpty())
        {
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Campaign does not exists.");
        }
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("Campaign", campaigns);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get Campaign successfully.", responseBody);
    }

    @PostMapping("/updateCampaignSequence")
    public ResponseEntity<Map<String,Object>> updateCampaignSequence(@RequestParam String userName,@RequestBody List<CampaignDTO> campaignDTOs)
    {
        if(userName == null)
        {
            logger.error("user name is required");
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, "user name is required");
        }
        List<UserCampaignSequence> userCampaignSequenceList = campaignService.updateCampaignSequence(userName,campaignDTOs);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("userCampaignSequence", userCampaignSequenceList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "User campaign sequence updated successfully.", responseBody);
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<Map<String,Object>> deleteCampaign(@RequestParam Long campaignId){

        try {
            boolean isDeleted = campaignService.deleteCampaign(campaignId);
            if (isDeleted) {
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, campaignId + " Campaign successfully deleted."); // 200 OK with response body
            } else {
                logger.warn("Failed to delete campaign with ID: {}", campaignId);
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_STATUS, "Failed to delete the Campaign.");
            }
        }catch(Exception e)
        {
            logger.warn("Deleting purpose given Campaign ID: {} not found", campaignId);
            return buildResponse(HttpStatus.NOT_FOUND,ERROR_STATUS,"Campaign not found."); // 404 Not Found with response body

        }
    }

    @DeleteMapping("/deleteSpecificationById")
    public ResponseEntity<Map<String,Object>> deleteCampaignSpecificationById(@RequestParam Long campaignSpecificationId){

        try {
            boolean isDeleted = campaignService.deleteCampaignSpecification(campaignSpecificationId);
            if (isDeleted) {
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, campaignSpecificationId + " Campaign specification successfully deleted."); // 200 OK with response body
            } else {
                logger.warn("Failed to delete campaign specification ID: {}", campaignSpecificationId);
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_STATUS, "Failed to delete the Campaign Specification.");
            }
        }catch(Exception e)
        {
            logger.warn("Deleting purpose given Campaign specification ID: {} not found", campaignSpecificationId);
            return buildResponse(HttpStatus.NOT_FOUND,ERROR_STATUS,"Campaign specification not found."); // 404 Not Found with response body

        }
    }

    @GetMapping("/getCampaignSpecificationByCampaignId")
    public ResponseEntity<Map<String,Object>> getCampaignSpecificationByCampaignId(
            @RequestParam Long campaignId,
            @RequestParam String userName)
    {
        if(campaignId == null)
        {
            logger.error(CAMPAIGN_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, CAMPAIGN_ID_REQUIRED);
        }
        List<CampaignSpecificationDTO> campaignSpecificationDTOList = campaignService.getCampaignSpecificationByCampaignId(campaignId,userName);
        if(campaignSpecificationDTOList.isEmpty())
        {
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Campaign Specification does not exists.");
        }
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("CampaignSpecification", campaignSpecificationDTOList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get Campaign Specification successfully.", responseBody);
    }


    @PostMapping("/updateCampaignSpecSequence")
    public ResponseEntity<Map<String,Object>> updateCampaignSpecSequence(@RequestParam String userName,@RequestBody List<CampaignSpecificationDTO> taskList)
    {
        if(userName == null)
        {
            logger.error("user name is required");
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, "user name is required");
        }
        List<UserCampaignSpecSeq> userTaskSequenceList = campaignService.updateCampaignSpecificSequence(userName,taskList);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("userCampaignSpecSequence", userTaskSequenceList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "User Specification sequence updated successfully.", responseBody);
    }

//    @GetMapping("/getCampaignFilterOnUserNameAndDueDateRange")
//    public ResponseEntity<Map<String,Object>> getCampaignFilterOnUserNameAndDueDateRange(String userName,
//                                                                                       @RequestParam Long startDate,
//                                                                                       @RequestParam Long endDate)
//    {
//        List<CampaignDTO> campaignList = campaignService.getCampaignFilterOnTaskIdAndDueDateRange(userName,startDate,endDate);
//        Map<String, Object> responseBody = new HashMap<>();
//        responseBody.put("campaignList", campaignList);
//        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "got campaign list.", responseBody);
//    }
@GetMapping("/getCampaignFilterOnUserNameAndDueDateRange")
public ResponseEntity<Map<String, Object>> getCampaignFilterOnUserNameAndDueDateRange(
        @RequestParam String userName,
        @RequestParam Long startDate,
        @RequestParam Long endDate,
        @RequestParam int todayYear,
        @RequestParam int todayMonth,
        @RequestParam int todayDay) {

    List<CampaignDTO> campaignList = campaignService.getCampaignsWithTasksInRange(
            userName, startDate, endDate, todayYear, todayMonth, todayDay);

    Map<String, Object> responseBody = new HashMap<>();
    responseBody.put("campaignList", campaignList);

    return buildResponse(HttpStatus.OK, "SUCCESS", "Got campaign list.", responseBody);
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
