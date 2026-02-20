package com.accrevent.radius.controller;

import com.accrevent.radius.dto.*;
import com.accrevent.radius.model.*;
import com.accrevent.radius.repository.UserRegionRepository;
import com.accrevent.radius.service.TaskService;
import com.accrevent.radius.util.TaskType;
import jakarta.persistence.DiscriminatorValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/task")

public class TaskController {
    private static final Logger logger = LoggerFactory.getLogger(TaskController.class);

    private static final String SUCCESS_STATUS = "success";
    private static final String ERROR_STATUS = "failure";
    private static final String TASK_ID_REQUIRED = "task Id must be provided in the request body.";
    private static final String LIFECYCLE_ID_REQUIRED = "lifecycle Id must be provided in the request body.";
    private static final String LIFECYCLE_PARENT_TASK_ID_REQUIRED = "lifecycle Id or parentTask Id must be provided in the request body.";
    private final TaskService taskService;
    private final UserRegionRepository userRegionRepository;
    public TaskController(TaskService taskService, UserRegionRepository userRegionRepository)
    {
        this.taskService = taskService;
        this.userRegionRepository = userRegionRepository;
    }

        @GetMapping("/getAll")
        public ResponseEntity<Map<String, Object>> getAllTask()
        {
            Map<String, Object> responseBody = new HashMap<>();
            try
            {
                List<Task> tasklist = taskService.getAllTask();
                if (tasklist.isEmpty()) {
                    return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "No task Exist");
                }

                List<Map<String, Object>> taskWithOptionalContact = new ArrayList<>();

                for (Task task : tasklist) {
                    Map<String, Object> taskMap = new HashMap<>();
                    taskMap.put("task", task);

                    // 🔍 Add task type using DiscriminatorValue
                    DiscriminatorValue dv = task.getClass().getAnnotation(DiscriminatorValue.class);
                    String taskType = dv != null ? dv.value() : "DEFAULT";
                    taskMap.put("type", taskType);
                    taskWithOptionalContact.add(taskMap);
                }

                responseBody.put("Task", taskWithOptionalContact);
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get all tasks successfully.", responseBody);
            }
            catch(Exception e)
            {
                return buildResponse(HttpStatus.NOT_FOUND, ERROR_STATUS, e.getMessage());
            }
        }

    @GetMapping("/getEmailOutreachTask")
    public ResponseEntity<List<TaskDTO>> getAllOutreachTasks() {
        List<TaskDTO> outreachTasks = taskService.getEmailOutreachTasks();
        return ResponseEntity.ok(outreachTasks);
    }

    @GetMapping("/getTaskByLifecycleId")
    public ResponseEntity<Map<String,Object>> getTaskByLifecycleId(@RequestParam Long lifecycleId,@RequestParam String userName)
    {

        if (lifecycleId == null) {
            logger.error(LIFECYCLE_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, LIFECYCLE_ID_REQUIRED);
        }
        List<Task> taskList = taskService.getTaskByLifecycleId(userName,lifecycleId);
        if(taskList.isEmpty())
        {
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Records does not exists");
        }

        List<Map<String, Object>> taskWithOptionalContact = new ArrayList<>();

        for (Task task : taskList) {
            Map<String, Object> taskMap = new HashMap<>();
            //taskMap.put("task", task);

            if(task instanceof EmailOutreachTask) {
                taskMap.put("task", taskService.transformToEmailOutreachTaskDTO((EmailOutreachTask) task));
            }
            else if(task instanceof PhoneOutreachTask) {
                taskMap.put("task", taskService.transformToPhoneOutreachTaskDTO((PhoneOutreachTask) task));
            }
            else if(task instanceof LinkedInOutreachTask) {
                taskMap.put("task", taskService.transformToLinkedInOutreachTaskDTO((LinkedInOutreachTask) task));
            }
            else {
                taskMap.put("task", taskService.transformToTaskDTO(task));
            }

            taskWithOptionalContact.add(taskMap);
        }

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("TaskDetail", taskWithOptionalContact);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Task Detail successfully .", responseBody);
    }

    // For regular tasks
    @PutMapping("/add")
    public ResponseEntity<Map<String, Object>> createTask(@RequestBody TaskDTO taskDTO) {
        try {
            if(taskDTO.getLifecycleId() == null && taskDTO.getParentTaskId() == null) {
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, LIFECYCLE_PARENT_TASK_ID_REQUIRED);
            }

            // Ensure type is not outreach for this endpoint
            if(TaskType.EMAIL_OUTREACH_TASK.equalsIgnoreCase(taskDTO.getType())) {
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS,
                        "Use /addOutreach endpoint for outreach tasks");
            }

            TaskDTO createdTask = taskService.createTask(taskDTO);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("createdTask", createdTask);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Task created successfully", responseBody);
        } catch(Exception e) {
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, e.getMessage());
        }
    }

    // For outreach tasks
    @PutMapping("/addEmailOutreachTask")
    public ResponseEntity<Map<String, Object>> createOutreachTask(@RequestBody EmailOutreachTaskDTO emailOutreachTaskDTO) {
        try {
            if(emailOutreachTaskDTO.getLifecycleId() == null && emailOutreachTaskDTO.getParentTaskId() == null) {
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, LIFECYCLE_PARENT_TASK_ID_REQUIRED);
            }
            if(emailOutreachTaskDTO.getContactId() == null) {
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, "Contact ID is required");
            }

            EmailOutreachTaskDTO createdTask = taskService.createEmailOutreachTask(emailOutreachTaskDTO);
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("createdTask", createdTask);
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Outreach task created successfully", responseBody);
        } catch(Exception e) {
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, e.getMessage());
        }
    }

    @PutMapping("/update")
    public ResponseEntity<Map<String,Object>> updateTask(@RequestBody TaskDTO taskDTO) {
        try {
            if(taskDTO.getLifecycleId() == null && taskDTO.getParentTaskId() == null) {
                logger.error(LIFECYCLE_PARENT_TASK_ID_REQUIRED);
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, LIFECYCLE_PARENT_TASK_ID_REQUIRED);
            }
            TaskDTO updatedTask = taskService.updateTask(taskDTO);

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("createdTask", updatedTask);
//            responseBody.put("isSystemComment", updatedTask.isSystemComment());

            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Task successfully updated.", responseBody);
        } catch(Exception e) {
            e.printStackTrace();
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, e.getMessage());
        }
    }

    @PutMapping("/updateLinkedInOutreachTask")
    public ResponseEntity<Map<String, Object>> updateLinkedInOutreachTask(@RequestBody LinkedInOutreachTaskDTO linkedInOutreachTaskDTO) {
        try {

            // Validate required fields
            if (linkedInOutreachTaskDTO.getLifecycleId() == null && linkedInOutreachTaskDTO.getParentTaskId() == null) {
                logger.error(LIFECYCLE_PARENT_TASK_ID_REQUIRED);
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, LIFECYCLE_PARENT_TASK_ID_REQUIRED);
            }

            // Validate task type
            if (!TaskType.LINKEDIN_OUTREACH_TASK.equalsIgnoreCase(linkedInOutreachTaskDTO.getType())) {
                String errorMsg = "Task type must be LinkedIn Outreach";
                logger.error(errorMsg);
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, errorMsg);
            }

            // Process the update
            LinkedInOutreachTaskDTO updatedTask = taskService.updateLinkedInOutreachTask(linkedInOutreachTaskDTO);

            // Build response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("updatedTask", updatedTask);


            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Linkedin Outreach task successfully updated.", responseBody);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error: " + e.getMessage());
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating email outreach task: ", e);
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_STATUS, "Failed to update linkedin outreach task");
        }
    }


