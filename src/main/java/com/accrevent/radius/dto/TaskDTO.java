package com.accrevent.radius.dto;

import com.accrevent.radius.util.TaskType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TaskDTO {
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
    private Boolean isSystemComment = false;
    private String type = TaskType.DEFAULT;
    private List<TaskDTO> subTasks = new ArrayList<>();
    private String path;
}
