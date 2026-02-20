package com.accrevent.radius.model;

import com.accrevent.radius.Serializable.UserTaskCommentTimeTrack;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import lombok.Data;

import java.time.ZonedDateTime;

@Entity
@IdClass(UserTaskCommentTimeTrack.class)
@Data
public class TaskCommentTrack {
    @Id
    private String userId;
    @Id
    private Long taskId;
    private ZonedDateTime dateTime;
}
