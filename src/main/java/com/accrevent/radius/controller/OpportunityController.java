package com.accrevent.radius.controller;

import com.accrevent.radius.dto.*;
import com.accrevent.radius.model.Lifecycle;
import com.accrevent.radius.model.Opportunity;
import com.accrevent.radius.model.UserLeadSequence;
import com.accrevent.radius.model.UserOpportunitySequence;
import com.accrevent.radius.repository.OpportunityRepository;
import com.accrevent.radius.service.OpportunityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/opportunity")

public class OpportunityController {
    private static final Logger logger = LoggerFactory.getLogger(OpportunityController.class);

    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "failure";
    private static final String OPPORTUNITY_ID_REQUIRED = "opportunity Id must be provided in the request body.";
    private static final String WORKSPACE_ID_REQUIRED = "Workspace Id must be provided in the request body.";
    private final OpportunityService opportunityService;
    private  final OpportunityRepository opportunityRepository;
    public OpportunityController(OpportunityService opportunityService, OpportunityRepository opportunityRepository)
    {
        this.opportunityService = opportunityService;
        this.opportunityRepository = opportunityRepository;
    }

    @GetMapping("/getAll")
    public ResponseEntity<Map<String, Object>> getAllOpportunity()
    {
        Map<String, Object> responseBody = new HashMap<>();
        try
        {
            List<Opportunity> opportunitylist = opportunityService.getAllOpportunity();
            if (opportunitylist.isEmpty()) {
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "No opportunity Exist");
            }
            responseBody.put("Opportunity", opportunityService.getAllOpportunity());
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "get all opportunity successfully.", responseBody);
        }
        catch(Exception e)
        {
            return buildResponse(HttpStatus.NOT_FOUND, ERROR_STATUS, e.getMessage());
        }
    }

//    @GetMapping("/getAll")
//    public ResponseEntity<Map<String, Object>> getAllOpportunity() {
//        Map<String, Object> responseBody = new HashMap<>();
//        try {
//            List<Opportunity> opportunitylist = opportunityService.getAllOpportunity();
//            Long opportunityCountExcludingClosed = opportunityService.getOpportunityCountExcludingClosedWonAndClosedLost();
//
//            if (opportunitylist.isEmpty()) {
//                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "No opportunity exists");
//            }
//            responseBody.put("Opportunity", opportunitylist);
//            responseBody.put("opportunityCountExcludingClosed", opportunityCountExcludingClosed); // Add the count to the response
//
//            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get all opportunities successfully.", responseBody);
//        } catch (Exception e) {
//            return buildResponse(HttpStatus.NOT_FOUND, ERROR_STATUS, e.getMessage());
//        }
//    }


    @GetMapping("/byOpportunityId")
    public ResponseEntity<Map<String,Object>> getOpportunityById(@RequestParam Long opportunityId)
    {
        if (opportunityId == null) {
            logger.error(OPPORTUNITY_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, OPPORTUNITY_ID_REQUIRED);
        }
        Optional<Opportunity> opportunity = opportunityService.getOpportunityById(opportunityId);
        if(opportunity.isPresent())
        {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("OpportunityDetail",opportunity.get());
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Opportunity Detail successfully .", responseBody);
        }else
        {
            logger.warn("Opportunity Detail with opportunity Id: {} not found", opportunityId);
            return buildResponse(HttpStatus.NOT_FOUND, ERROR_STATUS, "Opportunity Detail with the given opportunity Id not found.");
        }
    }
    @PutMapping("/add")
    public ResponseEntity<Map<String,Object>> createOpportunity(@RequestBody OpportunityDTO opportunityDTO)
    {
        try
        {
            if (opportunityDTO.getWorkspaceId() == null)
            {
                logger.error(WORKSPACE_ID_REQUIRED);
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, WORKSPACE_ID_REQUIRED);
            }

            OpportunityDTO createdOpportunity = opportunityService.createOpportunity(opportunityDTO);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("createdOpportunity", createdOpportunity);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Opportunity successfully created.", responseBody);
        }
        catch(Exception e)
        {
            return buildResponse(HttpStatus.BAD_REQUEST,ERROR_STATUS,e.getMessage());
        }
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String,Object>> updateOpportunity(@RequestBody OpportunityDTO opportunityDTO)
    {
        try
        {
            if (opportunityDTO.getWorkspaceId() == null)
            {
                logger.error(WORKSPACE_ID_REQUIRED);
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, WORKSPACE_ID_REQUIRED);
            }

            OpportunityDTO createdOpportunity = opportunityService.updateOpportunity(opportunityDTO);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("updatedOpportunity", createdOpportunity);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Opportunity successfully updated.", responseBody);
        }
        catch(Exception e)
        {
            return buildResponse(HttpStatus.BAD_REQUEST,ERROR_STATUS,e.getMessage());
        }
    }


    @PostMapping("/saveLastReadForOpportunity")
    public ResponseEntity<Map<String, Object>> saveLastReadForOpportunity(@RequestBody savelastOpportunityReadDTO requestDTO) {
        Map<String, Object> response = new HashMap<>();

        if (requestDTO.getUserName() == null || requestDTO.getUserName().trim().isEmpty()) {
            response.put("status", "error");
            response.put("message", "Username is required");
            return ResponseEntity.badRequest().body(response);
        }
        if ((requestDTO.getOpportunityId() == null) ) {
            response.put("status", "error");
            response.put("message", "OpportunityId is required");
            return ResponseEntity.badRequest().body(response);
        }
        if (requestDTO.getLastOpportunityViewed() == null) {
            response.put("status", "error");
            response.put("message", "LastOpportunityViewed is required");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            opportunityService.saveLastReadForOpportunity(requestDTO.getUserName(), requestDTO.getOpportunityId(), requestDTO.getLastOpportunityViewed());

            response.put("status", "success");
            response.put("message", "Opportunity last viewed data saved successfully!");
            response.put("data", requestDTO);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // If the Opportunity was not found
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response); // 400 Bad Request
        } catch (Exception e) {
            // For any other errors
            response.put("status", "error");
            response.put("message", "An unexpected error occurred while saving last viewed data. Please check all the required field-username,OpportunityId,LastOpportunityViewed should present");
            return ResponseEntity.status(500).body(response); // 500 Internal Server Error
        }
    }


    @DeleteMapping("/deleteById")
    public ResponseEntity<Map<String,Object>> deleteOpportunity(@RequestParam Long opportunityId){

        try {
            boolean isDeleted = opportunityService.deleteOpportunity(opportunityId);
            if (isDeleted) {
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, opportunityId + " Opportunity successfully deleted."); // 200 OK with response body
            } else {
                logger.warn("Failed to delete opportunity with ID: {}", opportunityId);
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_STATUS, "Failed to delete the Opportunity.");
            }
        }catch(Exception e)
        {
            logger.warn("Deleting purpose given Opportunity ID: {} not found", opportunityId);
            return buildResponse(HttpStatus.NOT_FOUND,ERROR_STATUS,"Opportunity not found."); // 404 Not Found with response body

        }
    }

    @PostMapping("/updateOpportunitySequence")
    public ResponseEntity<Map<String,Object>> updateOpportunitySequence(@RequestParam String userName,@RequestBody List<OpportunityDTO> opportunityDTOList)
    {
        if(userName == null)
        {
            logger.error("user name is required");
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, "user name is required");
        }
        List<UserOpportunitySequence> userOpportunitySequenceList = opportunityService.updateOpportunitySequence(userName,opportunityDTOList);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("userOpportunitySequence", userOpportunitySequenceList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "User Opportunity sequence updated successfully.", responseBody);
    }


