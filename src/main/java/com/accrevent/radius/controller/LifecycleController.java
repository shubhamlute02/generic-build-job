package com.accrevent.radius.controller;

import com.accrevent.radius.dto.*;
import com.accrevent.radius.model.*;
import com.accrevent.radius.repository.CommentsRepository;
import com.accrevent.radius.repository.TaskRepository;
import com.accrevent.radius.repository.UserLeadViewRepository;
import com.accrevent.radius.repository.UserOpportunityViewRepository;
import com.accrevent.radius.service.*;
import com.accrevent.radius.util.LifecycleName;
import com.accrevent.radius.util.RadiusConstants;
import com.accrevent.radius.util.RadiusUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/lifecycle")

public class LifecycleController {
    private static final Logger logger = LoggerFactory.getLogger(LifecycleController.class);

    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "failure";
    private static final String LIFECYCLE_ID_REQUIRED = "lifecycle Id must be provided in the request body.";
    private static final String CAMPAIGN_ID_REQUIRED = "campaign Id must be provided in the request body.";
    private static final String OPPORTUNITY_ID_REQUIRED = "Opportunity Id must be provided in the request body.";
    private static final String LEAD_ID_REQUIRED = "Lead Id must be provided in the request body.";
    private static final String VERSION_ID_REQUIRED = "Version Id must be provided in the request body.";

    private final LifecycleService lifecycleService;
    private final BookmarkService bookmarkService;
    private final OpportunityService opportunityService;
    private final TaskRepository taskRepository;
    private final CommentsRepository commentsRepository;
    private  final CommentsService commentsService;
    private final LeadService leadService;
    private final UserLeadViewRepository userLeadViewRepository;
    private  final UserOpportunityViewRepository userOpportunityViewRepository;
    public LifecycleController(LifecycleService lifecycleService, BookmarkService bookmarkService, OpportunityService opportunityService, TaskRepository taskRepository, CommentsRepository commentsRepository, CommentsService commentsService, LeadService leadService, UserLeadViewRepository userLeadViewRepository, UserOpportunityViewRepository userOpportunityViewRepository)
    {
        this.lifecycleService = lifecycleService;
        this.bookmarkService = bookmarkService;
        this.opportunityService = opportunityService;
        this.taskRepository = taskRepository;
        this.commentsRepository = commentsRepository;
        this.commentsService = commentsService;
        this.leadService = leadService;
        this.userLeadViewRepository = userLeadViewRepository;
        this.userOpportunityViewRepository = userOpportunityViewRepository;
    }

