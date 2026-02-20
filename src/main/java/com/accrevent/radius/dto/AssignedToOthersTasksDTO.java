package com.accrevent.radius.dto;

import lombok.Data;

import java.util.List;
@Data
public class AssignedToOthersTasksDTO {

    private List<UserTasks> assignedToOthersTasks;

    // Getters and setters
    public List<UserTasks> getAssignedToOthersTasks() {
        return assignedToOthersTasks;
    }

    public void setAssignedToOthersTasks(List<UserTasks> assignedToOthersTasks) {
        this.assignedToOthersTasks = assignedToOthersTasks;
    }

    public static class UserTasks {
        private String user;
        private List<TaskWeeklyPlannerDTO> tasks;

        // Getters and setters
        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public List<TaskWeeklyPlannerDTO> getTasks() {
            return tasks;
        }

        public void setTasks(List<TaskWeeklyPlannerDTO> tasks) {
            this.tasks = tasks;
        }
    }
}
