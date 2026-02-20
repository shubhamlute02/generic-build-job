package com.accrevent.radius.controller;


import com.accrevent.radius.dto.WorkspaceDTO;
import com.accrevent.radius.model.UserWorkspaceSequence;
import com.accrevent.radius.service.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workspace")

public class WorkspaceController {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceController.class);

    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "failure";
   
    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService)
    {
        this.workspaceService = workspaceService;
    }

    @GetMapping("/getAll")
    public ResponseEntity<Map<String, Object>> getAllWorkspace(@RequestParam String userName)
    {
        Map<String, Object> responseBody = new HashMap<>();
        try
        {
            List<WorkspaceDTO> workspacelist = workspaceService.getAllWorkspace(userName);
            if (workspacelist.isEmpty()) {
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "No workspace Exist");
            }
            responseBody.put("Workspace", workspacelist);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "get all workspace successfully.", responseBody);
        }
        catch(Exception e)
        {
            logger.debug("Exception occurred while fetching workspaces for user: {}", userName, e);
            return buildResponse(HttpStatus.NOT_FOUND, ERROR_STATUS, e.getMessage());
        }
    }


    @PutMapping("/add")
    public ResponseEntity<Map<String,Object>> createWorkspace(@RequestBody WorkspaceDTO workspaceDTO)
    {
        try
        {
            WorkspaceDTO createdWorkspace = workspaceService.createWorkspace(workspaceDTO);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("createdWorkspace", createdWorkspace);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Workspace successfully created.", responseBody);
        }
        catch(Exception e)
        {
            return buildResponse(HttpStatus.BAD_REQUEST,ERROR_STATUS,e.getMessage());
        }
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<Map<String,Object>> deleteWorkspace(@RequestParam Long workspaceId){

        try {
            boolean isDeleted = workspaceService.deleteWorkspace(workspaceId);
            if (isDeleted) {
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, workspaceId + " Workspace successfully deleted."); // 200 OK with response body
            } else {
                logger.warn("Failed to delete workspace with ID: {}", workspaceId);
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_STATUS, "Failed to delete the Workspace.");
            }
        }catch(Exception e)
        {
            logger.warn("Deleting purpose given Workspace ID: {} not found", workspaceId);
            return buildResponse(HttpStatus.NOT_FOUND,ERROR_STATUS,"Workspace not found."); // 404 Not Found with response body

        }
    }

    @PostMapping("/updateWorkspaceSequence")
    public ResponseEntity<Map<String,Object>> updateWorkspaceSequence(@RequestParam String userName,@RequestBody List<WorkspaceDTO> campaignDTOs)
    {
        if(userName == null)
        {
            logger.error("user name is required");
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, "user name is required");
        }
        List<UserWorkspaceSequence> userWorkspaceSequenceList = workspaceService.updateWorkspaceSequence(userName,campaignDTOs);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("userWorkspaceSequence", userWorkspaceSequenceList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "User workspace sequence updated successfully.", responseBody);
    }

    @PutMapping("/updateNameAndDescriptionAndOwnerByWorkspaceId")
    public ResponseEntity<Map<String,Object>> updateNameAndDescriptionAndOwnerByWorkspaceId(@RequestParam Long workspaceId,@RequestParam String workspaceName,@RequestParam String description,@RequestParam String owner){
       try {
           if (workspaceId == null) {
               logger.error("Workspace Id is required");
               return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, "Workspace Id is required");
           }
           Map<String, Object> responseBody = new HashMap<>();
           responseBody.put("Message", workspaceService.updateNameAndDescriptionAndOwnerByWorkspaceId(workspaceId, workspaceName, description, owner));
           return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Workspace updated successfully.", responseBody);
       }catch(Exception e){
           logger.warn("Given Workspace ID: {} not found", workspaceId);
           return buildResponse(HttpStatus.NOT_FOUND,ERROR_STATUS,e.getMessage()); // 404 Not Found with response body
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
