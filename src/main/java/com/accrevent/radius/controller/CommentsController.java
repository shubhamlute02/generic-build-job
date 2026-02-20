package com.accrevent.radius.controller;

import com.accrevent.radius.dto.CommentsDTO;
import com.accrevent.radius.model.Comments;
import com.accrevent.radius.service.CommentsService;
import com.accrevent.radius.service.OpportunityService;
import jakarta.persistence.EntityNotFoundException;
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
@RequestMapping("/comments")

public class  CommentsController {
    private static final Logger logger = LoggerFactory.getLogger(CommentsController.class);

    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "failure";
    private static final String COMMENTS_ID_REQUIRED = "comments Id must be provided in the request body.";
    private static final String CAMPAIGN_ID_REQUIRED = "campaign Id must be provided in the request body.";
    private static final String OPPORTUNITY_ID_REQUIRED = "Opportunity Id must be provided in the request body.";
    private static final String LEAD_ID_REQUIRED = "Lead Id must be provided in the request body.";
    private static final String RECORDS_NOT_EXIST ="Records does not exists";
    private static final String VERSION_ID_REQUIRED = "Version Id must be provided in the request body.";
    private static final String TASK_ID_REQUIRED = "Task Id must be provided in the request body.";
    private final CommentsService commentsService;
    private final OpportunityService opportunityService;
    public CommentsController(CommentsService commentsService, OpportunityService opportunityService)
    {
        this.commentsService = commentsService;
        this.opportunityService = opportunityService;
    }

    @PutMapping("/addCampaignComment")
    public ResponseEntity<?> addCampaignComment(@RequestBody CommentsDTO commentsDTO) {
        try{
            CommentsDTO created = commentsService.createCampaignComment(commentsDTO);
            return ResponseEntity.ok(created);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @PutMapping("/addOpportunityComment")
    public ResponseEntity<?> addOpportunityComment(@RequestBody CommentsDTO commentsDTO) {
        try{
            CommentsDTO created = commentsService.createOpportunityComment(commentsDTO);
            return ResponseEntity.ok(created);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @PutMapping("/addLeadComment")
    public ResponseEntity<?> addLeadComment(@RequestBody CommentsDTO commentsDTO) {
        try{
            CommentsDTO created = commentsService.createLeadComment(commentsDTO);
            return ResponseEntity.ok(created);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @PutMapping("/addTaskComment")
    public ResponseEntity<?> addTaskComment(@RequestBody CommentsDTO commentsDTO) {
       try{
           CommentsDTO created = commentsService.createTaskComment(commentsDTO);
           return ResponseEntity.ok(created);
       } catch (EntityNotFoundException e) {
           return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
       } catch (Exception e) {
           return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred."));
       }
    }

    @PutMapping("/addVersionComment")
    public ResponseEntity<?> addVersionComment(@RequestBody CommentsDTO commentsDTO) {
        try{
            CommentsDTO created = commentsService.createVersionComment(commentsDTO);
            return ResponseEntity.ok(created);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred."));
        }
    }


    @GetMapping("/byCommentsId")
    public ResponseEntity<Map<String,Object>> getCommentsById(@RequestParam Long commentsId)
    {
        if (commentsId == null) {
            logger.error(COMMENTS_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, COMMENTS_ID_REQUIRED);
        }
        Optional<Comments>comments = commentsService.getCommentsById(commentsId);
        if(comments.isPresent())
        {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("CommentsDetail",comments.get());
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Comments Detail successfully.", responseBody);
        }else
        {
            logger.warn("Comments Detail with Comments Id: {} not found", commentsId);
            return buildResponse(HttpStatus.NOT_FOUND, ERROR_STATUS, "Comments Detail with the given Comments Id not found.");
        }

    }
    @GetMapping("/getCommentsByCampaignId")
    public ResponseEntity<Map<String,Object>> getCommentsByCampaignId(@RequestParam Long campaignId)
    {
        if (campaignId == null) {
            logger.error(CAMPAIGN_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, CAMPAIGN_ID_REQUIRED);
        }
        List<CommentsDTO> commentsList = commentsService.getCommentsByCampaignId(campaignId);
        if(commentsList.isEmpty())
        {
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, RECORDS_NOT_EXIST);
        }
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("CommentsDetail",commentsList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Comments Detail successfully.", responseBody);
    }

    @GetMapping("/getCommentsByLeadId")
    public ResponseEntity<Map<String,Object>> getCommentsByLeadId(@RequestParam Long leadId)
    {
        if (leadId == null) {
            logger.error(LEAD_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, CAMPAIGN_ID_REQUIRED);
        }
        List<CommentsDTO> commentsList = commentsService.getCommentsByLeadIdAndTasks(leadId);
        if(commentsList.isEmpty())
        {
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, RECORDS_NOT_EXIST);
        }
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("CommentsDetail",commentsList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Comments Detail successfully .", responseBody);
    }

    @GetMapping("/getCommentsByOpportunityId")
    public ResponseEntity<Map<String,Object>> getCommentsByOpportunityId(@RequestParam Long opportunityId)
    {
        if (opportunityId == null) {
            logger.error(OPPORTUNITY_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, OPPORTUNITY_ID_REQUIRED);
        }
        List<CommentsDTO> commentsList = commentsService.getCommentsByOpportunityId(opportunityId);
        if(commentsList.isEmpty())
        {
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, RECORDS_NOT_EXIST);
        }
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("CommentsDetail",commentsList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Comments Detail successfully .", responseBody);
    }

    @GetMapping("/getCommentsByTaskId")
    public ResponseEntity<Map<String,Object>> getCommentsByTaskId(@RequestParam Long taskId)
    {
        if (taskId == null) {
            logger.error(TASK_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, TASK_ID_REQUIRED);
        }
        List<CommentsDTO> commentsList = commentsService.getCommentsByTaskId(taskId);
        if(commentsList.isEmpty())
        {
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, RECORDS_NOT_EXIST);
        }
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("CommentsDetail",commentsList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Got the Comments Detail successfully .", responseBody);
    }

    @GetMapping("/getCommentsByVersionId")
    public ResponseEntity<Map<String,Object>> getCommentsByVersionId(@RequestParam Long versionId) {
        if (versionId == null) {
            logger.error(VERSION_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, VERSION_ID_REQUIRED);
        }
        List<CommentsDTO> commentsList = commentsService.getCommentsByVersionId(versionId);
        if (commentsList.isEmpty()) {
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, RECORDS_NOT_EXIST);
        }
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("CommentsDetail", commentsList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Got the Comments Detail successfully .", responseBody);
    }


    @DeleteMapping("/deleteById")
    public ResponseEntity<Map<String,Object>> deleteComments(@RequestParam Long commentsId){

        try {
            boolean isDeleted = commentsService.deleteComments(commentsId);
            if (isDeleted) {
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, commentsId + " Comments successfully deleted."); // 200 OK with response body
            } else {
                logger.warn("Failed to delete comments with ID: {}", commentsId);
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_STATUS, "Failed to delete the Comments.");
            }
        }catch(Exception e)
        {
            logger.warn("Deleting purpose given Comments ID: {} not found", commentsId);
            return buildResponse(HttpStatus.NOT_FOUND,ERROR_STATUS,"Comments not found."); // 404 Not Found with response body

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
