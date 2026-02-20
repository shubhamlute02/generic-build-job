package com.accrevent.radius.Serializable;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

@Data
public class UserTaskCommentTimeTrack implements Serializable {
    private String userId;
    private Long taskId;

    public UserTaskCommentTimeTrack() {}
    public UserTaskCommentTimeTrack(String userId, Long taskId) {
        this.userId = userId;
        this.taskId = taskId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserTaskCommentTimeTrack that = (UserTaskCommentTimeTrack) o;
        return Objects.equals(userId, that.userId) && Objects.equals(taskId, that.taskId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, taskId);
    }
}
