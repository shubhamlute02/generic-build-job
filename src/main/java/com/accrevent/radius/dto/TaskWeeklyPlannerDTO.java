package com.accrevent.radius.dto;


import com.accrevent.radius.util.TaskType;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TaskWeeklyPlannerDTO {
    private Long taskId;
    private String taskName;
    private String description;
    private Long lifecycleId;
    private Long parentTaskId;
    private String assignTo;
    private boolean sendToAssignee;
    private String duration;
    private Integer durationValue;
    private Long dueDate;
    private String status;
    private Long createdOn;
    private int weeklyTaskSequence;
    private String path;
    private Long workspaceId;
    private String type;

    //Contact
    // New optional outreach fields
    private Long contactId;
    private String contactFirstName;
    private String contactLastName;
    private String contactEmailID;
    private String contactPhoneNo;
    private String contactCompany;
    private String contactCity;
    private String contactState;
    private String contactCountry;
    private String linkedInUrl;


    // Constructor for JPQL query (without type)
    public TaskWeeklyPlannerDTO(Long taskId, String taskName, String description,
                                Long lifecycleId, Long parentTaskId, String assignTo,
                                boolean sendToAssignee, String duration, Integer durationValue,
                                Long dueDate, String status, Long createdOn,
                                int weeklyTaskSequence, String path, Long workspaceId) {
        this(taskId, taskName, description, lifecycleId, parentTaskId, assignTo,
                sendToAssignee, duration, durationValue, dueDate, status, createdOn,
                weeklyTaskSequence, path, workspaceId,null);
    }

    public TaskWeeklyPlannerDTO(Long taskId, String taskName, String description, Long lifecycleId, Long parentTaskId, String assignTo, boolean sendToAssignee, String duration, Integer durationValue, Long dueDate, String status, Long createdOn, int weeklyTaskSequence, String path, Long workspaceId,String type) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.description = description;
        this.lifecycleId = lifecycleId;
        this.parentTaskId = parentTaskId;
        this.assignTo = assignTo;
        this.sendToAssignee = sendToAssignee;
        this.duration = duration;
        this.durationValue = durationValue;
        this.dueDate = dueDate;
        this.status = status;
        this.createdOn = createdOn;
        this.weeklyTaskSequence = weeklyTaskSequence;
        this.path = path;
        this.workspaceId = workspaceId;
        this.type = type;
    }
}