    @GetMapping("/getAll")
    public ResponseEntity<Map<String, Object>> getAllLifecycle()
    {
        Map<String, Object> responseBody = new HashMap<>();
        try
        {
            List<Lifecycle> lifecyclelist = lifecycleService.getAllLifecycle();
            if (lifecyclelist.isEmpty()) {
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "No lifecycle Exist");
            }
            responseBody.put("Lifecycle", lifecycleService.getAllLifecycle());
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "get all lifecycle successfully.", responseBody);
        }
        catch(Exception e)
        {
            return buildResponse(HttpStatus.NOT_FOUND, ERROR_STATUS, e.getMessage());
        }
    }

    @GetMapping("/getByType")
    public ResponseEntity<Map<String, Object>> getLifecyclesByType(@RequestParam String type) {
        List<Lifecycle> lifecycles = lifecycleService.getLifecyclesByType(type);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("LifecycleList", lifecycles);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Fetched lifecycles by type", responseBody);
    }



    @PutMapping("/add")
    public ResponseEntity<Map<String,Object>> createLifecycle(@RequestBody LifecycleDTO lifecycleDTO)
    {
        try
        {
            LifecycleDTO createdLifecycle = lifecycleService.createLifecycle(lifecycleDTO);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("createdLifecycle", createdLifecycle);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Lifecycle successfully created.", responseBody);
        }
        catch(Exception e)
        {
            return buildResponse(HttpStatus.BAD_REQUEST,ERROR_STATUS,e.getMessage());
        }
    }


    @GetMapping("/byLifecycleId")
    public ResponseEntity<Map<String,Object>> getLifecycleById(@RequestParam Long lifecycleId)
    {
        if (lifecycleId == null) {
            logger.error(LIFECYCLE_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, LIFECYCLE_ID_REQUIRED);
        }
        Optional<Lifecycle>lifecycle = lifecycleService.getLifecycleById(lifecycleId);
        if(lifecycle.isPresent())
        {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("LifecycleDetail",lifecycle.get());
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Lifecycle Detail successfully .", responseBody);
        }else
        {
            logger.warn("Lifecycle Detail with Lifecycle Id: {} not found", lifecycleId);
            return buildResponse(HttpStatus.NOT_FOUND, ERROR_STATUS, "Lifecycle Detail with the given Lifecycle Id not found.");
        }

    }
    @GetMapping("/getLifecycleByCampaignId")
    public ResponseEntity<Map<String,Object>> getLifecycleByCampaignId(@RequestParam Long campaignId,
                                                                       @RequestParam(required = false) String type)
    {
        if (campaignId == null) {
            logger.error(CAMPAIGN_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, CAMPAIGN_ID_REQUIRED);
        }
        List<Lifecycle> lifecycleList = lifecycleService.getLifecycleByCampaignId(campaignId, type);
        if(lifecycleList.isEmpty())
        {
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Records does not exists");
        }
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("LifecycleDetail",lifecycleList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Lifecycle Detail successfully .", responseBody);
    }

    @GetMapping("/getLifecycleByLeadId")
    public ResponseEntity<Map<String,Object>> getLifecycleByLeadId(@RequestParam Long leadId)
    {
        if (leadId == null) {
            logger.error(LEAD_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, CAMPAIGN_ID_REQUIRED);
        }
        List<Lifecycle> lifecycleList = lifecycleService.getLifecycleByLeadId(leadId);
        if(lifecycleList.isEmpty())
        {
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Records does not exists");
        }
        // build lead path
        String leadPath = leadService.buildPathForLead(leadId);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("LifecycleDetail",lifecycleList);
        responseBody.put("LeadPath", leadPath);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Lifecycle Detail successfully .", responseBody);
    }


    @GetMapping("/getActiveLifecycleAndLatestTaskByLeadId")
    public ResponseEntity<Map<String, Object>> getActiveLifecycleByLeadId(@RequestParam Long leadId,
                                                                          @RequestParam String userName) {
        if (leadId == null) {
            logger.error(LEAD_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, CAMPAIGN_ID_REQUIRED);
        }

        List<Lifecycle> lifecycleList = lifecycleService.getLifecycleByLeadId(leadId);

        List<Lifecycle> activeLifecycles = lifecycleList.stream()
                .filter(lc -> "active".equalsIgnoreCase(lc.getStatus()))
                .collect(Collectors.toList());

        if (activeLifecycles.isEmpty()) {
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "No active lifecycle records exist.");
        }

        Task earliestDueTask = null;
        Long earliestDueDate = null;
        String associatedLifecycleName = null;

        for (Lifecycle lifecycle : activeLifecycles) {
            if (lifecycle.getTaskList() != null) {
                for (Task task : lifecycle.getTaskList()) {
                    if ("In Work".equalsIgnoreCase(task.getStatus()) && task.getDueDate() != null) {
                        if (earliestDueDate == null || task.getDueDate() < earliestDueDate) {
                            earliestDueTask = task;
                            earliestDueDate = task.getDueDate();
                            associatedLifecycleName = lifecycle.getLifecycleName();
                        }
                    }

                    if (task.getSubTasks() != null) {
                        for (Task sub : task.getSubTasks()) {
                            if ("In Work".equalsIgnoreCase(sub.getStatus()) && sub.getDueDate() != null) {
                                if (earliestDueDate == null || sub.getDueDate() < earliestDueDate) {
                                    earliestDueTask = sub;
                                    earliestDueDate = sub.getDueDate();
                                    associatedLifecycleName = lifecycle.getLifecycleName();
                                }
                            }
                        }
                    }
                }
            }
        }

        //correct for red dot(both manual and system comment for lead ,task comment as well)

        // =================== Red Dot (isLeadViewed) Logic =========================

        UserLeadView userView = userLeadViewRepository
                .findByUserNameAndLeadId(userName, leadId)
                .orElseGet(() -> {
                    UserLeadView newView = new UserLeadView();
                    newView.setUserName(userName);
                    newView.setLeadId(leadId);
                    newView.setLastLeadViewed(0L);
                    return userLeadViewRepository.save(newView);
                });

        String userFullName = RadiusUtil.getCurrentUsername(); // Full name for system comment check

        // Fetch latest system and manual comments on Lead
        List<Comments> leadComments = commentsRepository.findByLeadLeadIdOrderByCreatedOnDesc(leadId);
        Optional<Comments> latestSystemCommentOpt = leadComments.stream()
                .filter(c -> RadiusConstants.SYSTEM_USER.equals(c.getCreatedBy()))
                .findFirst();
        Optional<Comments> latestManualCommentOnLeadOpt = leadComments.stream()
                .filter(c -> !RadiusConstants.SYSTEM_USER.equals(c.getCreatedBy()))
                .findFirst();

        // Fetch latest manual comments on all Tasks under Lead
        List<Long> taskIdsUnderLead = taskRepository.findTaskIdsByLeadId(leadId);
        Optional<Comments> latestManualCommentOnTaskOpt = Optional.empty();
        if (!taskIdsUnderLead.isEmpty()) {
            latestManualCommentOnTaskOpt = commentsRepository
                    .findByTaskTaskIdInOrderByCreatedOnDesc(taskIdsUnderLead)
                    .stream()
                    .filter(c -> !RadiusConstants.SYSTEM_USER.equals(c.getCreatedBy()))
                    .findFirst();
        }

        // Fetch latest system comments on all Tasks under Lead
        Optional<Comments> latestSystemCommentOnTaskOpt = Optional.empty();
        if (!taskIdsUnderLead.isEmpty()) {
            latestSystemCommentOnTaskOpt = commentsRepository
                    .findByTaskTaskIdInOrderByCreatedOnDesc(taskIdsUnderLead)
                    .stream()
                    .filter(c -> RadiusConstants.SYSTEM_USER.equals(c.getCreatedBy()))
                    .findFirst();
        }

        // Determine the latest comment among system, manual on lead, manual on task
        List<Comments> candidates = new ArrayList<>();
        latestSystemCommentOpt.ifPresent(candidates::add);
        latestManualCommentOnLeadOpt.ifPresent(candidates::add);
        latestManualCommentOnTaskOpt.ifPresent(candidates::add);
        latestSystemCommentOnTaskOpt.ifPresent(candidates::add);

        Comments latestComment = candidates.stream()
                .max(Comparator.comparingLong(Comments::getCreatedOn))
                .orElse(null);

        boolean hasUnreadComment = false;
        if (latestComment != null) {
            boolean isCreatedByCurrentUser = false;

            if (RadiusConstants.SYSTEM_USER.equals(latestComment.getCreatedBy())) {
                String desc = latestComment.getCommentDescription();
                if (desc != null &&
                        (desc.contains(userFullName + " created")
                                ||desc.contains(userFullName + " marked")
                                ||desc.contains(userFullName + " archived")
                                ||desc.contains(userFullName + " promoted  ")
                                ||desc.contains(userFullName + " moved")
                                ||desc.contains(userFullName + " changed")
                                ||desc.contains(userFullName + " reassigned")
                                ||desc.contains(userFullName + " rescheduled")
                                || desc.contains(userFullName + " updated"))) {
                    isCreatedByCurrentUser = true;
                }
            } else {
                isCreatedByCurrentUser = latestComment.getCreatedBy().equals(userFullName);
            }

            logger.debug("Latest comment text: {}", latestComment.getCommentDescription());
            logger.debug("User full name: {}", userFullName);
            logger.debug("Is created by current user: {}", isCreatedByCurrentUser);

            if (!isCreatedByCurrentUser && latestComment.getCreatedOn() > userView.getLastLeadViewed()) {
                hasUnreadComment = true;
                logger.debug("Marking as unread for user: {}", userName);
            }
        }

        boolean isLeadViewed = !hasUnreadComment;

        if (!hasUnreadComment) {
            userView.setLastLeadViewed(System.currentTimeMillis());
            userLeadViewRepository.save(userView);
            logger.debug("Updating last viewed time for user: {}", userName);
        }
// ====================== End updated logic ==========================

        boolean isLeadBookmarked = bookmarkService.getBookmarkForLead(userName, leadId);
        logger.info("BOOKMARK DEBUG - User: {}, Lead: {}, Bookmarked: {}",
                userName, leadId, isLeadBookmarked);


        // 🔄 End isLeadViewed logic

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("LifecycleDetail", activeLifecycles);
        responseBody.put("LatestDueTask", earliestDueTask != null ? earliestDueTask : null);
        responseBody.put("latestDueDate", earliestDueTask != null ? earliestDueTask.getDueDate() : null);
        responseBody.put("lifecycleName", associatedLifecycleName);
        responseBody.put("isLeadBookmarked", isLeadBookmarked);


        return buildResponse(HttpStatus.OK, SUCCESS_STATUS,
                "Retrieved active lifecycle details and earliest due task successfully.", responseBody);
    }


    @GetMapping("/getLifecycleByOpportunityId")
    public ResponseEntity<Map<String,Object>> getLifecycleByOpportunityId(@RequestParam Long opportunityId)
    {
        if (opportunityId == null) {
            logger.error(OPPORTUNITY_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, OPPORTUNITY_ID_REQUIRED);
        }
        List<Lifecycle> lifecycleList = lifecycleService.getLifecycleByOpportunityId(opportunityId);
        if(lifecycleList.isEmpty())
        {
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Records does not exists");
        }
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("LifecycleDetail",lifecycleList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Lifecycle Detail successfully .", responseBody);
    }


    @GetMapping("/getLifecycleByVersionId")
    public ResponseEntity<Map<String,Object>> getLifecycleByVersionId(@RequestParam Long versionId)
    {
        if (versionId == null) {
            logger.error(VERSION_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, OPPORTUNITY_ID_REQUIRED);
        }
        List<Lifecycle> lifecycleList = lifecycleService.getLifecycleByVersionId(versionId);
        if(lifecycleList.isEmpty())
        {
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Records does not exists");
        }
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("LifecycleDetail",lifecycleList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Lifecycle Detail successfully .", responseBody);
    }



//arti code with last viewed
//    @GetMapping("/getActiveLifecycleAndLatestTaskByOpportunityId")
//    public ResponseEntity<Map<String, Object>> getActiveLifecycleAndLatestTaskByOpportunityId(
//            @RequestParam Long opportunityId, @RequestParam String userName) {
//        if (opportunityId == null) {
//            logger.error(OPPORTUNITY_ID_REQUIRED);
//            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, OPPORTUNITY_ID_REQUIRED);
//        }
//
//        List<Lifecycle> lifecycleList = lifecycleService.getLifecycleByOpportunityId(opportunityId);
//        List<Lifecycle> activeLifecycles = lifecycleList.stream()
//                .filter(lc -> "active".equalsIgnoreCase(lc.getStatus()))
//                .collect(Collectors.toList());
//
//        if (activeLifecycles.isEmpty()) {
//            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "No active lifecycle records exist.");
//        }
//
//        Long latestDueDate = null;
//        String latestLifecycleName = null;
//
//        for (Lifecycle lifecycle : activeLifecycles) {
//            if (lifecycle.getTaskList() != null) {
//                for (Task task : lifecycle.getTaskList()) {
//                    if (task.getDueDate() != null && (latestDueDate == null || task.getDueDate() < latestDueDate)) {
//                        latestDueDate = task.getDueDate();
//                        latestLifecycleName = lifecycle.getLifecycleName();
//                    }
//
//                    if (task.getSubTasks() != null) {
//                        for (Task subTask : task.getSubTasks()) {
//                            if (subTask.getDueDate() != null && (latestDueDate == null || subTask.getDueDate() < latestDueDate)) {
//                                latestDueDate = subTask.getDueDate();
//                                latestLifecycleName = lifecycle.getLifecycleName();
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        // Determine isOpportunityViewed by comparing comments' createdOn with lastOpportunityViewed
//        boolean isOpportunityViewed = true;
//
//        // 1. Get last viewed date
//        Optional<UserOpportunityView> userViewOpt = userOpportunityViewRepository
//                .findByUserNameAndOpportunityId(userName, opportunityId);
//
//        Long lastOpportunityViewed = userViewOpt.map(UserOpportunityView::getLastOpportunityViewed).orElse(0L);
//
//        // 2. Get all comments for this opportunity
//        List<CommentsDTO> commentsList = commentsService.getCommentsByOpportunityId(opportunityId);
//
//        // 3. Check if any comment is newer than lastOpportunityViewed
////        for (CommentsDTO comment : commentsList) {
////            if (comment.getCreatedOn() != null && comment.getCreatedOn() > lastOpportunityViewed) {
////
////                isOpportunityViewed = false;
////                break;
////            }
////        }
//        // 3. Check if any comment is newer than lastOpportunityViewed
//        for (CommentsDTO comment : commentsList) {
//            if (comment.getCreatedOn() != null) {
//                // ✅ Log the values before comparison
//                logger.info("Comparing comment createdOn: {} with lastOpportunityViewed: {}",
//                        comment.getCreatedOn(), lastOpportunityViewed);
//
//                if (comment.getCreatedOn() > lastOpportunityViewed) {
//                    isOpportunityViewed = false;
//                    break;
//                }
//            }
//        }
//
//
//        Map<String, Object> response = new HashMap<>();
//        response.put("lifecycleName", latestLifecycleName);
//        response.put("latestDueDate", latestDueDate);
//        response.put("isOpportunityViewed", isOpportunityViewed);
//
//        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Retrieved active lifecycle and latest due date successfully.", response);
//    }

//arti correct code without last viewed with subtask
//@GetMapping("/getActiveLifecycleAndLatestTaskByOpportunityId")
//public ResponseEntity<Map<String, Object>> getActiveLifecycleAndLatestTaskByOpportunityId(@RequestParam Long opportunityId) {
//    if (opportunityId == null) {
//        logger.error(OPPORTUNITY_ID_REQUIRED);
//        return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, OPPORTUNITY_ID_REQUIRED);
//    }
//
//    List<Lifecycle> lifecycleList = lifecycleService.getLifecycleByOpportunityId(opportunityId);
//
//    // Filter for active lifecycles
//    List<Lifecycle> activeLifecycles = lifecycleList.stream()
//            .filter(lc -> "active".equalsIgnoreCase(lc.getStatus()))
//            .collect(Collectors.toList());
//
//    if (activeLifecycles.isEmpty()) {
//        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "No active lifecycle records exist.");
//    }
//
//    Task latestTask = null;
//    Long earliestDueDate = null;
//    String latestLifecycleName = null;
//
//    for (Lifecycle lifecycle : activeLifecycles) {
//        if (lifecycle.getTaskList() != null) {
//            for (Task task : lifecycle.getTaskList()) {
//                if ("In Work".equalsIgnoreCase(task.getStatus()) && task.getDueDate() != null) {
//                    if (earliestDueDate == null || task.getDueDate() < earliestDueDate) {
//                        latestTask = task;
//                        earliestDueDate = task.getDueDate();
//                        latestLifecycleName = lifecycle.getLifecycleName();
//                    }
//                }
//
//                // Check subtasks
//                if (task.getSubTasks() != null) {
//                    for (Task sub : task.getSubTasks()) {
//                        if ("In Work".equalsIgnoreCase(sub.getStatus()) && sub.getDueDate() != null) {
//                            if (earliestDueDate == null || sub.getDueDate() < earliestDueDate) {
//                                latestTask = sub;
//                                earliestDueDate = sub.getDueDate();
//                                latestLifecycleName = lifecycle.getLifecycleName();
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    Map<String, Object> response = new HashMap<>();
//    response.put("LifecycleDetail", activeLifecycles);
//    response.put("LatestDueTask", latestTask != null ? latestTask : null);
//    response.put("lifecycleName", latestLifecycleName);
//    response.put("latestDueDate", latestTask != null ? latestTask.getDueDate() : null);
//
//    return buildResponse(HttpStatus.OK, SUCCESS_STATUS,
//            "Retrieved active lifecycle and latest due task successfully.", response);
//}


@GetMapping("/getActiveLifecycleAndLatestTaskByOpportunityId")
public ResponseEntity<Map<String, Object>> getActiveLifecycleAndLatestTaskByOpportunityId(
        @RequestParam Long opportunityId,
        @RequestParam String userName) {

    if (opportunityId == null) {
        logger.error(OPPORTUNITY_ID_REQUIRED);
        return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, OPPORTUNITY_ID_REQUIRED);
    }

    List<Lifecycle> lifecycleList = lifecycleService.getLifecycleByOpportunityId(opportunityId);

    // Filter for active lifecycles
    List<Lifecycle> activeLifecycles = lifecycleList.stream()
            .filter(lc -> "active".equalsIgnoreCase(lc.getStatus()))
            .collect(Collectors.toList());

    if (activeLifecycles.isEmpty()) {
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "No active lifecycle records exist.");
    }

    Task latestTask = null;
    Long earliestDueDate = null;
    String latestLifecycleName = null;

    for (Lifecycle lifecycle : activeLifecycles) {
        if (lifecycle.getTaskList() != null) {
            for (Task task : lifecycle.getTaskList()) {
                if ("In Work".equalsIgnoreCase(task.getStatus()) && task.getDueDate() != null) {
                    if (earliestDueDate == null || task.getDueDate() < earliestDueDate) {
                        latestTask = task;
                        earliestDueDate = task.getDueDate();
                        latestLifecycleName = lifecycle.getLifecycleName();
                    }
                }

                // Check subtasks
                if (task.getSubTasks() != null) {
                    for (Task sub : task.getSubTasks()) {
                        if ("In Work".equalsIgnoreCase(sub.getStatus()) && sub.getDueDate() != null) {
                            if (earliestDueDate == null || sub.getDueDate() < earliestDueDate) {
                                latestTask = sub;
                                earliestDueDate = sub.getDueDate();
                                latestLifecycleName = lifecycle.getLifecycleName();
                            }
                        }
                    }
                }
            }
        }
    }

    boolean isOpportunityBookmarked = bookmarkService.getBookmarkForOpportunity(userName, opportunityId);
    logger.info("BOOKMARK DEBUG - User: {}, Opportunity: {}, Bookmarked: {}",
            userName, opportunityId, isOpportunityBookmarked);


    // 🔄 Determine isOpportunityViewed
    boolean isOpportunityViewed = true;

    Optional<UserOpportunityView> userViewOpt =
            userOpportunityViewRepository.findByUserNameAndOpportunityId(userName, opportunityId);
    Long lastOpportunityViewed = userViewOpt.map(UserOpportunityView::getLastOpportunityViewed).orElse(0L);

    List<CommentsDTO> commentsList = commentsService.getCommentsByOpportunityId(opportunityId);
    for (CommentsDTO comment : commentsList) {
        if (comment.getCreatedOn() != null) {
            System.out.println("Comparing comment createdOn: " + new Date(comment.getCreatedOn())
                    + " with lastOpportunityViewed: " + new Date(lastOpportunityViewed));

            if (comment.getCreatedOn() > lastOpportunityViewed) {
                isOpportunityViewed = false;
                break;
            }
        }

    }
    // 🔄 End isOpportunityViewed logic


    Map<String, Object> response = new HashMap<>();
    response.put("LifecycleDetail", activeLifecycles);
    response.put("LatestDueTask", latestTask != null ? latestTask : null);
    response.put("lifecycleName", latestLifecycleName);
    response.put("latestDueDate", latestTask != null ? latestTask.getDueDate() : null);
    response.put("isOpportunityViewed", isOpportunityViewed); // ✅ Add to response
    response.put("isOpportunityBookmarked", isOpportunityBookmarked);

    return buildResponse(HttpStatus.OK, SUCCESS_STATUS,
            "Retrieved active lifecycle and latest due task successfully.", response);
}




    @DeleteMapping("/deleteById")
    public ResponseEntity<Map<String,Object>> deleteLifecycle(@RequestParam Long lifecycleId){

        try {
            boolean isDeleted = lifecycleService.deleteLifecycle(lifecycleId);
            if (isDeleted) {
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, lifecycleId + " Lifecycle successfully deleted."); // 200 OK with response body
            } else {
                logger.warn("Failed to delete lifecycle with ID: {}", lifecycleId);
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_STATUS, "Failed to delete the Lifecycle.");
            }
        }catch(Exception e)
        {
            logger.warn("Deleting purpose given Lifecycle ID: {} not found", lifecycleId);
            return buildResponse(HttpStatus.NOT_FOUND,ERROR_STATUS,"Lifecycle not found."); // 404 Not Found with response body

        }
    }

    @PutMapping("/updateLifeCycleStatus")
    public ResponseEntity<Map<String,Object>> updateLifeCycleStatus(@RequestParam Long lifeCycleId,@RequestParam String status,@RequestParam String previouseLifecycle){
        if(lifeCycleId == null){
            logger.error("LifeCycle Id is required");
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, "LifeCycle Id is required");
        }
        Map<String,Object> responseBody = new HashMap<>();
        responseBody.put("Message",lifecycleService.updateLifeCycleStatus(lifeCycleId,status,previouseLifecycle));
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Status of lifeCycle updated successfully.", responseBody);

    }

    @PostMapping("/promoteMarketingContentVersion")
    public ResponseEntity<?> promoteMarketingContentVersion(
            @RequestParam Long versionId,
            @RequestParam String newLifecycle) {

        try {
            LifecycleStatusUpdateDTO result = lifecycleService.promoteMarketingContentVersion(versionId, newLifecycle);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error promoting version: " + e.getMessage());
        }
    }

    @GetMapping("/getLeadPipelineReport")
    public ResponseEntity<Map<String,Object>> getLeadPipelineReport (
            @RequestParam(required = false) Long campaignId)
    {
        Map<String,Object> responseBody = new HashMap<>();
        responseBody.put("Message",lifecycleService.getLeadPipelineReport(campaignId));
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, " get Lead Pipeline report successfully.", responseBody);

    }

    @GetMapping("/getLeadsForPipelineByBUandLCStatus")
    public ResponseEntity<Map<String,Object>> getLeadsForPipelineByBUandLCStatus(@RequestParam String businessUnit, @RequestParam String lifecycleStatus)
    {
        List<LeadDTO> leadList;
        try
        {
            leadList = lifecycleService.getLeadsForPipelineByBUandLCStatus(businessUnit, lifecycleStatus);
        }
        catch(Exception e)
        {
            leadList = new ArrayList();
            e.printStackTrace();
        }

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("LeadDetail", leadList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Lead Detail successfully .", responseBody);
    }

//    @GetMapping("/getOpportunitiesForPipelineByBUandLCStatus")
//    public ResponseEntity<Map<String, Object>> getOpportunitiesForPipelineByBUandLCStatus(
//            @RequestParam String businessUnit,
//            @RequestParam String lifecycleStatus) {
//
//        List<Opportunity> opportunityList;
//        try {
//            opportunityList = lifecycleService.getOpportunitiesForPipelineByBUandLCStatus(businessUnit, lifecycleStatus);
//        } catch (Exception e) {
//            opportunityList = new ArrayList<>();
//            e.printStackTrace();
//        }
//
//        Map<String, Object> responseBody = new HashMap<>();
//        responseBody.put("OpportunityDetail", opportunityList);
//        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Opportunity Detail successfully.", responseBody);
//    }

    @GetMapping("/getOpportunitiesForPipelineByBUandLCStatus")
    public ResponseEntity<Map<String, Object>> getOpportunitiesForPipelineByBUandLCStatus(
            @RequestParam String businessUnit,
            @RequestParam String lifecycleStatus) {

        List<Opportunity> opportunityList;
        try {
            opportunityList = lifecycleService.getOpportunitiesForPipelineByBUandLCStatus(businessUnit, lifecycleStatus);
        } catch (Exception e) {
            opportunityList = new ArrayList<>();
            e.printStackTrace();
        }

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("OpportunityDetail", opportunityList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Opportunity Detail successfully.", responseBody);
    }


    @GetMapping("/getCountForPipelineByBU")
    public ResponseEntity<Map<String,Object>> getCountForPipelineByBU(@RequestParam String businessUnit) throws Exception
    {
        SalesPipelineCountDTO dto = new SalesPipelineCountDTO();
        // Identified
        dto.setIdentified(lifecycleService.getLeadCount(businessUnit, LifecycleName.IDENTIFIED));

        // Research
        dto.setResearch(lifecycleService.getLeadCount(businessUnit, LifecycleName.RESEARCH));

        // Prospecting
        dto.setProspecting(lifecycleService.getLeadCount(businessUnit, LifecycleName.PROSPECTING));

        // Opportunity Lifecycle Stages
        dto.setDiscovery(lifecycleService.getOpportunityCount(businessUnit, LifecycleName.DISCOVERY));
        dto.setProposal(lifecycleService.getOpportunityCount(businessUnit, LifecycleName.PROPOSAL));
        dto.setCustomerEvaluating(lifecycleService.getOpportunityCount(businessUnit, LifecycleName.CUSTOMER_EVALUATING));
        dto.setClosedWon(lifecycleService.getOpportunityCount(businessUnit, LifecycleName.CLOSED_WON));
        dto.setClosedLost(lifecycleService.getOpportunityCount(businessUnit, LifecycleName.CLOSED_LOST));

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("PipelineCount", dto);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the pipeline count successfully .", responseBody);
    }

    @GetMapping("/getOpportunityPipelineReport")
    public ResponseEntity<Map<String,Object>> getOpportunityPipelineReport (
            @RequestParam(required = false) Long workspaceID, @RequestParam(required = false) Long opportunityId,
            @RequestParam(required = false) String startDate,@RequestParam(required = false) String endDate)
    {
        Map<String,Object> responseBody = new HashMap<>();
        responseBody.put("Message",lifecycleService.getOpportunityPipelineReport(workspaceID,opportunityId,startDate,endDate));
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, " get Opportunity Pipeline report successfully.", responseBody);

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