//    @GetMapping("/getOpportunityByWorkspaceId")
//    public ResponseEntity<Map<String,Object>> getOpportunityByWorkspaceId(@RequestParam Long workspaceId)
//    {
//        if(workspaceId == null)
//        {
//            logger.error(WORKSPACE_ID_REQUIRED);
//            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, WORKSPACE_ID_REQUIRED);
//        }
//        List<OpportunityDTO> opportunityDTOS = opportunityService.getOpportunityByWorkspaceId(workspaceId);
//        if(opportunityDTOS.isEmpty())
//        {
//            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Opportunity does not exists.");
//        }
//        Map<String, Object> responseBody = new HashMap<>();
//        responseBody.put("opportunity", opportunityDTOS);
//        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get opportunity successfully.", responseBody);
//    }

    @GetMapping("/getOpportunityByWorkspaceId")
    public ResponseEntity<Map<String, Object>> getOpportunityByWorkspaceId(@RequestParam Long workspaceId) {
        if (workspaceId == null) {
            logger.error(WORKSPACE_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, WORKSPACE_ID_REQUIRED);
        }
        // Fetch full Opportunity entities to access lifecycles
        List<Opportunity> opportunities = opportunityRepository.findByWorkspace_WorkspaceId(workspaceId);

        List<OpportunityDTO> opportunityDTOS = opportunityService.getOpportunityByWorkspaceId(workspaceId);

        if(opportunityDTOS.isEmpty())
        {
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Opportunity does not exists.");
        }

        if (opportunities.isEmpty()) {

            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Opportunity does not exist.");

        }

        // Count only opportunities that do NOT have any lifecycle with "Closed Won" or "Closed Lost"
        long validCount = opportunities.stream()
                .filter(opportunity -> {
                    // Find the current (active) lifecycle
                    Lifecycle activeLifecycle = opportunity.getOpportunitylifecycleList().stream()
                            .filter(lc -> "active".equalsIgnoreCase(lc.getStatus()))
                            .findFirst()
                            .orElse(null);

                    if (activeLifecycle == null) {
                        return true; // No active lifecycle → include
                    }

                    String currentStage = activeLifecycle.getLifecycleName();
                    return currentStage != null &&
                            !currentStage.trim().equalsIgnoreCase("closed won") &&
                            !currentStage.trim().equalsIgnoreCase("closed lost");
                })
                .count();

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("activeOpportunityCount", validCount);
        responseBody.put("opportunity", opportunityDTOS);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get opportunity successfully.", responseBody);
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

    @GetMapping("/getOpportunityFilterOnUserNameAndDueDateRange")
    public ResponseEntity<Map<String,Object>> getOpportunityFilterOnUserNameAndDueDateRange(String userName,
                                                                                            @RequestParam Long startDate,
                                                                                            @RequestParam Long endDate,
                                                                                            @RequestParam int todayYear,
                                                                                            @RequestParam int todayMonth,
                                                                                            @RequestParam int todayDay)
    {
        List<OpportunityDTO> opportunityDTOList = opportunityService.getOpportunitiesWithTasksInRange(userName,startDate,endDate,todayYear, todayMonth, todayDay);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("opportunityList", opportunityDTOList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "got opportunity list.", responseBody);
    }

}