@PutMapping("/updateEmailOutreachTask")
public ResponseEntity<Map<String, Object>> updateOutreachTask(@RequestBody EmailOutreachTaskDTO emailOutreachTaskDTO) {
    try {

        // Validate required fields
        if (emailOutreachTaskDTO.getLifecycleId() == null && emailOutreachTaskDTO.getParentTaskId() == null) {
            logger.error(LIFECYCLE_PARENT_TASK_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, LIFECYCLE_PARENT_TASK_ID_REQUIRED);
        }

        // Validate task type
        if (!TaskType.EMAIL_OUTREACH_TASK.equalsIgnoreCase(emailOutreachTaskDTO.getType())) {
            String errorMsg = "Task type must be Email Outreach";
            logger.error(errorMsg);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, errorMsg);
        }

        // Process the update
        EmailOutreachTaskDTO updatedTask = taskService.updateEmailOutreachTask(emailOutreachTaskDTO);

        // Build response
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("updatedTask", updatedTask);


        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Email Outreach task successfully updated.", responseBody);
    } catch (IllegalArgumentException e) {
        logger.error("Validation error: " + e.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, e.getMessage());
    } catch (Exception e) {
        logger.error("Error updating email outreach task: ", e);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_STATUS, "Failed to update email outreach task");
    }
}

    @PutMapping("/updatePhoneOutreachTask")
    public ResponseEntity<Map<String, Object>> updatePhoneOutreachTask(@RequestBody PhoneOutreachTaskDTO phoneOutreachTaskDTO) {
        try {

            // Validate required fields
            if (phoneOutreachTaskDTO.getLifecycleId() == null && phoneOutreachTaskDTO.getParentTaskId() == null) {
                logger.error(LIFECYCLE_PARENT_TASK_ID_REQUIRED);
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, LIFECYCLE_PARENT_TASK_ID_REQUIRED);
            }

            // Validate task type
            if (!TaskType.PHONE_OUTREACH_TASK.equalsIgnoreCase(phoneOutreachTaskDTO.getType())) {
                String errorMsg = "Task type must be phone Outreach";
                logger.error(errorMsg);
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, errorMsg);
            }

            // Process the update
            PhoneOutreachTaskDTO updatedTask = taskService.updatePhoneOutreachTask(phoneOutreachTaskDTO);

            // Build response
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("updatedTask", updatedTask);


            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Phone Outreach task successfully updated.", responseBody);
        } catch (IllegalArgumentException e) {
            logger.error("Validation error: " + e.getMessage());
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating phone outreach task: ", e);
            return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_STATUS, "Failed to update phone outreach task");
        }
    }

    @PostMapping("/byTaskId")
    public ResponseEntity<Map<String,Object>> getTaskById(@RequestParam Long taskId)
    {
        if (taskId == null) {
            logger.error(TASK_ID_REQUIRED);
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, TASK_ID_REQUIRED);
        }
        Optional<Task>task = taskService.getTaskById(taskId);
        if(task.isPresent())
        {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("TaskDetail",taskService.transformToTaskDTO(task.get()));
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Get the Task Detail successfully .", responseBody);
        }else
        {
            logger.warn("Task Detail with Task Id: {} not found", taskId);
            return buildResponse(HttpStatus.NOT_FOUND, ERROR_STATUS, "Task Detail with the given Task Id not found.");
        }

    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<Map<String,Object>> deleteTask(@RequestParam Long taskId){

        try {
            boolean isDeleted = taskService.deleteTask(taskId);
            if (isDeleted) {
                return buildResponse(HttpStatus.OK, SUCCESS_STATUS, taskId + " Task successfully deleted."); // 200 OK with response body
            } else {
                logger.warn("Failed to delete task with ID: {}", taskId);
                return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ERROR_STATUS, "Failed to delete the Task.");
            }
        }catch(Exception e)
        {
            logger.warn("Deleting purpose given Task ID: {} not found", taskId);
            return buildResponse(HttpStatus.NOT_FOUND,ERROR_STATUS,"Task not found."); // 404 Not Found with response body
        }
    }

    @PostMapping("/updateTaskSequence")
    public ResponseEntity<Map<String,Object>> updateTaskSequence(@RequestParam String userName,@RequestBody List<TaskDTO> taskList)
    {
        if(userName == null)
        {
            logger.error("user name is required");
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, "user name is required");
        }
        List<UserTaskSequence> userTaskSequenceList = taskService.updateTaskSequence(userName,taskList);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("userTaskSequence", userTaskSequenceList);
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "User Task sequence updated successfully.", responseBody);
    }



    @PutMapping("/updateSendToAssigneeByTaskId")
    public ResponseEntity<Map<String,Object>> updateSendToAssigneeByTaskId(@RequestParam Long taskId, @RequestParam Boolean sendToAssignee){
        if(taskId == null){
            logger.error("Task Id is required.");
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, "Task Id is required.");
        }
        Map<String,Object> responseBody = new HashMap<>();
        responseBody.put("Message",taskService.updateSendToAssigneeByTaskId(taskId,sendToAssignee));
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "sendToAssignee of Task updated successfully.", responseBody);

    }

    @GetMapping("/getInWorkEmailOutreachTasks")
    public ResponseEntity<Map<String, Object>> getInWorkEmailOutreachTasks( @RequestParam String assignTo,
                                                                            @RequestParam boolean sendToAssignee,
                                                                            @RequestParam int year,
                                                                            @RequestParam int month,
                                                                            @RequestParam int day) {
        List<InWorkOutreachTaskTypeDTO> inWorkTasks = taskService.getInWorkEmailOutreachTasks(assignTo, sendToAssignee, year, month, day);

        Map<String, Object> response = new HashMap<>();
        response.put("inWorkEmailOutreachTask", inWorkTasks);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/getInWorkLinkedInOutreachTasks")
    public ResponseEntity<Map<String, Object>> getInWorkLinkedInOutreachTasks(@RequestParam String assignTo,
                                                                              @RequestParam boolean sendToAssignee,
                                                                              @RequestParam int year,
                                                                              @RequestParam int month,
                                                                              @RequestParam int day) {
        List<InWorkOutreachTaskTypeDTO> inWorkTasks = taskService.getInWorkLinkedInOutreachTasks(assignTo, sendToAssignee, year, month, day);

        Map<String, Object> response = new HashMap<>();
        response.put("inWorkLinkedInOutreachTask", inWorkTasks);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/getInWorkPhoneOutreachTasks")
    public ResponseEntity<Map<String, Object>> getInWorkPhoneOutreachTasks(@RequestParam String assignTo,
                                                                           @RequestParam boolean sendToAssignee,
                                                                           @RequestParam int year,
                                                                           @RequestParam int month,
                                                                           @RequestParam int day) {
        List<InWorkOutreachTaskTypeDTO> inWorkTasks = taskService.getInWorkPhoneOutreachTasks(assignTo, sendToAssignee, year, month, day);

        Map<String, Object> response = new HashMap<>();
        response.put("campaignList", inWorkTasks);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/getPhoneTaskDetailsByTaskId")
    public ResponseEntity<?> getPhoneTaskDetailsByTaskId(@RequestParam Long taskId) {
        try {
            PhoneOutreachTaskDTO phoneTaskDTO = taskService.getPhoneTaskDetailsByTaskId(taskId);
            if (phoneTaskDTO == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "Phone Outreach Task not found for ID: " + taskId));
            }
            return ResponseEntity.ok(phoneTaskDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error fetching Phone Outreach Task details", "error", e.getMessage()));
        }
    }

    @PutMapping("/updateEmailOutreachStatus")
    public ResponseEntity<String> updateOutreachStatus(@RequestParam Long taskId, @RequestParam String newStatus) {
        String result = taskService.updateEmailOutreachTaskStatus(taskId, newStatus);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/updateLinkedInOutreachStatus")
    public ResponseEntity<String> updateLinkedInOutreachStatus(@RequestParam Long taskId, @RequestParam String newStatus) {
        String result = taskService.updateLinkedInOutreachTaskStatus(taskId, newStatus);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/updatePhoneOutreachStatus")
    public ResponseEntity<String> updatePhoneOutreachStatus(@RequestParam Long taskId, @RequestParam String newStatus) {
        String result = taskService.updatePhoneOutreachTaskStatus(taskId, newStatus);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/promotionAutomationForPhoneOutreach")
    public ResponseEntity<?> promotionAutomationForPhoneOutreach(
            @RequestParam Long taskId,
            @RequestParam String promotionAutomationPhoneOutreachStatus) {

        try {
            String result = taskService.promotionAutomationForPhoneOutreach(taskId, promotionAutomationPhoneOutreachStatus);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error processing promotion automation", "error", e.getMessage()));
        }
    }


    @PostMapping("/sentEmailOutreachTasks")
    public ResponseEntity<Map<String, Object>> sentEmailOutreachTasks(
            @RequestBody SentEmailOutreachTasksDTO request
    ) {
        List<String> messages = new ArrayList<>();

        for (EmailOutreachTaskDTO dto : request.getTaskEmailOutreachList()) {
            Long taskId = dto.getTaskId();
            String result = taskService.sentEmailOutreachTask(taskId);
            messages.add("Task Id" + taskId + ": " + result);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("messages", messages);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/sentLinkedInOutreachTasks")
    public ResponseEntity<Map<String, Object>> sentLinkedInOutreachTasks(
            @RequestBody SentLinkedInOutreachTaskDTO request
    ) {
        List<String> messages = new ArrayList<>();

        for (LinkedInOutreachTaskDTO dto : request.getTaskLinkedInOutreachList()) {
            Long taskId = dto.getTaskId();
            String result = taskService.sentLinkedInOutreachTask(taskId);
            messages.add("Task Id" + taskId + ": " + result);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("messages", messages);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/notActiveOnLinkedIn")
    public ResponseEntity<Map<String, Object>> notActiveOnLinkedIn(
            @RequestBody SentLinkedInOutreachTaskDTO request
    ) {
        List<String> messages = new ArrayList<>();

        for (LinkedInOutreachTaskDTO dto : request.getTaskLinkedInOutreachList()) {
            Long taskId = dto.getTaskId();
            String result = taskService.notActiveOnLinkedIn(taskId);
            messages.add("Task Id" + taskId + ": " + result);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("messages", messages);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/acceptLinkedInOutreachTask")
    public ResponseEntity<Map<String, Object>> acceptLinkedInOutreachTask(@RequestParam Long taskId) {
        String result = taskService.sentLinkedInOutreachTask(taskId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", result);
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }


    @GetMapping("/getLinkedInOutreachWaitingAcceptanceTask")
    public ResponseEntity<?> getLinkedInOutreachWaitingAcceptanceTask(@RequestParam String userName) {
        List<LinkedInOutreachTaskDTO> tasks = taskService.getLinkedInOutreachWaitingAcceptanceTask(userName);
        return ResponseEntity.ok(tasks);
    }


    @PutMapping("/updateDueDateAndWeeklyTaskSequence")
    public ResponseEntity<Map<String,Object>> updateDueDateAndWeeklyTaskSequence(@RequestBody List<TaskWeeklyPlannerDTO> taskDTOList){
        try {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("Message", taskService.updateDueDateAndWeeklyTaskSequenceByTaskId(taskDTOList));
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "DueDate And WeeklyTaskSequence field updated Successfully.", responseBody);
        }catch (Exception e)
        {
            return buildResponse(HttpStatus.NOT_FOUND,ERROR_STATUS,e.getMessage());
        }
    }

    @PutMapping("/updateTaskStatus")
    public ResponseEntity<Map<String,Object>> updateTaskStatus(@RequestParam Long taskId,String targetStatus){
        if(taskId == null){
            logger.error("Task Id is required");
            return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, "Task Id is required");
        }
        Map<String,Object> responseBody = new HashMap<>();
        responseBody.put("Message",taskService.updateTaskStatus(taskId,targetStatus));
        return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "Status of Task updated successfully.", responseBody);

    }

    @GetMapping("/getAssignedToOthersTasks")
    public ResponseEntity<AssignedToOthersTasksDTO> getAssignedToOthersTasks(
            @RequestParam String userName) {
        AssignedToOthersTasksDTO response = taskService.getAssignedToOthersTasks(userName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getInWorkEmailOutreachTasksByCampaignIdAndLifecycleStatus")
    public ResponseEntity<?> getInWorkEmailOutreachTasksByCampaignIdAndLifecycleStatus(
            @RequestParam Long campaignId,
            @RequestParam String lifecycleStatus,
            @RequestParam String assignTo,
            @RequestParam boolean sendToAssignee,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam int day) {

        List<EmailOutreachTaskDTO> taskList = taskService
                .getInWorkEmailOutreachTasksByCampaignIdAndLifecycleStatus(campaignId, lifecycleStatus,assignTo,sendToAssignee,year,month,day);

        Map<String, Object> response = new HashMap<>();
        response.put("taskList", taskList);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/getInWorkLinkedInOutreachTasksByCampaignIdAndLifecycleStatus")
    public ResponseEntity<?> getInWorkLinkedInOutreachTasksByCampaignIdAndLifecycleStatus(
            @RequestParam Long campaignId,
            @RequestParam String lifecycleStatus,
            @RequestParam String assignTo,
            @RequestParam boolean sendToAssignee,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam int day) {

        List<LinkedInOutreachTaskDTO> taskList = taskService
                .getInWorkLinkedInOutreachTasksByCampaignIdAndLifecycleStatus(campaignId, lifecycleStatus, assignTo,sendToAssignee,year,month,day);

        Map<String, Object> response = new HashMap<>();
        response.put("taskList", taskList);

        return ResponseEntity.ok(response);
    }



    @GetMapping("/getDailyTaskCount")
    public ResponseEntity<Map<String, Long>> getDailyTaskCount(
            @RequestParam String assignTo,
            @RequestParam boolean sendToAssignee,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam int day) {

        Map<String, Long> counts = taskService.getDailyTaskCount(assignTo, sendToAssignee, year, month, day);
        return ResponseEntity.ok(counts);
    }


    @GetMapping("/getDailyTasks")
    public ResponseEntity<Map<String,Object>> getDailyTasks
            (@RequestParam String assignTo,
             @RequestParam boolean sendToAssignee,
             @RequestParam int year, @RequestParam int month, @RequestParam int day,@RequestParam boolean excludeOutreachTasks){
        try{
            if(assignTo ==null)
            {
                logger.error("AssignTo field is required");
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, "AssignTo field is required");
            }
            Map<String,Object> responseBody = new HashMap<>();
            responseBody.put("Task List",taskService.getDailyTasks(assignTo,sendToAssignee,year,month,day,excludeOutreachTasks));
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "The task list was received successfully.", responseBody);
        }catch(Exception e)
        {
            e.printStackTrace();
            logger.warn("API getDailyTasks  Error ="+e.getMessage());
            return buildResponse(HttpStatus.NOT_FOUND,ERROR_STATUS,e.getMessage()); // 404 Not Found with response body
        }
    }

    @GetMapping("/getWeeklyTasks")
    public ResponseEntity<Map<String,Object>> getWeeklyTasks
            (@RequestParam String assignTo,
             @RequestParam boolean sendToAssignee,
             @RequestParam int year, @RequestParam int month, @RequestParam int weekStartDay,
             @RequestParam int todayYear,@RequestParam int todayMonth,@RequestParam int todayDay){
        try{
            if(assignTo ==null)
            {
                logger.error("AssignTo field is required");
                return buildResponse(HttpStatus.BAD_REQUEST, ERROR_STATUS, "AssignTo field is required");
            }
            Map<String,Object> responseBody = new HashMap<>();
            responseBody.put("Task List",taskService.getWeeklyTasks(assignTo,sendToAssignee,year,month,weekStartDay,todayYear, todayMonth,todayDay));
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "The task list was received successfully.", responseBody);
        }catch(Exception e)
        {
            e.printStackTrace();
            logger.warn("API getWeeklyTasks  Error ="+e.getMessage());
            return buildResponse(HttpStatus.NOT_FOUND,ERROR_STATUS,e.getMessage()); // 404 Not Found with response body
        }
    }

    @PutMapping("/updateAssignToAndWeeklyTaskSequence")
    public ResponseEntity<Map<String,Object>> updateAssignToAndWeeklyTaskSequence(@RequestParam Long taskId,@RequestParam String assignTo){
        try {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("Message", taskService.updateAssignToAndWeeklyTaskSequenceByTaskId(taskId,assignTo,99));
            return buildResponse(HttpStatus.OK, SUCCESS_STATUS, "AssignTo field updated Successfully.", responseBody);
        }catch (Exception e)
        {
            return buildResponse(HttpStatus.NOT_FOUND,ERROR_STATUS,e.getMessage());
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
